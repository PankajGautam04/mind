package com.mind.app.feature.memorydetail

import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.Memory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySmartActionsTest {

    @Test
    fun `detects otp code email phone and link from memory text`() {
        val memory = sampleMemory(
            extractedText = """
                Your verification code is 492817.
                Contact support@mindapp.io or +1 (415) 555-0199 for help.
                Visit https://www.mindapp.io/docs for details.
            """.trimIndent(),
        )

        val actions = MemorySmartActions.detect(memory)

        assertEquals(4, actions.size)
        assertEquals(SmartActionKind.COPY_CODE, actions[0].kind)
        assertEquals("492817", actions[0].value)
        assertEquals(SmartActionKind.SEND_EMAIL, actions[1].kind)
        assertEquals("support@mindapp.io", actions[1].value)
        assertEquals(SmartActionKind.CALL_PHONE, actions[2].kind)
        assertEquals("+14155550199", actions[2].value)
        assertEquals(SmartActionKind.OPEN_URL, actions[3].kind)
        assertEquals("Open mindapp.io", actions[3].label)
    }

    @Test
    fun `does not confuse dates or ips with phone numbers`() {
        val memory = sampleMemory(
            extractedText = "Build 192.168.1.42 failed on 2026-11-14 with status 500.",
        )

        val actions = MemorySmartActions.detect(memory)

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `prefers promo code and originalUrl when present`() {
        val memory = sampleMemory(
            extractedText = "Use promo code: SAVE25 at checkout.",
            originalUrl = "https://shop.example.com/cart",
        )

        val actions = MemorySmartActions.detect(memory)

        assertEquals(2, actions.size)
        assertEquals("SAVE25", actions[0].value)
        assertEquals("Open shop.example.com", actions[1].label)
    }

    private fun sampleMemory(
        extractedText: String? = null,
        originalUrl: String? = null,
    ) = Memory(
        id = "m1",
        aiTitle = "Sample",
        contentType = ContentType.SCREENSHOT,
        extractedText = extractedText,
        originalUrl = originalUrl,
        captureMethod = CaptureMethod.IN_APP,
        capturedAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 1_000L,
        aiStatus = AiProcessingStatus.COMPLETED,
    )
}
