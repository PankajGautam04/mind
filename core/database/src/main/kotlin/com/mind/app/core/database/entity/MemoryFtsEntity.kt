package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index synchronized automatically with [MemoryEntity].
 */
@Fts4(contentEntity = MemoryEntity::class)
@Entity(tableName = "memories_fts")
data class MemoryFtsEntity(
    @ColumnInfo(name = "ai_title")
    val aiTitle: String?,
    @ColumnInfo(name = "user_title")
    val userTitle: String?,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "extracted_text")
    val extractedText: String?,
    @ColumnInfo(name = "primary_subject")
    val primarySubject: String?,
    @ColumnInfo(name = "main_topic")
    val mainTopic: String?,
    @ColumnInfo(name = "user_notes")
    val userNotes: String?,
    @ColumnInfo(name = "source_app_name")
    val sourceAppName: String?,
    @ColumnInfo(name = "original_url")
    val originalUrl: String?,
)
