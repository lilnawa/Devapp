package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: Long): String = "editor/$projectId"
    }
    data object HtmlPreview : Screen("preview/{projectId}") {
        fun createRoute(projectId: Long): String = "preview/$projectId"
    }
    data object Terminal : Screen("terminal/{projectId}") {
        fun createRoute(projectId: Long): String = "terminal/$projectId"
    }
    data object AiAssistant : Screen("ai_assistant?projectId={projectId}") {
        fun createRoute(projectId: Long? = null): String =
            if (projectId != null) "ai_assistant?projectId=$projectId" else "ai_assistant"
    }
    data object GitHub : Screen("github")
    data object BuildPipeline : Screen("build/{projectId}") {
        fun createRoute(projectId: Long): String = "build/$projectId"
    }
    data object Settings : Screen("settings")
}
