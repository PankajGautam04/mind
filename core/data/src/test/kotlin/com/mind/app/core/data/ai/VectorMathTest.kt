package com.mind.app.core.data.ai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VectorMathTest {

    @Test
    fun encodeAndDecodeRoundTripPreservesFloats() {
        val original = floatArrayOf(0.125f, -0.5f, 1.0f, 42.75f, 0f)
        val encoded = VectorMath.encodeFloatVector(original)
        val decoded = VectorMath.decodeFloatVector(encoded)
        assertArrayEquals(original, decoded, 1e-6f)
    }

    @Test
    fun decodeRejectsEmptyOrMisalignedBytes() {
        assertEquals(0, VectorMath.decodeFloatVector(ByteArray(0)).size)
        assertEquals(0, VectorMath.decodeFloatVector(byteArrayOf(1, 2, 3)).size)
    }

    @Test
    fun cosineSimilarityHandlesIdenticalOrthogonalOppositeAndMismatched() {
        val a = floatArrayOf(1f, 2f, 3f)
        val sameScaled = floatArrayOf(2f, 4f, 6f)
        val ortho = floatArrayOf(1f, 0f, 0f)
        val ortho2 = floatArrayOf(0f, 1f, 0f)
        val opposite = floatArrayOf(-1f, -2f, -3f)

        assertEquals(1f, VectorMath.cosineSimilarity(a, sameScaled), 1e-5f)
        assertEquals(0f, VectorMath.cosineSimilarity(ortho, ortho2), 1e-5f)
        assertEquals(-1f, VectorMath.cosineSimilarity(a, opposite), 1e-5f)
        assertEquals(0f, VectorMath.cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(1f)), 1e-5f)
        assertEquals(0f, VectorMath.cosineSimilarity(FloatArray(0), FloatArray(0)), 1e-5f)
    }
}
