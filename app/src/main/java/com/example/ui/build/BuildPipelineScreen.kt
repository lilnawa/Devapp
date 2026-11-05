package com.example.ui.build

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuildLogEntity
import com.example.engine.build.SdkComponentStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildPipelineScreen(
    projectId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val activeProject by viewModel.activeProject.collectAsState()
    val sdkStatusList by viewModel.sdkStatusList.collectAsState()
    val currentBuildResult by viewModel.currentBuildResult.collectAsState()
    val isBuilding by viewModel.isBuildingApk.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Build Runner, 1: SDK Health, 2: History

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("build_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("APK Build Pipeline", fontWeight = FontWeight.Bold)
                        Text(
                            activeProject?.name ?: "Android Project",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshSdkStatus() },
                        modifier = Modifier.testTag("build_refresh_sdk")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check SDK")
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
                .background(DarkBackground)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceVariant,
                contentColor = NeonGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Assemble APK") },
                    modifier = Modifier.testTag("tab_assemble")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("SDK Toolchain") },
                    modifier = Modifier.testTag("tab_sdk_status")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Build History") },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            when (selectedTab) {
                0 -> AssembleApkTab(
                    viewModel = viewModel,
                    isBuilding = isBuilding,
                    currentBuildResult = currentBuildResult,
                    onCopyLogs = { logs ->
                        clipboardManager.setText(AnnotatedString(logs))
                    }
                )
                1 -> SdkToolchainTab(sdkStatusList = sdkStatusList)
                2 -> BuildHistoryTab(buildLogs = buildLogs)
            }
        }
    }
}

@Composable
fun AssembleApkTab(
    viewModel: MainViewModel,
    isBuilding: Boolean,
    currentBuildResult: com.example.engine.build.BuildResult?,
    onCopyLogs: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Build Action Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Compile & Package Android Binary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Runs AAPT2 resource linking, Kotlin compilation, D8 dexing, and debug keystore packaging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.runAndroidBuild("Debug APK") },
                        enabled = !isBuilding,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_debug_build_button")
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Build Debug APK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runAndroidBuild("Release APK") },
                        enabled = !isBuilding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_release_build_button")
                    ) {
                        Text("Release APK")
                    }
                }
            }
        }

        // Live Build Logs Terminal
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkEditorBg,
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .testTag("build_terminal_surface")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentBuildResult?.status) {
                                        "SUCCESS" -> NeonGreen
                                        "MISSING_SDK", "FAILED" -> ErrorRed
                                        else -> if (isBuilding) WarningAmber else CyberCyan
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isBuilding) "Compiling APK..." else "Build Console Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    if (currentBuildResult != null) {
                        IconButton(
                            onClick = { onCopyLogs(currentBuildResult.logs) },
                            modifier = Modifier.size(28.dp).testTag("copy_build_logs_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF1E293B))

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (currentBuildResult == null && !isBuilding) {
                        Text(
                            "Press 'Build Debug APK' above to execute pipeline.\nTargeting AAPT2, D8 DEX compiler, and target API 34.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else if (isBuilding) {
                        Text(
                            "> Executing Gradle Task :app:compileDebug...\n> Verifying local toolchains...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                    } else {
                        Text(
                            text = currentBuildResult?.logs ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = if (currentBuildResult?.success == true) Color(0xFFE2E8F0) else Color(0xFFFFB4AB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SdkToolchainTab(sdkStatusList: List<SdkComponentStatus>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sdk_toolchain_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Android SDK & Toolchain Verification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "DevHub verifies native binaries before attempting packaging to prevent false build states.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(sdkStatusList) { status ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (status.isAvailable) NeonGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (status.isAvailable) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (status.isAvailable) NeonGreen else ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            status.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            status.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.isAvailable) NeonGreen else Color.Gray
                        )
                        Text(
                            status.path,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildHistoryTab(buildLogs: List<BuildLogEntity>) {
    if (buildLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No builds executed yet.", color = Color.Gray)
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("build_history_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(buildLogs, key = { it.id }) { log ->
                val isSuccess = log.status == "SUCCESS"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${log.projectName} (${log.buildType})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSuccess) NeonGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    log.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuccess) NeonGreen else ErrorRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${dateFormat.format(Date(log.timestamp))} • Duration: ${log.durationMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkEditorBg)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = log.logs.take(300) + if (log.logs.length > 300) "..." else "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
