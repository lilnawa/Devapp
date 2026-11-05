package com.example.ui.terminal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.terminal.TerminalLine
import com.example.engine.terminal.TerminalLineType
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    projectId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val terminalLines by viewModel.terminalLines.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var inputCommand by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    val quickCommands = listOf("help", "ls -la", "pwd", "python main.py", "git status", "df -h", "uname -a", "clear")

    // Auto scroll to bottom when lines arrive
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("terminal_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("DevHub Terminal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "devhub@android:${viewModel.terminalEngine.currentDirectory.name}$",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val allText = terminalLines.joinToString("\n") { it.text }
                            clipboardManager.setText(AnnotatedString(allText))
                            copiedNotice = true
                        },
                        modifier = Modifier.testTag("terminal_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
                    }
                    IconButton(
                        onClick = { viewModel.clearTerminal() },
                        modifier = Modifier.testTag("terminal_clear_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
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
            // Quick Commands bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickCommands.forEach { cmd ->
                    Surface(
                        onClick = {
                            inputCommand = cmd
                            viewModel.executeTerminalCommand(cmd)
                            inputCommand = ""
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.testTag("cmd_chip_$cmd")
                    ) {
                        Text(
                            text = cmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Terminal log list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("terminal_log_list"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(terminalLines) { line ->
                    TerminalLineRow(line)
                }
            }

            // Command Prompt & Input bar
            Surface(
                color = DarkSurface,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$ ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )

                    OutlinedTextField(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        placeholder = { Text("Enter command (e.g. ls, python main.py)...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputCommand.isNotBlank()) {
                                    viewModel.executeTerminalCommand(inputCommand)
                                    inputCommand = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_command_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputCommand.isNotBlank()) {
                                viewModel.executeTerminalCommand(inputCommand)
                                inputCommand = ""
                            }
                        },
                        modifier = Modifier.testTag("terminal_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Run Command", tint = CyberCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLineRow(line: TerminalLine) {
    val color = when (line.type) {
        TerminalLineType.INPUT -> NeonGreen
        TerminalLineType.OUTPUT -> Color(0xFFE2E8F0)
        TerminalLineType.SUCCESS -> NeonGreen
        TerminalLineType.ERROR -> ErrorRed
        TerminalLineType.WARNING -> WarningAmber
        TerminalLineType.SYSTEM -> CyberCyan
    }

    Text(
        text = line.text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = color,
        lineHeight = 18.sp
    )
}
