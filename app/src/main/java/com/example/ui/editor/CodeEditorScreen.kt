package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.FileNode
import com.example.engine.python.ExecutionResult
import com.example.ui.EditorTab
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.SimpleInputDialog
import com.example.ui.theme.*
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    projectId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToTerminal: (Long) -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToBuild: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val activeProject by viewModel.activeProject.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val pythonResult by viewModel.pythonResult.collectAsState()
    val isRunningPython by viewModel.isRunningPython.collectAsState()

    var showSearchReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var fontSize by remember { mutableIntStateOf(userSettings?.fontSize ?: 14) }
    var wordWrap by remember { mutableStateOf(userSettings?.wordWrap ?: false) }

    // Undo / Redo history stack for current editor
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // Sync editor text state when active tab changes
    LaunchedEffect(currentTab?.file?.absolutePath) {
        val tabContent = currentTab?.content ?: ""
        textFieldValue = TextFieldValue(tabContent, TextRange(tabContent.length))
        undoStack = emptyList()
        redoStack = emptyList()
    }

    // Dialog state for File Manager drawer
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var targetFolderForNewFile by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.saveCurrentFile()
            onNavigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = DarkSurface
            ) {
                FileExplorerDrawerContent(
                    projectName = activeProject?.name ?: "Project",
                    fileTree = fileTree,
                    activeFilePath = currentTab?.file?.absolutePath,
                    onFileClick = { file ->
                        viewModel.openFile(file)
                        scope.launch { drawerState.close() }
                    },
                    onNewFile = { parent ->
                        targetFolderForNewFile = parent ?: File(activeProject?.localPath ?: "")
                        showNewFileDialog = true
                    },
                    onNewFolder = { parent ->
                        targetFolderForNewFile = parent ?: File(activeProject?.localPath ?: "")
                        showNewFolderDialog = true
                    },
                    onRename = { file -> fileToRename = file },
                    onDelete = { file -> fileToDelete = file },
                    onRefresh = { viewModel.refreshFileTree() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentFile()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    currentTab?.file?.name ?: "Editor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (currentTab?.isDirty == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(WarningAmber)
                                    )
                                }
                            }
                            Text(
                                "${activeProject?.name ?: ""} • ${currentTab?.language ?: "Plain Text"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    },
                    actions = {
                        // Action: Run/Preview depending on file type
                        val isHtml = currentTab?.file?.name?.endsWith(".html") == true ||
                                currentTab?.file?.name?.endsWith(".htm") == true
                        val isPython = currentTab?.file?.name?.endsWith(".py") == true
                        val isAndroid = activeProject?.language?.equals("Kotlin", ignoreCase = true) == true ||
                                activeProject?.language?.equals("Java", ignoreCase = true) == true

                        if (isHtml) {
                            IconButton(
                                onClick = {
                                    viewModel.saveCurrentFile()
                                    onNavigateToPreview(projectId)
                                },
                                modifier = Modifier.testTag("editor_preview_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Preview HTML", tint = NeonGreen)
                            }
                        } else if (isPython) {
                            IconButton(
                                onClick = {
                                    viewModel.saveCurrentFile()
                                    viewModel.runPythonScript(textFieldValue.text)
                                },
                                enabled = !isRunningPython,
                                modifier = Modifier.testTag("editor_run_python_button")
                            ) {
                                if (isRunningPython) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonGreen)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Python", tint = NeonGreen)
                                }
                            }
                        } else if (isAndroid) {
                            IconButton(
                                onClick = {
                                    viewModel.saveCurrentFile()
                                    onNavigateToBuild(projectId)
                                },
                                modifier = Modifier.testTag("editor_build_button")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = "Build APK", tint = NeonGreen)
                            }
                        }

                        // Save button
                        IconButton(
                            onClick = { viewModel.saveCurrentFile() },
                            modifier = Modifier.testTag("editor_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save file", tint = CyberCyan)
                        }

                        // Search/Replace toggle
                        IconButton(
                            onClick = { showSearchReplace = !showSearchReplace },
                            modifier = Modifier.testTag("editor_search_toggle")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search & Replace")
                        }

                        // Terminal Shortcut
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentFile()
                                onNavigateToTerminal(projectId)
                            },
                            modifier = Modifier.testTag("editor_terminal_button")
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = "Terminal")
                        }

                        // AI Assistant Shortcut
                        IconButton(
                            onClick = onNavigateToAi,
                            modifier = Modifier.testTag("editor_ai_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = DeepViolet)
                        }

                        // File Explorer Drawer toggle
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("editor_drawer_toggle")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Files")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(DarkEditorBg)
            ) {
                // Multi-tabs row
                if (openTabs.isNotEmpty()) {
                    EditorTabsRow(
                        tabs = openTabs,
                        activeIndex = activeTabIndex,
                        onSelectTab = { viewModel.selectTab(it) },
                        onCloseTab = { viewModel.closeTab(it) }
                    )
                }

                // Optional Search & Replace Bar
                if (showSearchReplace) {
                    SearchReplaceBar(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        replaceQuery = replaceQuery,
                        onReplaceChange = { replaceQuery = it },
                        onReplaceNext = {
                            if (searchQuery.isNotEmpty() && textFieldValue.text.contains(searchQuery)) {
                                val current = textFieldValue.text
                                val updated = current.replaceFirst(searchQuery, replaceQuery)
                                undoStack = undoStack + textFieldValue.text
                                textFieldValue = textFieldValue.copy(text = updated)
                                viewModel.updateCurrentTabContent(updated)
                            }
                        },
                        onReplaceAll = {
                            if (searchQuery.isNotEmpty() && textFieldValue.text.contains(searchQuery)) {
                                val current = textFieldValue.text
                                val updated = current.replace(searchQuery, replaceQuery)
                                undoStack = undoStack + textFieldValue.text
                                textFieldValue = textFieldValue.copy(text = updated)
                                viewModel.updateCurrentTabContent(updated)
                            }
                        },
                        onClose = { showSearchReplace = false }
                    )
                }

                // Editor Canvas with line numbers
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    EditorBodyWithLineNumbers(
                        textFieldValue = textFieldValue,
                        language = currentTab?.language ?: "Code",
                        fontSize = fontSize,
                        wordWrap = wordWrap,
                        onValueChange = { newVal ->
                            if (newVal.text != textFieldValue.text) {
                                undoStack = undoStack + textFieldValue.text
                                redoStack = emptyList()
                                viewModel.updateCurrentTabContent(newVal.text)
                            }
                            textFieldValue = newVal
                        }
                    )
                }

                // Python Output Peek Sheet if script was executed
                pythonResult?.let { result ->
                    PythonOutputSheet(
                        result = result,
                        onDismiss = { viewModel.clearPythonResult() }
                    )
                }

                // Bottom Developer Shortcut Bar: Brackets, Indentation, Undo/Redo, Font Size
                DeveloperQuickBar(
                    onInsertText = { snippet ->
                        val currentText = textFieldValue.text
                        val selection = textFieldValue.selection
                        val newText = StringBuilder(currentText)
                            .insert(selection.min, snippet)
                            .toString()
                        val newCursor = selection.min + snippet.length
                        undoStack = undoStack + currentText
                        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                        viewModel.updateCurrentTabContent(newText)
                    },
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onUndo = {
                        if (undoStack.isNotEmpty()) {
                            val prev = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                            redoStack = redoStack + textFieldValue.text
                            textFieldValue = TextFieldValue(prev, TextRange(prev.length))
                            viewModel.updateCurrentTabContent(prev)
                        }
                    },
                    onRedo = {
                        if (redoStack.isNotEmpty()) {
                            val next = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            undoStack = undoStack + textFieldValue.text
                            textFieldValue = TextFieldValue(next, TextRange(next.length))
                            viewModel.updateCurrentTabContent(next)
                        }
                    },
                    fontSize = fontSize,
                    onIncreaseFont = { if (fontSize < 24) fontSize += 1 },
                    onDecreaseFont = { if (fontSize > 10) fontSize -= 1 },
                    wordWrap = wordWrap,
                    onToggleWordWrap = { wordWrap = !wordWrap }
                )
            }
        }
    }

    // Dialogs for file management
    if (showNewFileDialog) {
        SimpleInputDialog(
            title = "New File",
            label = "File Name (e.g. script.js, utils.py)",
            confirmText = "Create",
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name ->
                showNewFileDialog = false
                targetFolderForNewFile?.let { folder ->
                    viewModel.createFile(folder, name)
                }
            }
        )
    }

    if (showNewFolderDialog) {
        SimpleInputDialog(
            title = "New Directory",
            label = "Folder Name",
            confirmText = "Create",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                showNewFolderDialog = false
                targetFolderForNewFile?.let { folder ->
                    viewModel.createDirectory(folder, name)
                }
            }
        )
    }

    fileToRename?.let { f ->
        SimpleInputDialog(
            title = "Rename ${if (f.isDirectory) "Directory" else "File"}",
            label = "New Name",
            initialValue = f.name,
            confirmText = "Rename",
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                viewModel.renameFile(f, newName)
                fileToRename = null
            }
        )
    }

    fileToDelete?.let { f ->
        ConfirmDeleteDialog(
            itemName = f.name,
            onDismiss = { fileToDelete = null },
            onConfirm = {
                viewModel.deleteFile(f)
                fileToDelete = null
            }
        )
    }
}

@Composable
fun EditorTabsRow(
    tabs: List<EditorTab>,
    activeIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("editor_tabs_row"),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tabs) { idx, tab ->
            val isSelected = idx == activeIndex
            Surface(
                onClick = { onSelectTab(idx) },
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                color = if (isSelected) DarkEditorBg else Color.Transparent,
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                modifier = Modifier.testTag("editor_tab_${tab.file.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tab.file.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyberCyan else Color.LightGray
                    )
                    if (tab.isDirty) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("•", color = WarningAmber, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onCloseTab(idx) },
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("close_tab_${tab.file.name}")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchReplaceBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceChange: (String) -> Unit,
    onReplaceNext: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = DarkSurface,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .testTag("search_replace_bar")
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Find in file...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("search_input")
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(18.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Replace with...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("replace_input")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onReplaceNext,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp).testTag("replace_button")
                ) {
                    Text("Replace", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onReplaceAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp).testTag("replace_all_button")
                ) {
                    Text("All", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun EditorBodyWithLineNumbers(
    textFieldValue: TextFieldValue,
    language: String,
    fontSize: Int,
    wordWrap: Boolean,
    onValueChange: (TextFieldValue) -> Unit
) {
    val lines = remember(textFieldValue.text) { textFieldValue.text.lines() }
    val lineCount = lines.size.coerceAtLeast(1)
    val highlightedVisual = remember(textFieldValue.text, language) {
        SyntaxHighlighter.highlight(textFieldValue.text, language)
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
    ) {
        // Line numbers column
        Column(
            modifier = Modifier
                .width(44.dp)
                .background(Color(0xFF0A0F17))
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (lineNum in 1..lineCount) {
                Text(
                    text = "$lineNum",
                    color = Color(0xFF475569),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.45).sp
                )
            }
        }

        Divider(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(),
            color = Color(0xFF1E293B)
        )

        // Code input area
        val contentModifier = if (wordWrap) {
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 48.dp)
        } else {
            Modifier
                .horizontalScroll(horizontalScroll)
                .fillMaxWidth()
                .padding(start = 8.dp, top = 12.dp, end = 32.dp, bottom = 48.dp)
        }

        Box(modifier = contentModifier) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color(0xFFF1F5F9),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.45).sp
                ),
                cursorBrush = SolidColor(CyberCyan),
                visualTransformation = {
                    androidx.compose.ui.text.input.TransformedText(
                        highlightedVisual,
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_text_field")
            )
        }
    }
}

@Composable
fun DeveloperQuickBar(
    onInsertText: (String) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    fontSize: Int,
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
    wordWrap: Boolean,
    onToggleWordWrap: () -> Unit
) {
    val quickTokens = listOf(
        "    ", "{", "}", "(", ")", "[", "]", "<", ">", "=", "\"", "'", ":", ";", ",", ".", "//", "function", "def", "class"
    )

    Surface(
        color = DarkSurface,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("developer_quick_bar")
    ) {
        Column {
            // Secondary row: Quick controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(32.dp).testTag("quick_undo")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.DarkGray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(32.dp).testTag("quick_redo")
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.DarkGray, modifier = Modifier.size(16.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleWordWrap, modifier = Modifier.height(28.dp)) {
                        Text(if (wordWrap) "Wrap: ON" else "Wrap: OFF", fontSize = 11.sp, color = if (wordWrap) CyberCyan else Color.Gray)
                    }
                    IconButton(onClick = onDecreaseFont, modifier = Modifier.size(28.dp)) {
                        Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("${fontSize}sp", fontSize = 11.sp, color = Color.LightGray)
                    IconButton(onClick = onIncreaseFont, modifier = Modifier.size(28.dp)) {
                        Text("A+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Primary horizontal scroll token keys
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(quickTokens) { token ->
                    Surface(
                        onClick = { onInsertText(token) },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.testTag("token_${token.trim().ifEmpty { "tab" }}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (token == "    ") "TAB" else token,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PythonOutputSheet(
    result: ExecutionResult,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .testTag("python_output_sheet")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (result.exitCode == 0) NeonGreen else ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Python Console (${result.executionTimeMs}ms, exit ${result.exitCode})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (result.output.isNotBlank()) {
                    Text(
                        result.output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
                if (result.errors.isNotBlank()) {
                    Text(
                        result.errors,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
fun FileExplorerDrawerContent(
    projectName: String,
    fileTree: List<FileNode>,
    activeFilePath: String?,
    onFileClick: (File) -> Unit,
    onNewFile: (File?) -> Unit,
    onNewFolder: (File?) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("file_explorer_drawer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                projectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CyberCyan
            )

            Row {
                IconButton(onClick = { onNewFile(null) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onNewFolder(null) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF1E293B))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(fileTree) { node ->
                FileTreeNodeItem(
                    node = node,
                    depth = 0,
                    activeFilePath = activeFilePath,
                    onFileClick = onFileClick,
                    onNewFile = onNewFile,
                    onNewFolder = onNewFolder,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun FileTreeNodeItem(
    node: FileNode,
    depth: Int,
    activeFilePath: String?,
    onFileClick: (File) -> Unit,
    onNewFile: (File?) -> Unit,
    onNewFolder: (File?) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    val isSelected = activeFilePath == node.path

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable {
                    if (node.isDirectory) {
                        expanded = !expanded
                    } else {
                        onFileClick(File(node.path))
                    }
                }
                .padding(start = (depth * 16 + 6).dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (node.isDirectory) {
                    Icon(
                        if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    val iconColor = when (node.extension.lowercase()) {
                        "html" -> CyberCyan
                        "css" -> ElectricBlue
                        "js" -> WarningAmber
                        "py" -> NeonGreen
                        "kt", "java" -> DeepViolet
                        else -> Color.LightGray
                    }
                    Icon(
                        Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFF1F5F9)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            onClick = {
                                showMenu = false
                                onNewFile(File(node.path))
                            },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            onClick = {
                                showMenu = false
                                onNewFolder(File(node.path))
                            },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename(File(node.path))
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete(File(node.path))
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }

        // Render sub-tree if directory is expanded
        if (node.isDirectory && expanded) {
            for (child in node.children) {
                FileTreeNodeItem(
                    node = child,
                    depth = depth + 1,
                    activeFilePath = activeFilePath,
                    onFileClick = onFileClick,
                    onNewFile = onNewFile,
                    onNewFolder = onNewFolder,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}
