package com.example.data.repository

import android.content.Context
import com.example.data.db.DevHubDatabase
import com.example.data.file.FileManager
import com.example.data.file.ProjectTemplates
import com.example.data.model.BuildLogEntity
import com.example.data.model.GitRepositoryEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.RecentFileEntity
import com.example.data.model.UserSettingsEntity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ProjectRepository(
    private val context: Context,
    val database: DevHubDatabase = DevHubDatabase.getDatabase(context),
    val fileManager: FileManager = FileManager(context)
) {
    private val projectDao = database.projectDao()
    private val recentFileDao = database.recentFileDao()
    private val userSettingsDao = database.userSettingsDao()
    private val buildLogDao = database.buildLogDao()
    private val gitRepoDao = database.gitRepoDao()
    private val aiChatDao = database.aiChatDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val recentProjects: Flow<List<ProjectEntity>> = projectDao.getRecentProjects()
    val favoriteProjects: Flow<List<ProjectEntity>> = projectDao.getFavoriteProjects()
    val recentFiles: Flow<List<RecentFileEntity>> = recentFileDao.getRecentFiles()
    val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getSettings()
    val buildLogs: Flow<List<BuildLogEntity>> = buildLogDao.getAllLogs()
    val gitRepositories: Flow<List<GitRepositoryEntity>> = gitRepoDao.getAllRepositories()
    val chatMessages = aiChatDao.getAllMessages()

    suspend fun initializeStarterProjectsIfEmpty() {
        val currentCount = projectDao.getProjectCount().firstOrNull() ?: 0
        if (currentCount == 0) {
            // Create Starter HTML Project
            createProject(
                name = "Modern Portfolio",
                templateType = ProjectTemplates.TEMPLATE_HTML,
                language = "HTML",
                description = "Interactive responsive web showcase with live JS telemetry"
            )

            // Create Starter Python Project
            createProject(
                name = "Python Analytics",
                templateType = ProjectTemplates.TEMPLATE_PYTHON,
                language = "Python",
                description = "Statistical calculation engine and algorithm utilities"
            )

            // Create Starter Android Kotlin Project
            createProject(
                name = "DevHub Compose App",
                templateType = ProjectTemplates.TEMPLATE_KOTLIN_ANDROID,
                language = "Kotlin",
                description = "Modern Jetpack Compose mobile application template"
            )

            // Create default UserSettings
            userSettingsDao.saveSettings(
                UserSettingsEntity(
                    id = 1,
                    themeMode = "dark",
                    fontSize = 14,
                    tabSize = 4,
                    autoSave = true,
                    wordWrap = false,
                    syntaxHighlight = true
                )
            )
        }
    }

    suspend fun createProject(
        name: String,
        templateType: String,
        language: String,
        description: String = ""
    ): ProjectEntity {
        val (dir, filesMap) = fileManager.createProject(name, templateType)
        val stats = fileManager.calculateDirectoryStats(dir)
        var totalLines = 0
        filesMap.values.forEach { content ->
            totalLines += content.lines().size
        }

        val project = ProjectEntity(
            name = dir.name,
            language = language,
            localPath = dir.absolutePath,
            description = description.ifBlank { "Mobile $language project" },
            fileCount = stats.first.coerceAtLeast(filesMap.size),
            totalLines = totalLines
        )
        val insertedId = projectDao.insertProject(project)
        return project.copy(id = insertedId)
    }

    suspend fun getProjectById(id: Long): ProjectEntity? {
        return projectDao.getProjectByIdDirect(id)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        val dir = File(project.localPath)
        fileManager.deleteFileOrDirectory(dir)
        recentFileDao.deleteForProject(project.id)
        projectDao.deleteProject(project)
    }

    suspend fun duplicateProject(project: ProjectEntity, newName: String): ProjectEntity? {
        val srcDir = File(project.localPath)
        val newDir = fileManager.duplicateProject(srcDir, newName) ?: return null
        val stats = fileManager.calculateDirectoryStats(newDir)
        val duplicate = ProjectEntity(
            name = newDir.name,
            language = project.language,
            localPath = newDir.absolutePath,
            description = "Copy of ${project.name}",
            fileCount = stats.first,
            totalLines = project.totalLines
        )
        val newId = projectDao.insertProject(duplicate)
        return duplicate.copy(id = newId)
    }

    suspend fun renameProject(project: ProjectEntity, newName: String): ProjectEntity? {
        val srcDir = File(project.localPath)
        val renamedDir = fileManager.renameFile(srcDir, newName) ?: return null
        val updated = project.copy(
            name = renamedDir.name,
            localPath = renamedDir.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
        return updated
    }

    suspend fun toggleFavorite(project: ProjectEntity) {
        projectDao.updateProject(project.copy(isFavorite = !project.isFavorite))
    }

    suspend fun touchProject(projectId: Long) {
        val project = projectDao.getProjectByIdDirect(projectId) ?: return
        val dir = File(project.localPath)
        val stats = fileManager.calculateDirectoryStats(dir)
        projectDao.updateProject(
            project.copy(
                updatedAt = System.currentTimeMillis(),
                fileCount = stats.first
            )
        )
    }

    suspend fun recordRecentFile(projectId: Long, file: File, language: String) {
        recentFileDao.insertRecentFile(
            RecentFileEntity(
                projectId = projectId,
                filePath = file.absolutePath,
                fileName = file.name,
                language = language,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
        touchProject(projectId)
    }

    suspend fun importZipProject(inputStream: InputStream, projectName: String, language: String): ProjectEntity? {
        val importedDir = fileManager.importProjectFromZip(inputStream, projectName) ?: return null
        val stats = fileManager.calculateDirectoryStats(importedDir)
        val project = ProjectEntity(
            name = importedDir.name,
            language = language,
            localPath = importedDir.absolutePath,
            description = "Imported ZIP archive",
            fileCount = stats.first
        )
        val id = projectDao.insertProject(project)
        return project.copy(id = id)
    }

    suspend fun exportZipProject(project: ProjectEntity, outputStream: OutputStream): Boolean {
        val dir = File(project.localPath)
        return fileManager.exportProjectToZip(dir, outputStream)
    }

    suspend fun updateSettings(settings: UserSettingsEntity) {
        userSettingsDao.saveSettings(settings)
    }

    suspend fun logBuild(
        projectId: Long,
        projectName: String,
        buildType: String,
        status: String,
        logs: String,
        durationMs: Long,
        artifactPath: String? = null
    ) {
        buildLogDao.insertLog(
            BuildLogEntity(
                projectId = projectId,
                projectName = projectName,
                buildType = buildType,
                status = status,
                logs = logs,
                durationMs = durationMs,
                artifactPath = artifactPath
            )
        )
    }
}
