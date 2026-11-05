package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.BuildLogEntity
import com.example.data.model.GitRepositoryEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.RecentFileEntity
import com.example.data.model.UserSettingsEntity

@Database(
    entities = [
        ProjectEntity::class,
        RecentFileEntity::class,
        AIChatMessageEntity::class,
        GitRepositoryEntity::class,
        BuildLogEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DevHubDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun aiChatDao(): AIChatDao
    abstract fun gitRepoDao(): GitRepoDao
    abstract fun buildLogDao(): BuildLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DevHubDatabase? = null

        fun getDatabase(context: Context): DevHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevHubDatabase::class.java,
                    "devhub_ide_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
