package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.BuildLogEntity
import com.example.data.model.GitRepositoryEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.RecentFileEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>
}

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC LIMIT 10")
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE projectId = :projectId ORDER BY lastOpenedAt DESC")
    fun getRecentFilesForProject(projectId: Long): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFileEntity): Long

    @Query("DELETE FROM recent_files WHERE filePath = :filePath")
    suspend fun deleteByFilePath(filePath: String)

    @Query("DELETE FROM recent_files WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)
}

@Dao
interface AIChatDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AIChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: Long): Flow<List<AIChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AIChatMessageEntity): Long

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface GitRepoDao {
    @Query("SELECT * FROM git_repositories ORDER BY lastSyncedAt DESC")
    fun getAllRepositories(): Flow<List<GitRepositoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: GitRepositoryEntity): Long

    @Update
    suspend fun updateRepository(repo: GitRepositoryEntity)

    @Delete
    suspend fun deleteRepository(repo: GitRepositoryEntity)
}

@Dao
interface BuildLogDao {
    @Query("SELECT * FROM build_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BuildLogEntity>>

    @Query("SELECT * FROM build_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProject(projectId: Long): Flow<List<BuildLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BuildLogEntity): Long

    @Query("DELETE FROM build_logs")
    suspend fun clearLogs()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
