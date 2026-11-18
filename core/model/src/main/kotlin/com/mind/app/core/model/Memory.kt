package com.mind.app.core.model

/**
 * Core domain representation of a saved digital memory in Mind.
 */
data class Memory(
    val id: String,
    val originalFilePath: String? = null,
    val thumbnailPath: String? = null,
    val contentType: ContentType = ContentType.SCREENSHOT,
    val aiTitle: String? = null,
    val userTitle: String? = null,
    val description: String? = null,
    val extractedText: String? = null,
    val primarySubject: String? = null,
    val mainTopic: String? = null,
    val contextSummary: String? = null,
    val primaryCategory: Category? = null,
    val subcategory: Category? = null,
    val tags: List<Tag> = emptyList(),
    val entities: List<String> = emptyList(),
    val detectedObjects: List<String> = emptyList(),
    val sourcePackageName: String? = null,
    val sourceAppName: String? = null,
    val originalUrl: String? = null,
    val capturedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val aiStatus: AiProcessingStatus = AiProcessingStatus.PENDING,
    val isFavourite: Boolean = false,
    val userNotes: String? = null,
    val embeddingId: String? = null,
    val confidenceScore: Float? = null,
    val isDeleted: Boolean = false,
    val deletedAtEpochMillis: Long? = null,
    val captureMethod: CaptureMethod = CaptureMethod.IN_APP,
) {
    /** User-edited title takes precedence over AI-generated title, normalized for compact headers. */
    val displayTitle: String
        get() = (userTitle?.takeIf { it.isNotBlank() }
            ?: aiTitle?.takeIf { it.isNotBlank() }
            ?: primarySubject?.takeIf { it.isNotBlank() }
            ?: "Untitled memory")
            .replace(Regex("\\s+"), " ")
            .trim()
}

/**
 * Rich detail model including connected memories and collection memberships.
 */
data class MemoryDetail(
    val memory: Memory,
    val additionalCategories: List<Category> = emptyList(),
    val collections: List<SmartCollection> = emptyList(),
    val connections: List<MemoryConnection> = emptyList(),
    val latestAiJob: AiProcessingJob? = null,
)

/**
 * Link between two related memories (e.g., AWS error + troubleshooting doc).
 */
data class MemoryConnection(
    val sourceMemoryId: String,
    val targetMemoryId: String,
    val targetTitle: String,
    val targetThumbnailPath: String?,
    val connectionType: ConnectionType,
    val strength: Float,
    val reason: String? = null,
    val createdAtEpochMillis: Long,
)

enum class ContentType {
    SCREENSHOT,
    IMAGE,
    VOICE_NOTE,
    URL,
    TEXT,
    QUOTE,
    PRODUCT,
    DOCUMENT,
    PLACE,
    ERROR_LOG,
    OTHER,
}

enum class AiProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    SKIPPED_LOCAL_ONLY,
    CANCELLED,
}

enum class CaptureMethod {
    QUICK_SETTINGS,
    FLOATING_BUTTON,
    WIDGET,
    APP_SHORTCUT,
    SHARESHEET,
    ACCESSIBILITY_SHORTCUT,
    ASSISTANT,
    IN_APP,
}

enum class ConnectionType {
    SAME_ENTITY,
    PROBLEM_SOLUTION,
    RELATED_TOPIC,
    SIMILAR_PRODUCT,
    USER_LINKED,
}
