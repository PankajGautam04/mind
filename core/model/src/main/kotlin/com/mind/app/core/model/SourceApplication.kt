package com.mind.app.core.model

/**
 * Known source application tracked for filtering and capture exclusion privacy controls.
 */
data class SourceApplication(
    val packageName: String,
    val displayName: String,
    val isExcluded: Boolean = false,
    val captureCount: Int = 0,
    val lastCapturedAtEpochMillis: Long? = null,
)
