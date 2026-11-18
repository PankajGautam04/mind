package com.mind.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import com.mind.app.core.data.ai.GeminiAiEngineRepository
import com.mind.app.core.data.repository.DataStorePreferencesRepository
import com.mind.app.core.data.repository.RoomAiJobRepository
import com.mind.app.core.data.repository.RoomCategoryRepository
import com.mind.app.core.data.repository.RoomCollectionRepository
import com.mind.app.core.data.repository.RoomMemoryRepository
import com.mind.app.core.data.repository.RoomSearchRepository
import com.mind.app.core.data.repository.RoomSourceAppRepository
import com.mind.app.core.data.repository.RoomTagRepository
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.database.dao.AiProcessingJobDao
import com.mind.app.core.database.dao.CategoryDao
import com.mind.app.core.database.dao.CollectionDao
import com.mind.app.core.database.dao.MemoryConnectionDao
import com.mind.app.core.database.dao.MemoryDao
import com.mind.app.core.database.dao.MemoryEmbeddingDao
import com.mind.app.core.database.dao.SourceApplicationDao
import com.mind.app.core.database.dao.TagDao
import com.mind.app.core.database.migration.MindMigrations
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.AiJobRepository
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SearchRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.mindPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mind_user_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMindDatabase(@ApplicationContext context: Context): MindDatabase {
        return Room.databaseBuilder(
            context,
            MindDatabase::class.java,
            MindDatabase.DATABASE_NAME,
        )
            .addCallback(MindDatabase.seedCallback())
            .addMigrations(*MindMigrations.ALL_MIGRATIONS)
            .build()
    }

    @Provides fun provideMemoryDao(db: MindDatabase): MemoryDao = db.memoryDao()
    @Provides fun provideCategoryDao(db: MindDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTagDao(db: MindDatabase): TagDao = db.tagDao()
    @Provides fun provideCollectionDao(db: MindDatabase): CollectionDao = db.collectionDao()
    @Provides fun provideAiProcessingJobDao(db: MindDatabase): AiProcessingJobDao = db.aiProcessingJobDao()
    @Provides fun provideSourceApplicationDao(db: MindDatabase): SourceApplicationDao = db.sourceApplicationDao()
    @Provides fun provideMemoryConnectionDao(db: MindDatabase): MemoryConnectionDao = db.memoryConnectionDao()
    @Provides fun provideMemoryEmbeddingDao(db: MindDatabase): MemoryEmbeddingDao = db.memoryEmbeddingDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.mindPreferencesDataStore
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindMemoryRepository(impl: RoomMemoryRepository): MemoryRepository
    @Binds abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository
    @Binds abstract fun bindTagRepository(impl: RoomTagRepository): TagRepository
    @Binds abstract fun bindCollectionRepository(impl: RoomCollectionRepository): CollectionRepository
    @Binds abstract fun bindSearchRepository(impl: RoomSearchRepository): SearchRepository
    @Binds abstract fun bindAiJobRepository(impl: RoomAiJobRepository): AiJobRepository
    @Binds abstract fun bindSourceAppRepository(impl: RoomSourceAppRepository): SourceAppRepository
    @Binds abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun bindAiEngineRepository(impl: GeminiAiEngineRepository): AiEngineRepository
}
