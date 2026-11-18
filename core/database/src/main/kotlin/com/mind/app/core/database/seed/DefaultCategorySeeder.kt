package com.mind.app.core.database.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mind.app.core.database.entity.CategoryEntity

/**
 * Canonical default hierarchical category catalog seeded when the Room database is created.
 */
object DefaultCategorySeeder {
    const val UNCATEGORIZED_ID = "cat_uncategorized"

    fun defaultCategories(seedTimestampMillis: Long = 0L): List<CategoryEntity> {
        val result = mutableListOf<CategoryEntity>()
        var order = 0

        fun addRoot(id: String, name: String, iconName: String, colorHex: Long, children: List<Pair<String, String>>) {
            result += CategoryEntity(
                id = id,
                name = name,
                parentId = null,
                iconName = iconName,
                colorHex = colorHex,
                isSystemDefault = true,
                isAiSuggested = false,
                sortOrder = order++,
                createdAt = seedTimestampMillis,
            )
            children.forEachIndexed { idx, (childId, childName) ->
                result += CategoryEntity(
                    id = childId,
                    name = childName,
                    parentId = id,
                    iconName = iconName,
                    colorHex = colorHex,
                    isSystemDefault = true,
                    isAiSuggested = false,
                    sortOrder = idx,
                    createdAt = seedTimestampMillis,
                )
            }
        }

        addRoot(
            id = "cat_technology",
            name = "Technology",
            iconName = "memory",
            colorHex = 0xFF4F46E5,
            children = listOf(
                "cat_technology_android" to "Android",
                "cat_technology_programming" to "Programming",
                "cat_technology_aws" to "AWS",
                "cat_technology_cloud" to "Cloud Computing",
                "cat_technology_ai" to "AI and Machine Learning",
                "cat_technology_gadgets" to "Gadgets",
            ),
        )

        addRoot(
            id = "cat_shopping",
            name = "Shopping",
            iconName = "shopping_bag",
            colorHex = 0xFF0D9488,
            children = listOf(
                "cat_shopping_electronics" to "Electronics",
                "cat_shopping_clothing" to "Clothing",
                "cat_shopping_accessories" to "Accessories",
                "cat_shopping_products" to "Products",
                "cat_shopping_wishlist" to "Wishlist",
            ),
        )

        addRoot(
            id = "cat_places",
            name = "Places",
            iconName = "place",
            colorHex = 0xFFD97706,
            children = listOf(
                "cat_places_restaurants" to "Restaurants",
                "cat_places_hotels" to "Hotels",
                "cat_places_travel" to "Travel Destinations",
                "cat_places_attractions" to "Attractions",
            ),
        )

        addRoot(
            id = "cat_learning",
            name = "Learning",
            iconName = "school",
            colorHex = 0xFF2563EB,
            children = listOf(
                "cat_learning_tutorials" to "Tutorials",
                "cat_learning_articles" to "Articles",
                "cat_learning_research" to "Research",
                "cat_learning_notes" to "Educational Notes",
            ),
        )

        addRoot(
            id = "cat_work",
            name = "Work",
            iconName = "work",
            colorHex = 0xFF7C3AED,
            children = listOf(
                "cat_work_projects" to "Projects",
                "cat_work_meetings" to "Meetings",
                "cat_work_tasks" to "Tasks",
                "cat_work_docs" to "Documentation",
            ),
        )

        addRoot(
            id = "cat_entertainment",
            name = "Entertainment",
            iconName = "movie",
            colorHex = 0xFFDB2777,
            children = listOf(
                "cat_entertainment_movies" to "Movies",
                "cat_entertainment_music" to "Music",
                "cat_entertainment_games" to "Games",
                "cat_entertainment_social" to "Social Media",
            ),
        )

        addRoot(
            id = "cat_personal",
            name = "Personal",
            iconName = "bookmark",
            colorHex = 0xFF059669,
            children = listOf(
                "cat_personal_ideas" to "Ideas",
                "cat_personal_quotes" to "Quotes",
                "cat_personal_important" to "Important Information",
                "cat_personal_notes" to "Notes",
            ),
        )

        addRoot(
            id = UNCATEGORIZED_ID,
            name = "Uncategorized",
            iconName = "inbox",
            colorHex = 0xFF64748B,
            children = emptyList(),
        )

        return result
    }

    /**
     * Inserts default categories inside the database creation transaction so initial observers
     * receive the populated tree immediately without startup races.
     */
    fun seedInto(db: SupportSQLiteDatabase, seedTimestampMillis: Long = System.currentTimeMillis()) {
        defaultCategories(seedTimestampMillis).forEach { category ->
            val values = ContentValues().apply {
                put("id", category.id)
                put("name", category.name)
                if (category.parentId != null) {
                    put("parent_id", category.parentId)
                } else {
                    putNull("parent_id")
                }
                put("icon_name", category.iconName)
                if (category.colorHex != null) {
                    put("color_hex", category.colorHex)
                } else {
                    putNull("color_hex")
                }
                put("is_system_default", if (category.isSystemDefault) 1 else 0)
                put("is_ai_suggested", if (category.isAiSuggested) 1 else 0)
                put("sort_order", category.sortOrder)
                put("created_at", category.createdAt)
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
        }
    }
}
