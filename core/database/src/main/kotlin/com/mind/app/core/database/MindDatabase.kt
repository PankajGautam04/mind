package com.mind.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mind.app.core.database.converter.MindTypeConverters
import com.mind.app.core.database.dao.AiProcessingJobDao
import com.mind.app.core.database.dao.CategoryDao
import com.mind.app.core.database.dao.CollectionDao
import com.mind.app.core.database.dao.MemoryConnectionDao
import com.mind.app.core.database.dao.MemoryDao
import com.mind.app.core.database.dao.MemoryEmbeddingDao
import com.mind.app.core.database.dao.SourceApplicationDao
import com.mind.app.core.database.dao.TagDao
import com.mind.app.core.database.entity.AiProcessingJobEntity
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.entity.CollectionEntity
import com.mind.app.core.database.entity.CollectionMemoryCrossRef
import com.mind.app.core.database.entity.MemoryCategoryCrossRef
import com.mind.app.core.database.entity.MemoryConnectionEntity
import com.mind.app.core.database.entity.MemoryEmbeddingEntity
import com.mind.app.core.database.entity.MemoryEntity
import com.mind.app.core.database.entity.MemoryFtsEntity
import com.mind.app.core.database.entity.MemoryTagCrossRef
import com.mind.app.core.database.entity.SourceApplicationEntity
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.database.seed.DefaultCategorySeeder

@Database(
    entities = [
        MemoryEntity::class,
        MemoryFtsEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        MemoryTagCrossRef::class,
        MemoryCategoryCrossRef::class,
        CollectionEntity::class,
        CollectionMemoryCrossRef::class,
        AiProcessingJobEntity::class,
        SourceApplicationEntity::class,
        MemoryConnectionEntity::class,
        MemoryEmbeddingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MindTypeConverters::class)
abstract class MindDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun aiProcessingJobDao(): AiProcessingJobDao
    abstract fun sourceApplicationDao(): SourceApplicationDao
    abstract fun memoryConnectionDao(): MemoryConnectionDao
    abstract fun memoryEmbeddingDao(): MemoryEmbeddingDao

    companion object {
        const val DATABASE_NAME = "mind_memory.db"

        fun seedCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DefaultCategorySeeder.seedInto(db)
            }
        }
    }
}
