package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.SimpleInputDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }
    var showAiKeyDialog by remember { mutableStateOf(false) }

    val currentSettings = userSettings ?: UserSettingsEntity()

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode
            item {
                SettingsSectionHeader("Appearance & Theme")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Color Scheme", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("dark" to "Cyber Dark", "light" to "Light", "system" to "System").forEach { (mode, label) ->
                                val isSelected = currentSettings.themeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSettings(currentSettings.copy(themeMode = mode)) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_$mode")
                                )
                            }
                        }
                    }
                }
            }

            // Code Editor Preferences
            item {
                SettingsSectionHeader("Code Editor Preferences")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Font Size Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Editor Font Size", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("${currentSettings.fontSize} sp", fontWeight = FontWeight.Bold, color = CyberCyan)
                            }
                            Slider(
                                value = currentSettings.fontSize.toFloat(),
                                onValueChange = {
                                    viewModel.updateSettings(currentSettings.copy(fontSize = it.toInt()))
                                },
                                valueRange = 10f..24f,
                                steps = 14,
                                modifier = Modifier.testTag("font_size_slider")
                            )
                        }

                        // Tab Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Indentation Tab Size", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Spaces per Tab key press", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2, 4, 8).forEach { size ->
                                    FilterChip(
                                        selected = currentSettings.tabSize == size,
                                        onClick = { viewModel.updateSettings(currentSettings.copy(tabSize = size)) },
                                        label = { Text("$size") }
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFF1E293B))

                        // Auto-Save Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto Save", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Save files automatically upon switching tabs", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = currentSettings.autoSave,
                                onCheckedChange = { viewModel.updateSettings(currentSettings.copy(autoSave = it)) },
                                modifier = Modifier.testTag("autosave_switch")
                            )
                        }

                        // Syntax Highlight Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Syntax Highlighting", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Real-time token coloring for 13+ languages", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = currentSettings.syntaxHighlight,
                                onCheckedChange = { viewModel.updateSettings(currentSettings.copy(syntaxHighlight = it)) },
                                modifier = Modifier.testTag("syntax_highlight_switch")
                            )
                        }
                    }
                }
            }

            // Cloud & External APIs
            item {
                SettingsSectionHeader("Developer Services & APIs")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // GitHub Integration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GitHub Account", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(
                                    if (currentSettings.githubUsername.isNotBlank()) "Connected as @${currentSettings.githubUsername}" else "Not connected",
                                    fontSize = 12.sp,
                                    color = if (currentSettings.githubUsername.isNotBlank()) NeonGreen else Color.Gray
                                )
                            }
                            Button(
                                onClick = { showTokenDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.testTag("settings_set_github_token")
                            ) {
                                Text(if (currentSettings.githubToken.isNotBlank()) "Update" else "Connect")
                            }
                        }

                        Divider(color = Color(0xFF1E293B))

                        // Gemini AI Key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gemini AI API Key", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(
                                    if (currentSettings.geminiApiKey.isNotBlank()) "Key configured (Active)" else "AI Studio BuildConfig key or local fallback",
                                    fontSize = 12.sp,
                                    color = if (currentSettings.geminiApiKey.isNotBlank()) DeepViolet else Color.Gray
                                )
                            }
                            Button(
                                onClick = { showAiKeyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
                                modifier = Modifier.testTag("settings_set_ai_key")
                            ) {
                                Text("Configure")
                            }
                        }
                    }
                }
            }

            // Storage & Environment
            item {
                SettingsSectionHeader("Storage & Runtime Environment")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Local Projects Sandbox:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        Text(
                            context.filesDir.resolve("devhub_projects").absolutePath,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Architecture:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        Text(
                            "Linux Android ARM64 • Target SDK 36 (Android 10–16)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // About DevHub
            item {
                SettingsSectionHeader("About DevHub")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DevHub – Mobile Developer IDE", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Version 1.0.0 (Release Build)", fontSize = 12.sp, color = CyberCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Professional mobile development environment built with Kotlin, Jetpack Compose, Material 3, Room, AAPT2 build pipeline, embedded Python 3.12 engine, and GitHub OAuth integration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        SimpleInputDialog(
            title = "GitHub Personal Access Token",
            label = "GitHub PAT (ghp_...)",
            initialValue = currentSettings.githubToken,
            confirmText = "Save",
            onDismiss = { showTokenDialog = false },
            onConfirm = { token ->
                showTokenDialog = false
                viewModel.connectGitHub(token)
            }
        )
    }

    if (showAiKeyDialog) {
        SimpleInputDialog(
            title = "Gemini AI API Key",
            label = "API Key",
            initialValue = currentSettings.geminiApiKey,
            confirmText = "Save",
            onDismiss = { showAiKeyDialog = false },
            onConfirm = { key ->
                showAiKeyDialog = false
                viewModel.updateSettings(currentSettings.copy(geminiApiKey = key.trim()))
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = CyberCyan
    )
}
