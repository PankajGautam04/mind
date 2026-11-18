package com.mind.app.core.data.ai

import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.data.mapper.toJson
import com.mind.app.core.data.security.SecureApiKeyStore
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.database.dao.AiProcessingJobDao
import com.mind.app.core.database.dao.CategoryDao
import com.mind.app.core.database.dao.CollectionDao
import com.mind.app.core.database.dao.MemoryConnectionDao
import com.mind.app.core.database.dao.MemoryDao
import com.mind.app.core.database.dao.MemoryEmbeddingDao
import com.mind.app.core.database.dao.SourceApplicationDao
import com.mind.app.core.database.entity.AiProcessingJobEntity
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.entity.CollectionEntity
import com.mind.app.core.database.entity.CollectionMemoryCrossRef
import com.mind.app.core.database.entity.MemoryCategoryCrossRef
import com.mind.app.core.database.entity.MemoryConnectionEntity
import com.mind.app.core.database.entity.MemoryEmbeddingEntity
import com.mind.app.core.database.seed.DefaultCategorySeeder
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SearchRepository
import com.mind.app.core.domain.repository.TagRepository
import com.mind.app.core.model.AiConnectionTestResult
import com.mind.app.core.model.AiJobStatus
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.AskMindAnswer
import com.mind.app.core.model.CollectionRule
import com.mind.app.core.model.ConnectionType
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.GeminiModelInfo
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemorySearchFilter
import com.mind.app.core.model.MetadataSource
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiEngineRepository @Inject constructor(
    private val workManager: WorkManager,
    private val database: MindDatabase,
    private val memoryDao: MemoryDao,
    private val categoryDao: CategoryDao,
    private val collectionDao: CollectionDao,
    private val connectionDao: MemoryConnectionDao,
    private val memoryEmbeddingDao: MemoryEmbeddingDao,
    private val sourceAppDao: SourceApplicationDao,
    private val aiJobDao: AiProcessingJobDao,
    private val tagRepository: TagRepository,
    private val searchRepository: SearchRepository,
    private val preferencesRepository: PreferencesRepository,
    private val secureApiKeyStore: SecureApiKeyStore,
    private val geminiRestClient: GeminiRestClient,
) : AiEngineRepository {

    override suspend fun enqueueAnalysis(memoryId: String) {
        enqueueAnalysisInternal(memoryId, isRecoveryRetry = false)
    }

    override suspend fun enqueueLocalOcr(memoryId: String) {
        val request = OneTimeWorkRequestBuilder<LocalOcrWorker>()
            .setInputData(workDataOf(LocalOcrWorker.KEY_MEMORY_ID to memoryId))
            .addTag("local_ocr_$memoryId")
            .build()

        workManager.enqueueUniqueWork(
            "local_ocr_$memoryId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun scheduleRetryWhenOnline() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<RetryPendingAnalysesWorker>()
            .setConstraints(constraints)
            .setInitialDelay(45, TimeUnit.SECONDS)
            .addTag("retry_pending_analyses")
            .build()

        workManager.enqueueUniqueWork(
            "retry_pending_analyses",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun enqueueFailedRecoveryRetries(): Int {
        val ids = memoryDao.getFailedMemoryIds()
        ids.forEach { enqueueAnalysisInternal(it, isRecoveryRetry = true) }
        return ids.size
    }

    private suspend fun enqueueAnalysisInternal(memoryId: String, isRecoveryRetry: Boolean) {
        val now = System.currentTimeMillis()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AnalyzeMemoryWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    AnalyzeMemoryWorker.KEY_MEMORY_ID to memoryId,
                    AnalyzeMemoryWorker.KEY_IS_RECOVERY_RETRY to isRecoveryRetry,
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("analyze_$memoryId")
            .build()

        aiJobDao.upsertJob(
            AiProcessingJobEntity(
                id = "job_${UUID.randomUUID()}",
                memoryId = memoryId,
                workManagerId = request.id.toString(),
                status = AiJobStatus.QUEUED,
                attemptCount = 0,
                createdAt = now,
                updatedAt = now,
            )
        )

        workManager.enqueueUniqueWork(
            "analyze_memory_$memoryId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override suspend fun cancelAnalysis(memoryId: String) {
        workManager.cancelUniqueWork("analyze_memory_$memoryId")
    }

    override suspend fun analyzeNow(memoryId: String, markFailedOnError: Boolean): Result<Unit> {
        val prefs = preferencesRepository.observePreferences().first()
        if (prefs.localOnlyMode || !prefs.cloudAiEnabled) {
            memoryDao.updateAiStatus(memoryId, AiProcessingStatus.SKIPPED_LOCAL_ONLY, System.currentTimeMillis())
            return Result.failure(IllegalStateException("Cloud AI is disabled. Enable Gemini AI in Settings first."))
        }

        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            if (markFailedOnError) {
                memoryDao.updateAiStatus(memoryId, AiProcessingStatus.FAILED, System.currentTimeMillis())
            }
            return Result.failure(IllegalStateException("Add your Gemini API key or proxy URL in Settings first."))
        }

        val entity = memoryDao.getMemoryEntityById(memoryId)
            ?: return Result.failure(IllegalArgumentException("Memory not found"))
        if (entity.isDeleted) {
            return Result.success(Unit)
        }

        val excludedPackages = sourceAppDao.observeExcludedPackages().first()
        if (!entity.sourcePackageName.isNullOrBlank() && entity.sourcePackageName in excludedPackages) {
            memoryDao.updateAiStatus(memoryId, AiProcessingStatus.SKIPPED_LOCAL_ONLY, System.currentTimeMillis())
            return Result.success(Unit)
        }

        val now = System.currentTimeMillis()
        val jobId = "job_${UUID.randomUUID()}"
        aiJobDao.upsertJob(
            AiProcessingJobEntity(
                id = jobId,
                memoryId = memoryId,
                status = AiJobStatus.RUNNING,
                attemptCount = 1,
                createdAt = now,
                updatedAt = now,
            )
        )
        memoryDao.updateAiStatus(memoryId, AiProcessingStatus.PROCESSING, now)

        val categories = categoryDao.getAllCategoriesOnce()
        val prompt = buildAnalysisPrompt(entity, categories, prefs.aiLanguageTag)
        val (mediaFile, mediaMime) = resolveMediaInput(entity.originalFilePath, entity.contentType)

        val analysisOutcome = GeminiModelFallbackExecutor.execute(
            preferredModel = prefs.geminiModel,
            fetchLiveModels = {
                geminiRestClient.listGenerateContentModels(prefs.customBackendUrl, apiKey)
                    .map { models -> models.map { it.id } }
            },
            onModelSwitched = { newModel ->
                preferencesRepository.setGeminiModel(newModel)
            },
            attempt = { candidateModel ->
                geminiRestClient.generateStructuredAnalysis(
                    baseUrl = prefs.customBackendUrl,
                    apiKey = apiKey,
                    modelId = candidateModel,
                    prompt = prompt,
                    mediaFile = mediaFile,
                    mediaMimeType = mediaMime,
                ).mapCatching { rawResponse ->
                    GeminiResponseParser.parseAnalysis(rawResponse).getOrThrow()
                }
            },
        )

        val parsed = analysisOutcome.getOrElse { error ->
            val errNow = System.currentTimeMillis()
            if (markFailedOnError) {
                memoryDao.updateAiStatus(memoryId, AiProcessingStatus.FAILED, errNow)
            }
            aiJobDao.updateJobStatus(jobId, AiJobStatus.FAILED, error.message, errNow, null)
            return Result.failure(error)
        }.value

        applyAnalysisResults(memoryId, entity, parsed, prefs.autoCategorizationEnabled)
        generateAndStoreEmbedding(
            memoryId = memoryId,
            baseUrl = prefs.customBackendUrl,
            apiKey = apiKey,
        )
        val doneAt = System.currentTimeMillis()
        aiJobDao.updateJobStatus(jobId, AiJobStatus.SUCCEEDED, null, doneAt, doneAt)
        return Result.success(Unit)
    }

    override suspend fun retryFailedAnalyses(): Int {
        val ids = memoryDao.getReanalyzableMemoryIds()
        ids.forEach { enqueueAnalysis(it) }
        return ids.size
    }

    override suspend fun semanticSearch(query: String, limit: Int): Result<List<Memory>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return Result.success(emptyList())

        val prefs = preferencesRepository.observePreferences().first()
        if (prefs.localOnlyMode || !prefs.cloudAiEnabled) {
            return Result.failure(IllegalStateException("Semantic AI search requires Cloud Gemini AI to be enabled in Settings."))
        }
        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            return Result.failure(IllegalStateException("Add your Gemini API key in Settings for Semantic Search."))
        }

        val (queryModelVersion, queryVector) = geminiRestClient.embedText(
            baseUrl = prefs.customBackendUrl,
            apiKey = apiKey,
            text = trimmedQuery,
        ).getOrElse { error ->
            return Result.failure(error)
        }

        // Backfill missing or legacy-model embeddings for recent memories so cosine spaces match.
        backfillMissingEmbeddings(
            baseUrl = prefs.customBackendUrl,
            apiKey = apiKey,
            targetModelVersion = queryModelVersion,
            maxCount = 5,
        )

        val allEmbeddings = memoryEmbeddingDao.getAllEmbeddings()
        val rankedIds = allEmbeddings.mapNotNull { emb ->
            if (emb.modelVersion != queryModelVersion) return@mapNotNull null
            val vec = VectorMath.decodeFloatVector(emb.vectorBlob)
            val sim = VectorMath.cosineSimilarity(queryVector, vec)
            if (sim >= 0.25f) emb.memoryId to sim else null
        }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        val vectorHits = rankedIds.mapNotNull { id ->
            memoryDao.getMemoryWithDetailsById(id)?.toDomain()
        }
        val keywordHits = searchRepository.searchMemories(MemorySearchFilter(query = trimmedQuery)).first()

        val merged = LinkedHashMap<String, Memory>(vectorHits.size + keywordHits.size)
        vectorHits.forEach { merged[it.id] = it }
        keywordHits.forEach { merged.putIfAbsent(it.id, it) }
        return Result.success(merged.values.take(limit).toList())
    }

    override suspend fun askMind(question: String): Result<AskMindAnswer> {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a question to ask Mind."))
        }

        val prefs = preferencesRepository.observePreferences().first()
        if (prefs.localOnlyMode || !prefs.cloudAiEnabled) {
            return Result.failure(IllegalStateException("Ask Mind requires Cloud Gemini AI to be enabled in Settings."))
        }

        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            return Result.failure(IllegalStateException("Add your Gemini API key in Settings to use Ask Mind."))
        }

        val candidates = retrieveRelevantMemories(trimmedQuestion)
        if (candidates.isEmpty()) {
            return Result.success(
                AskMindAnswer(
                    question = trimmedQuestion,
                    answerText = "You don't have any saved memories yet. Save a screenshot, voice note, link, or note first, then ask Mind about it.",
                    citedMemories = emptyList(),
                )
            )
        }

        val contextBlock = candidates.joinToString("\n\n---\n\n") { memory ->
            buildString {
                appendLine("Title: ${memory.displayTitle}")
                memory.primaryCategory?.let { appendLine("Category: ${it.name}") }
                memory.description?.let { appendLine("Description: $it") }
                memory.extractedText?.let { appendLine("Extracted Text: ${it.take(800)}") }
                if (memory.entities.isNotEmpty()) appendLine("Entities: ${memory.entities.joinToString(", ")}")
                if (memory.tags.isNotEmpty()) appendLine("Tags: ${memory.tags.joinToString(", ") { it.name }}")
                memory.originalUrl?.let { appendLine("URL: $it") }
            }.trim()
        }

        val prompt = """
            You are Mind, a private personal memory assistant.
            Answer the user's question using ONLY the saved memories provided below.
            Do not invent details not present in these memories. If the answer isn't in the saved memories, clearly say so.
            Keep the answer concise and cite relevant memory titles.

            USER QUESTION:
            $trimmedQuestion

            SAVED MEMORIES:
            $contextBlock
        """.trimIndent()

        return GeminiModelFallbackExecutor.execute(
            preferredModel = prefs.geminiModel,
            fetchLiveModels = {
                geminiRestClient.listGenerateContentModels(prefs.customBackendUrl, apiKey)
                    .map { models -> models.map { it.id } }
            },
            onModelSwitched = { newModel ->
                preferencesRepository.setGeminiModel(newModel)
            },
            attempt = { candidateModel ->
                geminiRestClient.generateTextAnswer(
                    baseUrl = prefs.customBackendUrl,
                    apiKey = apiKey,
                    modelId = candidateModel,
                    prompt = prompt,
                )
            },
        ).map { outcome ->
            AskMindAnswer(
                question = trimmedQuestion,
                answerText = outcome.value.trim(),
                citedMemories = candidates,
            )
        }
    }

    override suspend fun askAboutMemory(memoryId: String, question: String): Result<String> {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a question about this memory."))
        }

        val prefs = preferencesRepository.observePreferences().first()
        if (prefs.localOnlyMode || !prefs.cloudAiEnabled) {
            return Result.failure(IllegalStateException("Ask about this memory requires Cloud Gemini AI to be enabled in Settings."))
        }

        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            return Result.failure(IllegalStateException("Add your Gemini API key in Settings first."))
        }

        val detail = memoryDao.getMemoryWithDetailsById(memoryId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Memory not found."))
        val (mediaFile, mediaMime) = resolveMediaInput(detail.originalFilePath, detail.contentType)

        val contextBlock = buildString {
            appendLine("Title: ${detail.displayTitle}")
            appendLine("Type: ${detail.contentType.name}")
            detail.primaryCategory?.let { appendLine("Category: ${it.name}") }
            detail.description?.let { appendLine("Summary: $it") }
            detail.userNotes?.let { appendLine("User Notes: $it") }
            detail.extractedText?.let { appendLine("Extracted Text: ${it.take(2500)}") }
            if (detail.entities.isNotEmpty()) appendLine("Entities: ${detail.entities.joinToString(", ")}")
            if (detail.tags.isNotEmpty()) appendLine("Tags: ${detail.tags.joinToString(", ") { it.name }}")
            detail.originalUrl?.let { appendLine("URL: $it") }
        }.trim()

        val prompt = """
            You are Mind, a private personal memory assistant.
            Answer the user's question specifically about THIS saved memory using the attached media (if any) and metadata below.
            Be concise, helpful, and direct. Do not invent facts outside this memory.

            QUESTION:
            $trimmedQuestion

            MEMORY METADATA:
            $contextBlock
        """.trimIndent()

        return GeminiModelFallbackExecutor.execute(
            preferredModel = prefs.geminiModel,
            fetchLiveModels = {
                geminiRestClient.listGenerateContentModels(prefs.customBackendUrl, apiKey)
                    .map { models -> models.map { it.id } }
            },
            onModelSwitched = { newModel ->
                preferencesRepository.setGeminiModel(newModel)
            },
            attempt = { candidateModel ->
                geminiRestClient.generateTextAnswer(
                    baseUrl = prefs.customBackendUrl,
                    apiKey = apiKey,
                    modelId = candidateModel,
                    prompt = prompt,
                    mediaFile = mediaFile,
                    mediaMimeType = mediaMime,
                )
            },
        ).map { it.value.trim() }
    }

    override suspend fun fetchAvailableModels(): Result<List<GeminiModelInfo>> {
        val prefs = preferencesRepository.observePreferences().first()
        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            return Result.failure(IllegalStateException("Enter and save a Gemini API key first."))
        }
        return geminiRestClient.listGenerateContentModels(prefs.customBackendUrl, apiKey)
    }

    override suspend fun testConnection(): AiConnectionTestResult {
        val prefs = preferencesRepository.observePreferences().first()
        val apiKey = secureApiKeyStore.getApiKey()
        if (apiKey.isNullOrBlank() && prefs.customBackendUrl.isBlank()) {
            return AiConnectionTestResult(
                success = false,
                message = "No API key or backend proxy configured.",
            )
        }

        val start = System.currentTimeMillis()
        val result = GeminiModelFallbackExecutor.execute(
            preferredModel = prefs.geminiModel,
            fetchLiveModels = {
                geminiRestClient.listGenerateContentModels(prefs.customBackendUrl, apiKey)
                    .map { models -> models.map { it.id } }
            },
            onModelSwitched = { newModel ->
                preferencesRepository.setGeminiModel(newModel)
            },
            attempt = { candidateModel ->
                geminiRestClient.generateTextAnswer(
                    baseUrl = prefs.customBackendUrl,
                    apiKey = apiKey,
                    modelId = candidateModel,
                    prompt = "Reply with OK.",
                )
            },
        )
        val elapsed = System.currentTimeMillis() - start

        return result.fold(
            onSuccess = { outcome ->
                val modelNotice = if (outcome.switchedFrom != null) {
                    "Model '${outcome.switchedFrom}' unavailable; auto-switched to ${outcome.usedModel}"
                } else {
                    "Connected to ${outcome.usedModel}"
                }
                AiConnectionTestResult(
                    success = true,
                    message = "$modelNotice (${elapsed}ms): ${outcome.value.take(40)}",
                    latencyMs = elapsed,
                )
            },
            onFailure = { error ->
                AiConnectionTestResult(
                    success = false,
                    message = error.message ?: "Connection failed",
                    latencyMs = elapsed,
                )
            },
        )
    }

    private suspend fun applyAnalysisResults(
        memoryId: String,
        existing: com.mind.app.core.database.entity.MemoryEntity,
        parsed: ParsedMemoryAnalysis,
        autoCategorize: Boolean,
    ) {
        val updatedAt = System.currentTimeMillis()
        val (resolvedCatId, resolvedSubcatId) = if (autoCategorize) {
            resolveCategories(parsed.categoryName, parsed.subcategoryName, updatedAt)
        } else {
            existing.categoryId to existing.subcategoryId
        }

        val updatedEntity = existing.copy(
            contentType = parsed.contentType ?: existing.contentType,
            aiTitle = parsed.title ?: existing.aiTitle,
            description = parsed.description ?: existing.description,
            extractedText = parsed.extractedText ?: existing.extractedText,
            primarySubject = parsed.primarySubject ?: existing.primarySubject,
            mainTopic = parsed.mainTopic ?: existing.mainTopic,
            contextSummary = parsed.contextSummary ?: existing.contextSummary,
            categoryId = resolvedCatId,
            subcategoryId = resolvedSubcatId,
            entities = if (parsed.entities.isNotEmpty()) parsed.entities else existing.entities,
            detectedObjects = if (parsed.objects.isNotEmpty()) parsed.objects else existing.detectedObjects,
            confidenceScore = parsed.confidence ?: existing.confidenceScore,
            aiStatus = AiProcessingStatus.COMPLETED,
            updatedAt = updatedAt,
        )

        database.withTransaction {
            memoryDao.upsertMemory(updatedEntity)
            memoryDao.clearCategoryRefsForMemory(memoryId)
            val refs = buildList {
                resolvedCatId?.let { add(MemoryCategoryCrossRef(memoryId, it, isPrimary = true)) }
                resolvedSubcatId?.let { add(MemoryCategoryCrossRef(memoryId, it, isPrimary = false)) }
            }
            if (refs.isNotEmpty()) {
                memoryDao.upsertCategoryRefs(refs)
            }
        }

        if (parsed.tags.isNotEmpty()) {
            tagRepository.setTagsForMemory(memoryId, parsed.tags, MetadataSource.AI)
        }

        linkConnectedMemories(memoryId, parsed)
        if (autoCategorize) {
            updateSuggestedSmartCollections(memoryId, parsed)
        }
    }

    private suspend fun resolveCategories(
        categoryName: String?,
        subcategoryName: String?,
        now: Long,
    ): Pair<String?, String?> {
        if (categoryName.isNullOrBlank()) {
            return DefaultCategorySeeder.UNCATEGORIZED_ID to null
        }

        val existingRoot = categoryDao.findByNameAndParent(categoryName, null)
        val rootId = if (existingRoot != null) {
            existingRoot.id
        } else {
            val newId = "cat_ai_${UUID.randomUUID()}"
            categoryDao.upsert(
                CategoryEntity(
                    id = newId,
                    name = categoryName,
                    parentId = null,
                    isSystemDefault = false,
                    isAiSuggested = true,
                    sortOrder = 50,
                    createdAt = now,
                )
            )
            newId
        }

        if (subcategoryName.isNullOrBlank() || subcategoryName.equals(categoryName, ignoreCase = true)) {
            return rootId to null
        }

        val existingSub = categoryDao.findByNameAndParent(subcategoryName, rootId)
        val subId = if (existingSub != null) {
            existingSub.id
        } else {
            val newSubId = "cat_ai_${UUID.randomUUID()}"
            categoryDao.upsert(
                CategoryEntity(
                    id = newSubId,
                    name = subcategoryName,
                    parentId = rootId,
                    isSystemDefault = false,
                    isAiSuggested = true,
                    sortOrder = 50,
                    createdAt = now,
                )
            )
            newSubId
        }

        return rootId to subId
    }

    private suspend fun linkConnectedMemories(
        memoryId: String,
        parsed: ParsedMemoryAnalysis,
    ) {
        val recent = memoryDao.getRecentMemoryDetailsSnapshot(40)
            .filter { it.memory.id != memoryId }
        if (recent.isEmpty()) return

        val myEntities = parsed.entities.map { it.lowercase() }.toSet()
        val myTags = parsed.tags.map { it.lowercase() }.toSet()
        val now = System.currentTimeMillis()

        val newConnections = recent.mapNotNull { candidate ->
            val candEntities = candidate.memory.entities.map { it.lowercase() }.toSet()
            val candTags = candidate.tags.map { it.name.lowercase() }.toSet()
            val sharedEntities = myEntities.intersect(candEntities)
            val sharedTags = myTags.intersect(candTags)
            val score = (sharedEntities.size * 2f) + sharedTags.size.toFloat()
            if (score < 2f) return@mapNotNull null

            val type = when {
                parsed.contentType == ContentType.ERROR_LOG && candidate.memory.contentType == ContentType.DOCUMENT ->
                    ConnectionType.PROBLEM_SOLUTION
                parsed.contentType == ContentType.PRODUCT && candidate.memory.contentType == ContentType.PRODUCT ->
                    ConnectionType.SIMILAR_PRODUCT
                sharedEntities.isNotEmpty() -> ConnectionType.SAME_ENTITY
                else -> ConnectionType.RELATED_TOPIC
            }
            val reason = when {
                sharedEntities.isNotEmpty() -> "Shares ${sharedEntities.take(2).joinToString(", ")}"
                else -> "Related tags: ${sharedTags.take(2).joinToString(", ")}"
            }

            MemoryConnectionEntity(
                sourceMemoryId = memoryId,
                targetMemoryId = candidate.memory.id,
                connectionType = type,
                strength = (score / 6f).coerceIn(0.3f, 1f),
                reason = reason,
                createdAt = now,
            )
        }.take(5)

        if (newConnections.isNotEmpty()) {
            connectionDao.upsertConnections(newConnections)
        }
    }

    private suspend fun updateSuggestedSmartCollections(
        memoryId: String,
        parsed: ParsedMemoryAnalysis,
    ) {
        val candidateName = parsed.suggestedCollection
            ?: parsed.entities.firstOrNull()?.let { "$it Collection" }
            ?: return

        val now = System.currentTimeMillis()
        val existing = collectionDao.findByName(candidateName)
        if (existing != null) {
            collectionDao.insertMemoryRef(
                CollectionMemoryCrossRef(
                    collectionId = existing.id,
                    memoryId = memoryId,
                    addedAt = now,
                    isAutoAdded = true,
                )
            )
            return
        }

        val primaryEntity = parsed.entities.firstOrNull() ?: return
        val matchingRecent = memoryDao.getRecentMemoryDetailsSnapshot(40)
            .filter { row -> row.memory.entities.any { it.equals(primaryEntity, ignoreCase = true) } }
        if (matchingRecent.size >= 2) {
            val colId = "col_smart_${UUID.randomUUID()}"
            collectionDao.upsertCollection(
                CollectionEntity(
                    id = colId,
                    name = candidateName,
                    description = "Automatically grouped memories related to $primaryEntity",
                    isSmartCollection = true,
                    isSuggested = true,
                    ruleJson = CollectionRule(entities = listOf(primaryEntity)).toJson(),
                    coverMemoryId = memoryId,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            matchingRecent.forEach { row ->
                collectionDao.insertMemoryRef(
                    CollectionMemoryCrossRef(
                        collectionId = colId,
                        memoryId = row.memory.id,
                        addedAt = now,
                        isAutoAdded = true,
                    )
                )
            }
        }
    }

    private suspend fun generateAndStoreEmbedding(
        memoryId: String,
        baseUrl: String,
        apiKey: String?,
    ) {
        val detail = memoryDao.getMemoryWithDetailsById(memoryId)?.toDomain() ?: return
        val document = buildString {
            appendLine(detail.displayTitle)
            detail.description?.let { appendLine(it) }
            detail.mainTopic?.let { appendLine(it) }
            detail.contextSummary?.let { appendLine(it) }
            if (detail.entities.isNotEmpty()) appendLine(detail.entities.joinToString(", "))
            if (detail.tags.isNotEmpty()) appendLine(detail.tags.joinToString(", ") { it.name })
            detail.extractedText?.let { appendLine(it.take(1500)) }
        }.trim()
        if (document.isBlank()) return

        geminiRestClient.embedText(
            baseUrl = baseUrl,
            apiKey = apiKey,
            text = document,
        ).onSuccess { (embModel, vector) ->
            memoryEmbeddingDao.upsertEmbedding(
                MemoryEmbeddingEntity(
                    id = "emb_$memoryId",
                    memoryId = memoryId,
                    modelVersion = embModel,
                    vectorBlob = VectorMath.encodeFloatVector(vector),
                    dimension = vector.size,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private suspend fun backfillMissingEmbeddings(
        baseUrl: String,
        apiKey: String?,
        targetModelVersion: String,
        maxCount: Int,
    ) {
        val currentModelIds = memoryEmbeddingDao.getAllEmbeddings()
            .filter { it.modelVersion == targetModelVersion }
            .map { it.memoryId }
            .toSet()
        val recentIds = memoryDao.getRecentMemoryDetailsSnapshot(30)
            .map { it.memory.id }
            .filterNot { it in currentModelIds }
            .take(maxCount)
        recentIds.forEach { id ->
            runCatching { generateAndStoreEmbedding(id, baseUrl, apiKey) }
        }
    }

    private suspend fun retrieveRelevantMemories(question: String): List<Memory> {
        val semanticHits = semanticSearch(question, limit = 6).getOrDefault(emptyList())
        val ftsMatches = searchRepository.searchMemories(MemorySearchFilter(query = question)).first()
        val merged = LinkedHashMap<String, Memory>(semanticHits.size + ftsMatches.size)
        semanticHits.forEach { merged[it.id] = it }
        ftsMatches.forEach { merged.putIfAbsent(it.id, it) }
        if (merged.isNotEmpty()) {
            return merged.values.take(8).toList()
        }

        val recent = memoryDao.getRecentMemoryDetailsSnapshot(30).map { it.toDomain() }
        val tokens = question.lowercase().split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
        if (tokens.isEmpty()) return recent.take(6)

        val scored = recent.map { memory ->
            val haystack = listOfNotNull(
                memory.displayTitle,
                memory.description,
                memory.extractedText,
                memory.primaryCategory?.name,
                memory.entities.joinToString(" "),
                memory.tags.joinToString(" ") { it.name },
            ).joinToString(" ").lowercase()
            val hits = tokens.count { it in haystack }
            memory to hits
        }.sortedByDescending { it.second }

        return scored.filter { it.second > 0 }.map { it.first }.ifEmpty { recent.take(6) }.take(8)
    }

    private fun resolveMediaInput(path: String?, contentType: ContentType): Pair<File?, String?> {
        if (path.isNullOrBlank()) return null to null
        val file = File(path)
        if (!file.exists()) return null to null
        return when {
            contentType == ContentType.VOICE_NOTE || file.extension.equals("m4a", true) ->
                file to "audio/mp4"
            file.extension.equals("png", true) -> file to "image/png"
            file.extension.equals("webp", true) -> file to "image/webp"
            else -> file to "image/jpeg"
        }
    }

    private fun buildAnalysisPrompt(
        entity: com.mind.app.core.database.entity.MemoryEntity,
        categories: List<CategoryEntity>,
        languageTag: String,
    ): String {
        val rootNames = categories.filter { it.parentId == null }.joinToString(", ") { it.name }
        return """
            Analyze this captured personal memory for the Mind Android app (response language: $languageTag).
            Reuse existing root categories whenever appropriate: [$rootNames].
            Return ONLY valid JSON with these exact keys:
            {
              "title": "concise meaningful title",
              "content_type": "screenshot|image|voice_note|url|text|quote|product|document|place|error_log|other",
              "primary_subject": "main subject",
              "category": "best category name",
              "subcategory": "best subcategory name or null",
              "tags": ["up to 6 concise searchable tags"],
              "description": "clear summary of context and intent",
              "entities": ["brands, people, products, services, places"],
              "objects": ["visible objects or audio elements"],
              "extracted_text": "exact OCR text or speech transcript if present, else empty string",
              "main_topic": "short topic label",
              "context": "why the user likely saved this",
              "confidence": 0.95,
              "suggested_collection": "optional collection name or null"
            }

            Known metadata:
            - Content type hint: ${entity.contentType.name}
            - Existing text: ${entity.extractedText.orEmpty()}
            - Original URL: ${entity.originalUrl.orEmpty()}
            - Source app: ${entity.sourceAppName.orEmpty()}
        """.trimIndent()
    }
}
