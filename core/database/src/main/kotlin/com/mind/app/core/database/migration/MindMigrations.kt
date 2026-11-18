package com.mind.app.core.database.migration

import androidx.room.migration.Migration

/**
 * Explicit Room schema migrations registry. Add new schema transitions here rather than
 * using destructive fallbacks so user memories are always preserved across upgrades.
 */
object MindMigrations {
    val ALL_MIGRATIONS: Array<Migration> = emptyArray()
}
