package com.example.ui.github

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.github.GitHubRepo
import com.example.ui.MainViewModel
import com.example.ui.components.SimpleInputDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProject: (Long) -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val githubUser by viewModel.githubUser.collectAsState()
    val githubRepos by viewModel.githubRepos.collectAsState()
    val isLoading by viewModel.isGitHubLoading.collectAsState()
    val message by viewModel.gitHubMessage.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showTokenDialog by remember { mutableStateOf(false) }
    var repoForReadme by remember { mutableStateOf<GitHubRepo?>(null) }
    var readmeContent by remember { mutableStateOf<String?>(null) }
    var isReadmeLoading by remember { mutableStateOf(false) }

    // Auto connect if token is already in settings
    LaunchedEffect(userSettings?.githubToken) {
        val token = userSettings?.githubToken
        if (!token.isNullOrBlank() && githubUser == null) {
            viewModel.connectGitHub(token)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("github_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GitHub Integration", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTokenDialog = true },
                        modifier = Modifier.testTag("github_token_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Set Token")
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
            // User Header or Connect banner
            if (githubUser != null) {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                githubUser?.login?.take(2)?.uppercase() ?: "GH",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                githubUser?.name ?: githubUser?.login ?: "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "@${githubUser?.login} • ${githubUser?.publicRepos} public repos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }

                        Button(
                            onClick = { showTokenDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243248)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("github_switch_account")
                        ) {
                            Text("Switch", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Connect GitHub", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Enter your Personal Access Token to browse, clone, and push to your repositories.", fontSize = 12.sp, color = Color.LightGray)
                        }
                        Button(
                            onClick = { showTokenDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.testTag("github_connect_button")
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }

            // Status message banner
            message?.let { msg ->
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        msg,
                        fontSize = 12.sp,
                        color = CyberCyan,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search repositories on GitHub...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("github_search_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.searchGitHub(searchQuery) },
                    modifier = Modifier.testTag("github_search_btn")
                ) {
                    Text("Search")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricBlue)
                }
            }

            // Repositories List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("github_repo_list"),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(githubRepos, key = { it.id }) { repo ->
                    GitHubRepoCard(
                        repo = repo,
                        onClone = {
                            viewModel.cloneGitHubRepo(repo) { newProjId ->
                                onNavigateToProject(newProjId)
                            }
                        },
                        onViewReadme = {
                            repoForReadme = repo
                            isReadmeLoading = true
                            readmeContent = null
                            scope.launch {
                                val result = viewModel.gitHubService.getReadme(repo.fullName, userSettings?.githubToken)
                                readmeContent = result.getOrNull() ?: "# README\n\nNo README file available."
                                isReadmeLoading = false
                            }
                        }
                    )
                }
            }
        }
    }

    // GitHub PAT Input Dialog
    if (showTokenDialog) {
        SimpleInputDialog(
            title = "GitHub Personal Access Token",
            label = "Token (ghp_...)",
            initialValue = userSettings?.githubToken ?: "",
            confirmText = "Save & Connect",
            onDismiss = { showTokenDialog = false },
            onConfirm = { token ->
                showTokenDialog = false
                viewModel.connectGitHub(token)
            }
        )
    }

    // README Preview Dialog
    repoForReadme?.let { repo ->
        AlertDialog(
            onDismissRequest = { repoForReadme = null },
            title = { Text(repo.name, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (isReadmeLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ElectricBlue)
                        }
                    } else {
                        LazyColumn {
                            item {
                                Text(
                                    readmeContent ?: "Loading README...",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toClone = repo
                        repoForReadme = null
                        viewModel.cloneGitHubRepo(toClone) { id ->
                            onNavigateToProject(id)
                        }
                    },
                    modifier = Modifier.testTag("readme_clone_button")
                ) {
                    Text("Clone Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { repoForReadme = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GitHubRepoCard(
    repo: GitHubRepo,
    onClone: () -> Unit,
    onViewReadme: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("github_repo_card_${repo.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    repo.fullName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )

                if (repo.isPrivate) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF334155)
                    ) {
                        Text(
                            "Private",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        repo.language ?: "Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "★ ${repo.stars}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                    Text(
                        "⑂ ${repo.forks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onViewReadme,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("view_readme_${repo.name}")
                    ) {
                        Text("README", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onClone,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("clone_btn_${repo.name}")
                    ) {
                        Text("Clone", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
