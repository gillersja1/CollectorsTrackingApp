package com.example.collectorstrackingapp

import com.example.collectorstrackingapp.model.Collectible

/**
 * Summary figures shown at the top of the collection overview.
 */
data class CollectionSummary(
    val totalItems: Int,
    val portfolioValue: Double,
    val categoryBreakdown: Map<String, Int>
)

/**
 * Pure collection logic, kept out of FirstFragment so it can be unit tested
 * without a device or emulator.
 */
object CollectionLogic {

    const val CATEGORY_ALL = "All"
    const val SORT_NEWEST = "Newest"
    const val SORT_OLDEST = "Oldest"
    const val SORT_NAME = "Name (A-Z)"

    /**
     * Summarizes the items the user owns. Wishlist items aren't owned yet, so
     * they're excluded from the total, the portfolio value and the breakdown.
     */
    fun summarize(collectibles: List<Collectible>): CollectionSummary {
        val owned = collectibles.filter { !it.isWishlist }
        return CollectionSummary(
            totalItems = owned.size,
            portfolioValue = owned.sumOf { it.estimatedValue },
            categoryBreakdown = owned.groupingBy { it.category }.eachCount()
        )
    }

    /**
     * Filters by category (case-insensitive; "All" keeps everything) and sorts
     * by the given order. Unknown sort orders fall back to newest first.
     */
    fun filterAndSort(collectibles: List<Collectible>, category: String, sortOrder: String): List<Collectible> {
        val filtered = if (category == CATEGORY_ALL) {
            collectibles
        } else {
            collectibles.filter { it.category.equals(category, ignoreCase = true) }
        }

        return when (sortOrder) {
            SORT_OLDEST -> filtered.sortedBy { it.timestamp }
            SORT_NAME -> filtered.sortedBy { it.name.lowercase() }
            else -> filtered.sortedByDescending { it.timestamp }
        }
    }
}
