package com.mind.app.feature.memorydetail

import com.mind.app.core.model.Memory
import java.net.URI

enum class SmartActionKind {
    COPY_CODE,
    OPEN_URL,
    CALL_PHONE,
    SEND_EMAIL,
}

data class MemorySmartAction(
    val id: String,
    val label: String,
    val value: String,
    val kind: SmartActionKind,
)

/**
 * Extracts up to four high-signal, zero-clutter contextual actions directly from
 * a memory's OCR text, user notes, entities, and original URL.
 */
object MemorySmartActions {

    private val otpRegex = Regex(
        """(?i)\b(?:otp|one[- ]time\s*passcode|verification(?:\s+code)?|security\s+code|passcode|2fa|auth(?:\s+code)?|pin(?:\s+code)?)\b\s*(?:is|:|#|-)*\s*\b([0-9]{4,8})\b"""
    )
    private val promoRegex = Regex(
        """(?i)\b(?:promo|coupon|voucher|referral|discount)\s*(?:code)?\s*(?:is|:|#|-)+\s*\b([A-Z0-9-]{4,16})\b"""
    )
    private val emailRegex = Regex(
        """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""
    )
    private val urlRegex = Regex(
        """https?://[^\s<>"'\])]+""",
        RegexOption.IGNORE_CASE,
    )
    private val formattedPhoneRegex = Regex(
        """(?<![\w./-])(?:\+\d{1,3}[\s.-]?)?(?:\(\d{2,4}\)|\d{2,5})[\s.-]?\d{3,5}[\s.-]?\d{3,5}(?![\w./-])"""
    )
    private val isoDatePattern = Regex("""^\d{4}[-/]\d{1,2}[-/]\d{1,2}$""")
    private val dottedIpOrVersion = Regex("""^\d+\.\d+\.\d+(?:\.\d+)?$""")

    fun detect(memory: Memory): List<MemorySmartAction> {
        val text = buildString {
            memory.extractedText?.let { appendLine(it) }
            memory.userNotes?.let { appendLine(it) }
            memory.description?.let { appendLine(it) }
        }.trim()

        val actions = mutableListOf<MemorySmartAction>()

        detectVerificationOrPromoCode(text)?.let(actions::add)
        detectEmail(text)?.let(actions::add)
        detectPhoneNumber(text)?.let(actions::add)
        detectUrl(memory.originalUrl, text)?.let(actions::add)

        return actions.take(4)
    }

    private fun detectVerificationOrPromoCode(text: String): MemorySmartAction? {
        if (text.isBlank()) return null
        val otpCode = otpRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!otpCode.isNullOrBlank()) {
            return MemorySmartAction(
                id = "code_$otpCode",
                label = "Copy code $otpCode",
                value = otpCode,
                kind = SmartActionKind.COPY_CODE,
            )
        }
        val promoCode = promoRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!promoCode.isNullOrBlank() && promoCode.any { it.isDigit() || it.isUpperCase() }) {
            return MemorySmartAction(
                id = "promo_$promoCode",
                label = "Copy code $promoCode",
                value = promoCode,
                kind = SmartActionKind.COPY_CODE,
            )
        }
        return null
    }

    private fun detectEmail(text: String): MemorySmartAction? {
        if (text.isBlank()) return null
        val email = emailRegex.find(text)?.value?.trim() ?: return null
        return MemorySmartAction(
            id = "email_$email",
            label = "Email $email",
            value = email,
            kind = SmartActionKind.SEND_EMAIL,
        )
    }

    private fun detectPhoneNumber(text: String): MemorySmartAction? {
        if (text.isBlank()) return null
        val match = formattedPhoneRegex.findAll(text)
            .map { it.value.trim() }
            .firstOrNull { candidate ->
                if (isoDatePattern.matches(candidate) || dottedIpOrVersion.matches(candidate)) {
                    return@firstOrNull false
                }
                val digitsOnly = candidate.filter { it.isDigit() }
                if (digitsOnly.length !in 10..15) return@firstOrNull false
                val hasPhoneFormatting = candidate.startsWith("+") ||
                    candidate.contains('(') ||
                    candidate.contains(' ') ||
                    candidate.count { it == '-' } >= 2 ||
                    digitsOnly.length == 10
                hasPhoneFormatting
            } ?: return null

        val normalized = buildString {
            if (match.trimStart().startsWith("+")) append('+')
            match.filterTo(this) { it.isDigit() }
        }
        return MemorySmartAction(
            id = "phone_$normalized",
            label = "Call $match",
            value = normalized,
            kind = SmartActionKind.CALL_PHONE,
        )
    }

    private fun detectUrl(originalUrl: String?, text: String): MemorySmartAction? {
        val rawUrl = originalUrl?.takeIf { it.isNotBlank() }
            ?: urlRegex.find(text)?.value?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
            ?: return null

        val normalizedUrl = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        val host = runCatching {
            URI(normalizedUrl).host?.removePrefix("www.")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "link"

        return MemorySmartAction(
            id = "url_$normalizedUrl",
            label = "Open $host",
            value = normalizedUrl,
            kind = SmartActionKind.OPEN_URL,
        )
    }
}
