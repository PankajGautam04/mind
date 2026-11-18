package com.mind.app.core.data

import com.mind.app.core.data.repository.FtsQuerySanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQuerySanitizerTest {
    @Test
    fun sanitizesPlainWordsIntoPrefixQuery() {
        assertEquals("samsung* galaxy*", FtsQuerySanitizer.toPrefixFtsQuery("Samsung Galaxy"))
    }

    @Test
    fun stripsUnsafeFtsOperatorsAndPunctuation() {
        assertEquals(
            "accessdeniedexception* user* arn* aws* iam*",
            FtsQuerySanitizer.toPrefixFtsQuery("AccessDeniedException: \"User\" -arn:aws:iam*"),
        )
        assertEquals(
            "not* or* and* near*",
            FtsQuerySanitizer.toPrefixFtsQuery("NOT OR AND NEAR"),
        )
    }

    @Test
    fun returnsNullForBlankOrPunctuationOnlyQueries() {
        assertNull(FtsQuerySanitizer.toPrefixFtsQuery("   "))
        assertNull(FtsQuerySanitizer.toPrefixFtsQuery("---:::**"))
    }
}
