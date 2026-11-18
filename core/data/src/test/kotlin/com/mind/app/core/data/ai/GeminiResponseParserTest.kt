package com.mind.app.core.data.ai

import com.mind.app.core.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiResponseParserTest {

    @Test
    fun parsesCodeFencedJsonAndDeduplicatesTags() {
        val raw = """
            ```json
            {
              "title": "Samsung Galaxy S25 Ultra Listing",
              "content_type": "product",
              "primary_subject": "Galaxy S25 Ultra",
              "category": "Shopping",
              "subcategory": "Wishlist",
              "tags": ["samsung", "Samsung", " smartphone ", "flagship", ""],
              "description": "Flagship Android phone spotted while browsing deals.",
              "entities": ["Samsung", "Snapdragon 8 Elite"],
              "objects": ["smartphone"],
              "extracted_text": "Galaxy S25 Ultra 512GB Titanium",
              "main_topic": "Consumer Electronics",
              "context": "Shopping comparison",
              "confidence": 1.35,
              "suggested_collection": "Phone Upgrade"
            }
            ```
        """.trimIndent()

        val result = GeminiResponseParser.parseAnalysis(raw)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Samsung Galaxy S25 Ultra Listing", parsed.title)
        assertEquals(ContentType.PRODUCT, parsed.contentType)
        assertEquals("Shopping", parsed.categoryName)
        assertEquals("Wishlist", parsed.subcategoryName)
        assertEquals(listOf("samsung", "smartphone", "flagship"), parsed.tags)
        assertEquals(1.0f, parsed.confidence)
        assertEquals("Phone Upgrade", parsed.suggestedCollection)
    }

    @Test
    fun mapsErrorLogAndVoiceNoteContentTypes() {
        val rawError = """{"title": "AWS S3 AccessDenied", "content_type": "error_log", "tags": ["aws", "s3"]}"""
        val errParsed = GeminiResponseParser.parseAnalysis(rawError).getOrThrow()
        assertEquals(ContentType.ERROR_LOG, errParsed.contentType)

        val rawVoice = """{"title": "Sprint Standup", "content_type": "voice_memo"}"""
        val voiceParsed = GeminiResponseParser.parseAnalysis(rawVoice).getOrThrow()
        assertEquals(ContentType.VOICE_NOTE, voiceParsed.contentType)
    }

    @Test
    fun rejectsResponseWithoutJsonObject() {
        val result = GeminiResponseParser.parseAnalysis("Sorry, I cannot process this.")
        assertTrue(result.isFailure)
    }
}
