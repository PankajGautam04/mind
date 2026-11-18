package com.mind.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mind.app.core.database.converter.MindTypeConverters
import com.mind.app.core.database.entity.MemoryEntity
import com.mind.app.core.database.entity.MemoryTagCrossRef
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.database.seed.DefaultCategorySeeder
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.MetadataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindDatabaseTest {
    private lateinit var db: MindDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MindDatabase::class.java)
            .addCallback(MindDatabase.seedCallback())
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedsDefaultCategoriesAndSubcategoriesOnCreate() = runTest {
        val categories = db.categoryDao().observeAllCategories().first()
        val roots = categories.filter { it.parentId == null }
        val children = categories.filter { it.parentId != null }

        assertEquals(8, roots.size)
        assertEquals(31, children.size)
        assertNotNull(db.categoryDao().getById(DefaultCategorySeeder.UNCATEGORIZED_ID))
        assertNotNull(db.categoryDao().getById("cat_technology_aws"))
    }

    @Test
    fun insertsMemoryWithTagsAndRetrievesViaFtsSearch() = runTest {
        val now = 1_700_000_000_000L
        val memory = MemoryEntity(
            id = "mem-aws-1",
            contentType = ContentType.ERROR_LOG,
            aiTitle = "AWS S3 AccessDenied Troubleshooting",
            description = "IAM role missing s3:GetObject permission",
            extractedText = "AccessDeniedException: User is not authorized to perform s3:GetObject",
            primarySubject = "AWS IAM Policy",
            categoryId = "cat_technology",
            subcategoryId = "cat_technology_aws",
            entities = listOf("AWS", "S3", "IAM"),
            detectedObjects = listOf("Terminal Log"),
            capturedAt = now,
            updatedAt = now,
            aiStatus = AiProcessingStatus.COMPLETED,
            isFavourite = true,
            captureMethod = CaptureMethod.QUICK_SETTINGS,
        )
        db.memoryDao().upsertMemory(memory)

        val tag = TagEntity(
            id = "tag-aws",
            name = "AWS",
            normalizedName = "aws",
            isAiGenerated = true,
            createdAt = now,
        )
        db.tagDao().insertTag(tag)
        db.tagDao().upsertMemoryTagRefs(
            listOf(MemoryTagCrossRef(memoryId = memory.id, tagId = tag.id, source = MetadataSource.AI))
        )

        val ftsMatches = db.memoryDao().searchMemoriesFts(
            ftsQuery = "AccessDeniedException*",
            categoryId = null,
            sourcePackageName = null,
            fromEpochMillis = null,
            toEpochMillis = null,
            favouritesOnly = false,
        ).first()

        assertEquals(1, ftsMatches.size)
        assertEquals("Technology", ftsMatches.first().primaryCategory?.name)
        assertEquals("AWS", ftsMatches.first().subcategory?.name)
        assertEquals(listOf("AWS"), ftsMatches.first().tags.map { it.name })

        db.memoryDao().softDelete(memory.id, now + 1000L)
        val afterSoftDelete = db.memoryDao().observeRecentMemories(10).first()
        assertTrue(afterSoftDelete.isEmpty())

        db.memoryDao().restore(memory.id, now + 2000L)
        val afterRestore = db.memoryDao().observeRecentMemories(10).first()
        assertEquals(1, afterRestore.size)
    }

    @Test
    fun typeConvertersRoundTripListsAndEnums() {
        val converters = MindTypeConverters()
        val rawJson = converters.fromStringList(listOf("Samsung", "Galaxy"))
        assertEquals(listOf("Samsung", "Galaxy"), converters.toStringList(rawJson))
        assertEquals(ContentType.PRODUCT, converters.toContentType("PRODUCT"))
        assertEquals(ContentType.OTHER, converters.toContentType("UNKNOWN_VALUE"))
    }
}
