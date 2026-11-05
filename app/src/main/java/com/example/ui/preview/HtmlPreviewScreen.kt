package com.example.ui.preview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkEditorBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlPreviewScreen(
    projectId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeProject by viewModel.activeProject.collectAsState()
    val consoleLogs by viewModel.webConsoleLogs.collectAsState()

    var isMobileViewport by remember { mutableStateOf(false) }
    var showConsoleSheet by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val projectDir = remember(activeProject) {
        activeProject?.localPath?.let { File(it) }
    }

    val indexHtmlFile = remember(projectDir) {
        projectDir?.let { dir ->
            val index = File(dir, "index.html")
            if (index.exists()) index else dir.walkTopDown().firstOrNull { it.extension.lowercase() in listOf("html", "htm") }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.loadUrl("about:blank")
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("preview_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Live Web Preview", fontWeight = FontWeight.Bold)
                        Text(
                            "${activeProject?.name ?: "Website"} • ${if (isMobileViewport) "Mobile (375px)" else "Responsive Viewport"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }
                },
                actions = {
                    // Mobile vs Desktop toggle
                    IconButton(
                        onClick = { isMobileViewport = !isMobileViewport },
                        modifier = Modifier.testTag("preview_viewport_toggle")
                    ) {
                        Icon(
                            if (isMobileViewport) Icons.Default.Smartphone else Icons.Default.DesktopWindows,
                            contentDescription = "Toggle viewport",
                            tint = if (isMobileViewport) CyberCyan else Color.LightGray
                        )
                    }

                    // Reload safely using loadDataWithBaseURL
                    IconButton(
                        onClick = {
                            indexHtmlFile?.let { file ->
                                val parentFolder = file.parentFile
                                val htmlText = try { file.readText() } catch (e: Exception) { "" }
                                webViewRef?.loadDataWithBaseURL("file://${parentFolder.absolutePath}/", htmlText, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.testTag("preview_reload_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }

                    // Open external browser
                    IconButton(
                        onClick = {
                            indexHtmlFile?.let { file ->
                                try {
                                    val uri = Uri.fromFile(file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "text/html")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open in Browser"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.testTag("preview_external_browser_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }

                    // Console Drawer Toggle
                    BadgedBox(
                        badge = {
                            if (consoleLogs.isNotEmpty()) {
                                Badge(containerColor = CyberCyan) {
                                    Text("${consoleLogs.size}")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { showConsoleSheet = !showConsoleSheet },
                            modifier = Modifier.testTag("preview_console_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "Console Logs")
                        }
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val containerModifier = if (isMobileViewport) {
                    Modifier
                        .width(375.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF30363D), RoundedCornerShape(16.dp))
                } else {
                    Modifier.fillMaxSize()
                }

                Box(modifier = containerModifier) {
                    if (indexHtmlFile != null && indexHtmlFile.exists()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    // Use software layer to prevent virtualized GPU renderer crash (code -1) in emulator
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        allowFileAccessFromFileURLs = true
                                        allowUniversalAccessFromFileURLs = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        cacheMode = WebSettings.LOAD_NO_CACHE
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                            if (consoleMessage != null) {
                                                val prefix = when (consoleMessage.messageLevel()) {
                                                    ConsoleMessage.MessageLevel.ERROR -> "[ERROR]"
                                                    ConsoleMessage.MessageLevel.WARNING -> "[WARN]"
                                                    else -> "[LOG]"
                                                }
                                                viewModel.addWebConsoleLog("$prefix ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                                            }
                                            return true
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                            // Returning true prevents aw_browser_terminator from killing the host app
                                            viewModel.addWebConsoleLog("[RECOVERED] Renderer process was recovered safely.")
                                            try {
                                                view?.stopLoading()
                                                view?.destroy()
                                            } catch (_: Exception) {}
                                            return true
                                        }
                                    }

                                    val parentFolder = indexHtmlFile.parentFile
                                    val htmlText = try { indexHtmlFile.readText() } catch (e: Exception) { "" }
                                    loadDataWithBaseURL("file://${parentFolder.absolutePath}/", htmlText, "text/html", "UTF-8", null)
                                    webViewRef = this
                                }
                            },
                            update = { webView ->
                                webViewRef = webView
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("preview_webview")
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No HTML file found in project",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Interactive Live JavaScript Console Drawer
            if (showConsoleSheet) {
                Surface(
                    color = Color(0xFF0F172A),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("preview_console_sheet")
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
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "JavaScript Console (${consoleLogs.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }

                            Row {
                                TextButton(
                                    onClick = { viewModel.clearWebConsole() },
                                    modifier = Modifier.height(28.dp).testTag("clear_console_btn")
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = { showConsoleSheet = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF1E293B))

                        if (consoleLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No console logs yet. Interact with the page to trigger events.", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(consoleLogs) { log ->
                                    val color = when {
                                        log.startsWith("[ERROR]") -> ErrorRed
                                        log.startsWith("[WARN]") -> WarningAmber
                                        else -> CyberCyan
                                    }
                                    Text(
                                        text = log,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
