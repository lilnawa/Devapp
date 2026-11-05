package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.ai.AiAssistantScreen
import com.example.ui.build.BuildPipelineScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.editor.CodeEditorScreen
import com.example.ui.github.GitHubScreen
import com.example.ui.navigation.Screen
import com.example.ui.preview.HtmlPreviewScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.terminal.TerminalScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val userSettings by mainViewModel.userSettings.collectAsState()

            val isDark = when (userSettings?.themeMode) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true // default to sleek cyber dark theme
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        // 1. Splash Screen
                        composable(Screen.Splash.route) {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Main Dashboard
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = mainViewModel,
                                onNavigateToEditor = { projectId ->
                                    navController.navigate(Screen.Editor.createRoute(projectId))
                                },
                                onNavigateToTerminal = { projectId ->
                                    navController.navigate(Screen.Terminal.createRoute(projectId))
                                },
                                onNavigateToAi = {
                                    navController.navigate(Screen.AiAssistant.createRoute())
                                },
                                onNavigateToGitHub = {
                                    navController.navigate(Screen.GitHub.route)
                                },
                                onNavigateToBuild = { projectId ->
                                    navController.navigate(Screen.BuildPipeline.createRoute(projectId))
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        // 3. Code Editor Screen
                        composable(
                            route = Screen.Editor.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            CodeEditorScreen(
                                projectId = projectId,
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPreview = { pId ->
                                    navController.navigate(Screen.HtmlPreview.createRoute(pId))
                                },
                                onNavigateToTerminal = { pId ->
                                    navController.navigate(Screen.Terminal.createRoute(pId))
                                },
                                onNavigateToAi = {
                                    navController.navigate(Screen.AiAssistant.createRoute(projectId))
                                },
                                onNavigateToBuild = { pId ->
                                    navController.navigate(Screen.BuildPipeline.createRoute(pId))
                                }
                            )
                        }

                        // 4. HTML Live Web Preview
                        composable(
                            route = Screen.HtmlPreview.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            HtmlPreviewScreen(
                                projectId = projectId,
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 5. Terminal Screen
                        composable(
                            route = Screen.Terminal.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            TerminalScreen(
                                projectId = projectId,
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. AI Assistant Copilot
                        composable(
                            route = Screen.AiAssistant.route,
                            arguments = listOf(navArgument("projectId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) {
                            AiAssistantScreen(
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onInsertIntoEditor = { snippet: String ->
                                    val currentTab = mainViewModel.currentTab.value
                                    if (currentTab != null) {
                                        mainViewModel.updateCurrentTabContent(currentTab.content + "\n" + snippet)
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 7. GitHub Integration
                        composable(Screen.GitHub.route) {
                            GitHubScreen(
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToProject = { newId ->
                                    navController.navigate(Screen.Editor.createRoute(newId))
                                }
                            )
                        }

                        // 8. APK Build Pipeline
                        composable(
                            route = Screen.BuildPipeline.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            BuildPipelineScreen(
                                projectId = projectId,
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 9. Settings Screen
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
