package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAssistantService
import com.example.data.file.FileNode
import com.example.data.file.ProjectTemplates
import com.example.data.github.GitHubRepo
import com.example.data.github.GitHubService
import com.example.data.github.GitHubUser
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.BuildLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.ProjectRepository
import com.example.engine.build.AndroidBuildPipeline
import com.example.engine.build.BuildResult
import com.example.engine.build.SdkComponentStatus
import com.example.engine.python.ExecutionResult
import com.example.engine.python.PythonInterpreter
import com.example.engine.terminal.TerminalEngine
import com.example.engine.terminal.TerminalLine
import com.example.engine.terminal.TerminalLineType
import com.example.ui.editor.SyntaxHighlighter
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditorTab(
    val file: File,
    val language: String,
    var content: String,
    var isDirty: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProjectRepository(application)
    val gitHubService = GitHubService()
    val aiAssistantService = AiAssistantService()
    val buildPipeline = AndroidBuildPipeline(application)
    val pythonInterpreter = PythonInterpreter()
    val terminalEngine = TerminalEngine(application)

    // Flow states
    val allProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentProjects = repository.recentProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentFiles = repository.recentFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val userSettings = repository.userSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSettingsEntity(id = 1, themeMode = "dark", fontSize = 14, tabSize = 4, autoSave = true)
    )
    val buildLogs = repository.buildLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project and editor state
    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    val currentTab: StateFlow<EditorTab?> = combine(_openTabs, _activeTabIndex) { tabs, index ->
        if (tabs.isNotEmpty() && index in tabs.indices) tabs[index] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Terminal state
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(
        listOf(
            TerminalLine("DevHub Mobile IDE Terminal v1.0.0 [Android Shell]", TerminalLineType.SYSTEM),
            TerminalLine("Type 'help' for available commands, 'ls' to list project files.", TerminalLineType.OUTPUT)
        )
    )
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    // Python Execution state
    private val _pythonResult = MutableStateFlow<ExecutionResult?>(null)
    val pythonResult: StateFlow<ExecutionResult?> = _pythonResult.asStateFlow()
    val isRunningPython = MutableStateFlow(false)

    // AI Assistant state
    val isAiGenerating = MutableStateFlow(false)

    // GitHub state
    private val _githubUser = MutableStateFlow<GitHubUser?>(null)
    val githubUser: StateFlow<GitHubUser?> = _githubUser.asStateFlow()

    private val _githubRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val githubRepos: StateFlow<List<GitHubRepo>> = _githubRepos.asStateFlow()

    val isGitHubLoading = MutableStateFlow(false)
    val gitHubMessage = MutableStateFlow<String?>(null)

    // Build pipeline state
    private val _sdkStatusList = MutableStateFlow<List<SdkComponentStatus>>(emptyList())
    val sdkStatusList: StateFlow<List<SdkComponentStatus>> = _sdkStatusList.asStateFlow()

    private val _currentBuildResult = MutableStateFlow<BuildResult?>(null)
    val currentBuildResult: StateFlow<BuildResult?> = _currentBuildResult.asStateFlow()

    val isBuildingApk = MutableStateFlow(false)

    // HTML Preview Console logs
    private val _webConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val webConsoleLogs: StateFlow<List<String>> = _webConsoleLogs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeStarterProjectsIfEmpty()
            refreshSdkStatus()
        }
    }

    fun selectProject(projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            _activeProject.value = project
            if (project != null) {
                val projectDir = File(project.localPath)
                terminalEngine.setDirectory(projectDir)
                refreshFileTree()
                openDefaultFileForProject(projectDir)
            }
        }
    }

    fun refreshFileTree() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val tree = repository.fileManager.listFileTree(File(proj.localPath))
            _fileTree.value = tree
        }
    }

    private suspend fun openDefaultFileForProject(projectDir: File) {
        val preferred = listOf("index.html", "main.py", "MainActivity.kt", "Main.java", "README.md")
        var fileToOpen: File? = null
        for (name in preferred) {
            val candidate = File(projectDir, name)
            if (candidate.exists() && candidate.isFile) {
                fileToOpen = candidate
                break
            }
        }
        if (fileToOpen == null) {
            fileToOpen = projectDir.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
        }
        if (fileToOpen != null) {
            openFile(fileToOpen)
        }
    }

    fun openFile(file: File) {
        viewModelScope.launch {
            val currentTabs = _openTabs.value.toMutableList()
            val existingIndex = currentTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
            if (existingIndex >= 0) {
                _activeTabIndex.value = existingIndex
            } else {
                val content = repository.fileManager.readFile(file.absolutePath)
                val lang = SyntaxHighlighter.detectLanguage(file.name)
                val newTab = EditorTab(file = file, language = lang, content = content)
                currentTabs.add(newTab)
                _openTabs.value = currentTabs
                _activeTabIndex.value = currentTabs.size - 1

                _activeProject.value?.let { proj ->
                    repository.recordRecentFile(proj.id, file, lang)
                }
            }
        }
    }

    fun closeTab(index: Int) {
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            tabs.removeAt(index)
            _openTabs.value = tabs
            if (_activeTabIndex.value >= tabs.size) {
                _activeTabIndex.value = (tabs.size - 1).coerceAtLeast(0)
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _openTabs.value.indices) {
            _activeTabIndex.value = index
        }
    }

    fun updateCurrentTabContent(newContent: String) {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val current = tabs[index]
            if (current.content != newContent) {
                tabs[index] = current.copy(content = newContent, isDirty = true)
                _openTabs.value = tabs
            }
        }
    }

    fun saveCurrentFile() {
        val index = _activeTabIndex.value
        val tabs = _openTabs.value.toMutableList()
        if (index in tabs.indices) {
            val tab = tabs[index]
            viewModelScope.launch {
                repository.fileManager.writeFile(tab.file.absolutePath, tab.content)
                tabs[index] = tab.copy(isDirty = false)
                _openTabs.value = tabs
                _activeProject.value?.let { repository.touchProject(it.id) }
            }
        }
    }

    fun createNewProject(name: String, template: String, language: String, desc: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val project = repository.createProject(name, template, language, desc)
            selectProject(project.id)
            onCreated(project.id)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _openTabs.value = emptyList()
            }
            repository.deleteProject(project)
        }
    }

    fun duplicateProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch {
            val duplicate = repository.duplicateProject(project, newName)
            if (duplicate != null) {
                selectProject(duplicate.id)
            }
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch {
            val renamed = repository.renameProject(project, newName)
            if (_activeProject.value?.id == project.id && renamed != null) {
                _activeProject.value = renamed
            }
        }
    }

    fun toggleFavorite(project: ProjectEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(project)
        }
    }

    fun createFile(parentDir: File, fileName: String) {
        viewModelScope.launch {
            val created = repository.fileManager.createFile(parentDir, fileName)
            refreshFileTree()
            if (created != null) {
                openFile(created)
            }
        }
    }

    fun createDirectory(parentDir: File, dirName: String) {
        viewModelScope.launch {
            repository.fileManager.createDirectory(parentDir, dirName)
            refreshFileTree()
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            // Close tab if open
            val tabs = _openTabs.value.toMutableList()
            val index = tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
            if (index >= 0) {
                tabs.removeAt(index)
                _openTabs.value = tabs
                _activeTabIndex.value = (_activeTabIndex.value - 1).coerceAtLeast(0)
            }
            repository.fileManager.deleteFileOrDirectory(file)
            refreshFileTree()
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            val renamed = repository.fileManager.renameFile(file, newName)
            refreshFileTree()
            if (renamed != null) {
                val tabs = _openTabs.value.toMutableList()
                val index = tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
                if (index >= 0) {
                    val lang = SyntaxHighlighter.detectLanguage(renamed.name)
                    tabs[index] = tabs[index].copy(file = renamed, language = lang)
                    _openTabs.value = tabs
                }
            }
        }
    }

    fun importZipProject(inputStream: InputStream, name: String, language: String, onImported: (Long) -> Unit) {
        viewModelScope.launch {
            val project = repository.importZipProject(inputStream, name, language)
            if (project != null) {
                selectProject(project.id)
                onImported(project.id)
            }
        }
    }

    fun exportProjectZip(project: ProjectEntity, outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportZipProject(project, outputStream)
        }
    }

    // Terminal Commands
    fun executeTerminalCommand(cmd: String) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            val lines = _terminalLines.value.toMutableList()
            lines.add(TerminalLine("devhub:~$ ${cmd.trim()}", TerminalLineType.INPUT))
            if (cmd.trim().equals("clear", ignoreCase = true)) {
                _terminalLines.value = emptyList()
                return@launch
            }
            val outputs = terminalEngine.executeCommand(cmd)
            lines.addAll(outputs)
            _terminalLines.value = lines
        }
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    // Python Execution
    fun runPythonScript(scriptContent: String? = null) {
        val code = scriptContent ?: currentTab.value?.content ?: return
        viewModelScope.launch {
            isRunningPython.value = true
            val result = pythonInterpreter.execute(code)
            _pythonResult.value = result
            isRunningPython.value = false

            // Also pipe into terminal for integrated feel
            val lines = _terminalLines.value.toMutableList()
            lines.add(TerminalLine("[Python Run: ${currentTab.value?.file?.name ?: "script.py"}]", TerminalLineType.SYSTEM))
            result.output.lines().filter { it.isNotBlank() }.forEach {
                lines.add(TerminalLine(it, TerminalLineType.OUTPUT))
            }
            if (result.errors.isNotBlank()) {
                result.errors.lines().filter { it.isNotBlank() }.forEach {
                    lines.add(TerminalLine(it, TerminalLineType.ERROR))
                }
            }
            _terminalLines.value = lines
        }
    }

    fun clearPythonResult() {
        _pythonResult.value = null
    }

    // Web Console
    fun addWebConsoleLog(msg: String) {
        val current = _webConsoleLogs.value.toMutableList()
        current.add(msg)
        _webConsoleLogs.value = current
    }

    fun clearWebConsole() {
        _webConsoleLogs.value = emptyList()
    }

    // AI Assistant
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val currentCode = currentTab.value?.content
        val currentLang = currentTab.value?.language
        val settings = userSettings.value

        viewModelScope.launch {
            val userMsg = AIChatMessageEntity(
                projectId = _activeProject.value?.id,
                role = "user",
                content = prompt.trim()
            )
            repository.database.aiChatDao().insertMessage(userMsg)

            isAiGenerating.value = true
            val response = aiAssistantService.sendMessage(
                prompt = prompt,
                userApiKey = settings?.geminiApiKey,
                codeContext = currentCode,
                language = currentLang
            )
            isAiGenerating.value = false

            val assistantMsg = AIChatMessageEntity(
                projectId = _activeProject.value?.id,
                role = "assistant",
                content = response.text,
                codeSnippet = response.codeBlocks.firstOrNull()
            )
            repository.database.aiChatDao().insertMessage(assistantMsg)
        }
    }

    fun clearAiChat() {
        viewModelScope.launch {
            repository.database.aiChatDao().clearAllMessages()
        }
    }

    // GitHub Integration
    fun connectGitHub(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            isGitHubLoading.value = true
            gitHubMessage.value = null
            val userResult = gitHubService.getAuthenticatedUser(token)
            if (userResult.isSuccess) {
                _githubUser.value = userResult.getOrNull()
                val reposResult = gitHubService.getUserRepositories(token)
                _githubRepos.value = reposResult.getOrDefault(emptyList())
                gitHubMessage.value = "Connected as ${userResult.getOrNull()?.login}"

                // Save to settings
                val current = userSettings.value ?: UserSettingsEntity()
                repository.updateSettings(
                    current.copy(
                        githubToken = token,
                        githubUsername = userResult.getOrNull()?.login ?: ""
                    )
                )
            } else {
                gitHubMessage.value = "Failed: ${userResult.exceptionOrNull()?.message}"
            }
            isGitHubLoading.value = false
        }
    }

    fun searchGitHub(query: String) {
        if (query.isBlank()) return
        val token = userSettings.value?.githubToken
        viewModelScope.launch {
            isGitHubLoading.value = true
            val result = gitHubService.searchRepositories(query, token)
            _githubRepos.value = result.getOrDefault(emptyList())
            isGitHubLoading.value = false
        }
    }

    fun cloneGitHubRepo(repo: GitHubRepo, onCloned: (Long) -> Unit) {
        val token = userSettings.value?.githubToken
        viewModelScope.launch {
            isGitHubLoading.value = true
            gitHubMessage.value = "Cloning ${repo.name}..."
            val (dir, _) = repository.fileManager.createProject(repo.name, ProjectTemplates.TEMPLATE_EMPTY)
            val cloneResult = gitHubService.cloneRepoFiles(repo.fullName, dir, token)

            if (cloneResult.isSuccess) {
                val stats = repository.fileManager.calculateDirectoryStats(dir)
                val project = ProjectEntity(
                    name = dir.name,
                    language = repo.language ?: "Code",
                    localPath = dir.absolutePath,
                    description = repo.description ?: "Cloned from GitHub: ${repo.fullName}",
                    fileCount = stats.first
                )
                val id = repository.database.projectDao().insertProject(project)
                selectProject(id)
                gitHubMessage.value = "Successfully cloned ${repo.name} (${cloneResult.getOrNull()} files)"
                onCloned(id)
            } else {
                gitHubMessage.value = "Clone failed: ${cloneResult.exceptionOrNull()?.message}"
            }
            isGitHubLoading.value = false
        }
    }

    // Android Build Pipeline
    fun refreshSdkStatus() {
        _sdkStatusList.value = buildPipeline.checkSdkEnvironment()
    }

    fun runAndroidBuild(buildType: String = "Debug APK") {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            isBuildingApk.value = true
            val projectDir = File(proj.localPath)
            val result = buildPipeline.runBuild(projectDir, buildType)
            _currentBuildResult.value = result

            repository.logBuild(
                projectId = proj.id,
                projectName = proj.name,
                buildType = buildType,
                status = result.status,
                logs = result.logs,
                durationMs = result.durationMs,
                artifactPath = result.generatedApkPath
            )
            isBuildingApk.value = false
        }
    }

    // Settings update
    fun updateSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }
}
