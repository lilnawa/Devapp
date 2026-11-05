package com.example.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.ProjectTemplates
import com.example.data.model.ProjectEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.SimpleInputDialog
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToTerminal: (Long) -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToGitHub: () -> Unit,
    onNavigateToBuild: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val allProjects by viewModel.allProjects.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDuplicate by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    // ZIP Import Launcher
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip") ?: "ImportedProject"
                viewModel.importZipProject(inputStream, fileName, "Code") { newId ->
                    onNavigateToEditor(newId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DevHub",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            " IDE",
                            fontWeight = FontWeight.Medium,
                            color = CyberCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAi,
                        modifier = Modifier.testTag("dashboard_ai_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = DeepViolet)
                    }
                    IconButton(
                        onClick = onNavigateToGitHub,
                        modifier = Modifier.testTag("dashboard_github_button")
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "GitHub", tint = ElectricBlue)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Project") },
                text = { Text("New Project") },
                containerColor = CyberCyan,
                contentColor = Color(0xFF00363D),
                modifier = Modifier.testTag("dashboard_fab_new_project")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Welcome Banner
            item {
                HeroBanner(
                    projectCount = allProjects.size,
                    onOpenTerminal = {
                        val firstProject = allProjects.firstOrNull()
                        if (firstProject != null) onNavigateToTerminal(firstProject.id)
                        else onNavigateToTerminal(0L)
                    }
                )
            }

            // Quick Actions Scroll Row
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionChip(
                        title = "HTML Site",
                        icon = Icons.Default.Language,
                        color = CyberCyan,
                        onClick = {
                            viewModel.createNewProject(
                                name = "WebSite_${System.currentTimeMillis() % 1000}",
                                template = ProjectTemplates.TEMPLATE_HTML,
                                language = "HTML",
                                desc = "Responsive website with live DOM and JS telemetry"
                            ) { onNavigateToEditor(it) }
                        }
                    )
                    QuickActionChip(
                        title = "Python App",
                        icon = Icons.Default.Code,
                        color = ElectricBlue,
                        onClick = {
                            viewModel.createNewProject(
                                name = "Python_${System.currentTimeMillis() % 1000}",
                                template = ProjectTemplates.TEMPLATE_PYTHON,
                                language = "Python",
                                desc = "Embedded Python script calculation engine"
                            ) { onNavigateToEditor(it) }
                        }
                    )
                    QuickActionChip(
                        title = "Android App",
                        icon = Icons.Default.Android,
                        color = NeonGreen,
                        onClick = {
                            viewModel.createNewProject(
                                name = "Android_${System.currentTimeMillis() % 1000}",
                                template = ProjectTemplates.TEMPLATE_KOTLIN_ANDROID,
                                language = "Kotlin",
                                desc = "Jetpack Compose Android app project"
                            ) { onNavigateToEditor(it) }
                        }
                    )
                    QuickActionChip(
                        title = "Import ZIP",
                        icon = Icons.Default.FolderZip,
                        color = WarningAmber,
                        onClick = {
                            zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                        }
                    )
                    QuickActionChip(
                        title = "GitHub",
                        icon = Icons.Default.CloudSync,
                        color = DeepViolet,
                        onClick = onNavigateToGitHub
                    )
                }
            }

            // Recently Opened Files
            if (recentFiles.isNotEmpty()) {
                item {
                    Text(
                        "Recently Edited Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentFiles.take(6).forEach { rf ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.selectProject(rf.projectId)
                                        viewModel.openFile(File(rf.filePath))
                                        onNavigateToEditor(rf.projectId)
                                    }
                                    .testTag("recent_file_${rf.fileName}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        rf.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Projects Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Projects (${allProjects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Projects List
            if (allProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No projects found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showNewProjectDialog = true },
                                modifier = Modifier.testTag("empty_create_project_button")
                            ) {
                                Text("Create First Project")
                            }
                        }
                    }
                }
            } else {
                items(allProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = {
                            viewModel.selectProject(project.id)
                            onNavigateToEditor(project.id)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(project) },
                        onRename = { projectToRename = project },
                        onDuplicate = { projectToDuplicate = project },
                        onDelete = { projectToDelete = project },
                        onOpenTerminal = {
                            viewModel.selectProject(project.id)
                            onNavigateToTerminal(project.id)
                        },
                        onOpenBuild = {
                            viewModel.selectProject(project.id)
                            onNavigateToBuild(project.id)
                        }
                    )
                }
            }

            // Bottom Spacer for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Dialogs
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, template, lang, desc ->
                showNewProjectDialog = false
                viewModel.createNewProject(name, template, lang, desc) { id ->
                    onNavigateToEditor(id)
                }
            }
        )
    }

    projectToRename?.let { proj ->
        SimpleInputDialog(
            title = "Rename Project",
            label = "Project Name",
            initialValue = proj.name,
            confirmText = "Rename",
            onDismiss = { projectToRename = null },
            onConfirm = { newName ->
                viewModel.renameProject(proj, newName)
                projectToRename = null
            }
        )
    }

    projectToDuplicate?.let { proj ->
        SimpleInputDialog(
            title = "Duplicate Project",
            label = "New Project Name",
            initialValue = "${proj.name}_copy",
            confirmText = "Duplicate",
            onDismiss = { projectToDuplicate = null },
            onConfirm = { newName ->
                viewModel.duplicateProject(proj, newName)
                projectToDuplicate = null
            }
        )
    }

    projectToDelete?.let { proj ->
        ConfirmDeleteDialog(
            itemName = proj.name,
            onDismiss = { projectToDelete = null },
            onConfirm = {
                viewModel.deleteProject(proj)
                projectToDelete = null
            }
        )
    }
}

@Composable
fun HeroBanner(
    projectCount: Int,
    onOpenTerminal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Developer Workspace",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "DevHub IDE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0A0E14),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            "READY",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricItem(label = "Active Projects", value = "$projectCount")
                    MetricItem(label = "Environment", value = "Android 10–16")
                    MetricItem(label = "Architecture", value = "ARM64 Linux")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenTerminal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243248)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("hero_open_terminal_button")
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Open Mobile Terminal",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
    }
}

@Composable
fun QuickActionChip(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .width(115.dp)
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBuild: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val langColor = when (project.language.uppercase()) {
        "HTML" -> CyberCyan
        "PYTHON" -> ElectricBlue
        "KOTLIN" -> DeepViolet
        "JAVA" -> WarningAmber
        else -> NeonGreen
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(langColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = langColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            project.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = langColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("fav_btn_${project.name}")
                    ) {
                        Icon(
                            if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (project.isFavorite) WarningAmber else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.testTag("menu_btn_${project.name}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Terminal") },
                                onClick = {
                                    expandedMenu = false
                                    onOpenTerminal()
                                },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                            )
                            if (project.language.equals("Kotlin", ignoreCase = true) || project.language.equals("Java", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text("Build Pipeline") },
                                    onClick = {
                                        expandedMenu = false
                                        onOpenBuild()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    expandedMenu = false
                                    onRename()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    expandedMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${project.fileCount} files • Updated ${dateFormat.format(Date(project.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Text(
                    "Open Code →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
        }
    }
}
