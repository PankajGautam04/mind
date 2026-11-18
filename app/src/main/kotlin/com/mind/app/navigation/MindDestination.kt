package com.mind.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes for Mind.
 */
sealed interface MindDestination {
    @Serializable
    data object Home : MindDestination

    @Serializable
    data object Collections : MindDestination

    @Serializable
    data class CollectionDetail(val collectionId: String) : MindDestination

    @Serializable
    data class Search(val categoryId: String? = null) : MindDestination

    @Serializable
    data object Favourites : MindDestination

    @Serializable
    data class MemoryDetail(val memoryId: String) : MindDestination

    @Serializable
    data object Settings : MindDestination
}
