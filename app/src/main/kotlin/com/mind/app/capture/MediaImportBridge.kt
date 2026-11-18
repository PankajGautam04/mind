package com.mind.app.capture

import android.content.Context
import androidx.core.net.toUri
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.model.ContentType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedCaptureMedia(
    val originalPath: String,
    val thumbnailPath: String?,
    val contentType: ContentType,
)

interface MediaImportBridge {
    suspend fun importUri(uriString: String): ImportedCaptureMedia?
    fun allocateVoiceOutputPath(): String
}

@Singleton
class AndroidMediaImportBridge @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localMediaStorage: LocalMediaStorage,
) : MediaImportBridge {

    override suspend fun importUri(uriString: String): ImportedCaptureMedia? = withContext(Dispatchers.IO) {
        val uri = uriString.toUri()
        val imported = localMediaStorage.importUriToStorage(
            contentResolver = context.contentResolver,
            uri = uri,
            memoryKey = "import_${UUID.randomUUID()}",
        ) ?: return@withContext null
        ImportedCaptureMedia(
            originalPath = imported.originalPath,
            thumbnailPath = imported.thumbnailPath,
            contentType = imported.contentType,
        )
    }

    override fun allocateVoiceOutputPath(): String =
        localMediaStorage.allocateVoiceFile("voice_${UUID.randomUUID()}").absolutePath
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppCaptureModule {
    @Binds
    @Singleton
    abstract fun bindMediaImportBridge(impl: AndroidMediaImportBridge): MediaImportBridge
}
