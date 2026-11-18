package com.mind.app.core.data.mapper

import com.mind.app.core.database.entity.AiProcessingJobEntity
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.entity.MemoryEntity
import com.mind.app.core.database.entity.SourceApplicationEntity
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.database.model.CollectionWithCount
import com.mind.app.core.database.model.ConnectedMemoryRow
import com.mind.app.core.database.model.MemoryWithDetails
import com.mind.app.core.model.AiProcessingJob
import com.mind.app.core.model.Category
import com.mind.app.core.model.CollectionRule
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemoryConnection
import com.mind.app.core.model.SmartCollection
import com.mind.app.core.model.SourceApplication
import com.mind.app.core.model.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val mapperJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CollectionRulePayload(
    val entities: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    parentId = parentId,
    iconName = iconName,
    colorHex = colorHex,
    isSystemDefault = isSystemDefault,
    isAiSuggested = isAiSuggested,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt,
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    isAiGenerated = isAiGenerated,
    createdAtEpochMillis = createdAt,
)

fun MemoryWithDetails.toDomain(): Memory = Memory(
    id = memory.id,
    originalFilePath = memory.originalFilePath,
    thumbnailPath = memory.thumbnailPath,
    contentType = memory.contentType,
    aiTitle = memory.aiTitle,
    userTitle = memory.userTitle,
    description = memory.description,
    extractedText = memory.extractedText,
    primarySubject = memory.primarySubject,
    mainTopic = memory.mainTopic,
    contextSummary = memory.contextSummary,
    primaryCategory = primaryCategory?.toDomain(),
    subcategory = subcategory?.toDomain(),
    tags = tags.map { it.toDomain() },
    entities = memory.entities,
    detectedObjects = memory.detectedObjects,
    sourcePackageName = memory.sourcePackageName,
    sourceAppName = memory.sourceAppName,
    originalUrl = memory.originalUrl,
    capturedAtEpochMillis = memory.capturedAt,
    updatedAtEpochMillis = memory.updatedAt,
    aiStatus = memory.aiStatus,
    isFavourite = memory.isFavourite,
    userNotes = memory.userNotes,
    embeddingId = memory.embeddingId,
    confidenceScore = memory.confidenceScore,
    isDeleted = memory.isDeleted,
    deletedAtEpochMillis = memory.deletedAt,
    captureMethod = memory.captureMethod,
)

fun Memory.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    originalFilePath = originalFilePath,
    thumbnailPath = thumbnailPath,
    contentType = contentType,
    aiTitle = aiTitle,
    userTitle = userTitle,
    description = description,
    extractedText = extractedText,
    primarySubject = primarySubject,
    mainTopic = mainTopic,
    contextSummary = contextSummary,
    categoryId = primaryCategory?.id,
    subcategoryId = subcategory?.id,
    entities = entities,
    detectedObjects = detectedObjects,
    sourcePackageName = sourcePackageName,
    sourceAppName = sourceAppName,
    originalUrl = originalUrl,
    capturedAt = capturedAtEpochMillis,
    updatedAt = updatedAtEpochMillis,
    aiStatus = aiStatus,
    isFavourite = isFavourite,
    userNotes = userNotes,
    embeddingId = embeddingId,
    confidenceScore = confidenceScore,
    isDeleted = isDeleted,
    deletedAt = deletedAtEpochMillis,
    captureMethod = captureMethod,
)

fun CollectionWithCount.toDomain(): SmartCollection {
    val parsedRule = collection.ruleJson?.let { raw ->
        runCatching {
            val payload = mapperJson.decodeFromString<CollectionRulePayload>(raw)
            CollectionRule(
                entities = payload.entities,
                categoryIds = payload.categoryIds,
                tags = payload.tags,
                keywords = payload.keywords,
            )
        }.getOrNull()
    }
    return SmartCollection(
        id = collection.id,
        name = collection.name,
        description = collection.description,
        isSmartCollection = collection.isSmartCollection,
        isSuggested = collection.isSuggested,
        rule = parsedRule,
        coverMemoryId = collection.coverMemoryId,
        coverThumbnailPath = coverThumbnailPath,
        iconName = collection.iconName,
        memoryCount = memoryCount,
        createdAtEpochMillis = collection.createdAt,
        updatedAtEpochMillis = collection.updatedAt,
    )
}

fun CollectionRule.toJson(): String = mapperJson.encodeToString(
    CollectionRulePayload(
        entities = entities,
        categoryIds = categoryIds,
        tags = tags,
        keywords = keywords,
    )
)

fun AiProcessingJobEntity.toDomain(): AiProcessingJob = AiProcessingJob(
    id = id,
    memoryId = memoryId,
    workManagerId = workManagerId,
    status = status,
    attemptCount = attemptCount,
    lastError = lastError,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt,
    completedAtEpochMillis = completedAt,
)

fun AiProcessingJob.toEntity(): AiProcessingJobEntity = AiProcessingJobEntity(
    id = id,
    memoryId = memoryId,
    workManagerId = workManagerId,
    status = status,
    attemptCount = attemptCount,
    lastError = lastError,
    createdAt = createdAtEpochMillis,
    updatedAt = updatedAtEpochMillis,
    completedAt = completedAtEpochMillis,
)

fun SourceApplicationEntity.toDomain(): SourceApplication = SourceApplication(
    packageName = packageName,
    displayName = displayName,
    isExcluded = isExcluded,
    captureCount = captureCount,
    lastCapturedAtEpochMillis = lastCapturedAt,
)

fun ConnectedMemoryRow.toDomain(): MemoryConnection {
    val title = targetUserTitle?.takeIf { it.isNotBlank() }
        ?: targetAiTitle?.takeIf { it.isNotBlank() }
        ?: targetPrimarySubject?.takeIf { it.isNotBlank() }
        ?: "Untitled memory"
    return MemoryConnection(
        sourceMemoryId = sourceMemoryId,
        targetMemoryId = targetMemoryId,
        targetTitle = title,
        targetThumbnailPath = targetThumbnailPath,
        connectionType = connectionType,
        strength = strength,
        reason = reason,
        createdAtEpochMillis = createdAt,
    )
}
