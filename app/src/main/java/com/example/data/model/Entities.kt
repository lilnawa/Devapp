package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val language: String, // "HTML", "Python", "Kotlin", "Java", "Markdown", "JavaScript"
    val localPath: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileCount: Int = 1,
    val totalLines: Int = 0
)

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val fileName: String,
    val language: String,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val cursorPosition: Int = 0
)

@Entity(tableName = "ai_chat_messages")
data class AIChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val role: String, // "user", "assistant"
    val content: String,
    val codeSnippet: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "git_repositories")
data class GitRepositoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val owner: String,
    val cloneUrl: String,
    val defaultBranch: String = "main",
    val localPath: String,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastCommitMessage: String? = null
)

@Entity(tableName = "build_logs")
data class BuildLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val projectName: String,
    val buildType: String, // "Debug APK", "Release APK"
    val status: String, // "SUCCESS", "FAILED", "WARNING", "CONFIG_REQUIRED"
    val logs: String,
    val durationMs: Long,
    val artifactPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: String = "dark", // "dark", "light", "system"
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val autoSave: Boolean = true,
    val wordWrap: Boolean = false,
    val syntaxHighlight: Boolean = true,
    val githubToken: String = "",
    val githubUsername: String = "",
    val geminiApiKey: String = "",
    val aiModel: String = "gemini-1.5-flash"
)
