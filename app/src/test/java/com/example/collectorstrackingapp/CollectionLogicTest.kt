package com.example.collectorstrackingapp

import com.example.collectorstrackingapp.CollectionLogic.CATEGORY_ALL
import com.example.collectorstrackingapp.CollectionLogic.SORT_NAME
import com.example.collectorstrackingapp.CollectionLogic.SORT_NEWEST
import com.example.collectorstrackingapp.CollectionLogic.SORT_OLDEST
import com.example.collectorstrackingapp.model.Collectible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionLogicTest {

    private fun item(name: String, category: String = "FunkoPop", timestamp: Long = 0, value: Double = 0.0, wishlist: Boolean = false) =
        Collectible(name = name, category = category, description = "", timestamp = timestamp, estimatedValue = value, isWishlist = wishlist)

    private val batman = item("Batman", "FunkoPop", timestamp = 200, value = 15.0)
    private val charizard = item("charizard", "Pokemon", timestamp = 300, value = 250.5)
    private val alien = item("Alien", "funkopop", timestamp = 100, value = 9.99)
    private val items = listOf(batman, charizard, alien)

    // --- summarize ---

    @Test
    fun summarize_emptyCollection_isAllZeros() {
        val summary = CollectionLogic.summarize(emptyList())

        assertEquals(0, summary.totalItems)
        assertEquals(0.0, summary.portfolioValue, 0.0)
        assertTrue(summary.categoryBreakdown.isEmpty())
    }

    @Test
    fun summarize_sumsEstimatedValues() {
        val summary = CollectionLogic.summarize(items)

        assertEquals(3, summary.totalItems)
        assertEquals(275.49, summary.portfolioValue, 0.001)
    }

    @Test
    fun summarize_breakdownCountsEachCategoryExactly() {
        val summary = CollectionLogic.summarize(items)

        // Category names are case-sensitive in the breakdown, unlike the filter.
        assertEquals(mapOf("FunkoPop" to 1, "Pokemon" to 1, "funkopop" to 1), summary.categoryBreakdown)
    }

    @Test
    fun summarize_excludesWishlistItems() {
        // Regression test: wishlist items (not yet owned) used to count toward
        // the total, the portfolio value and the category breakdown.
        val grail = item("Grail", "Comics", value = 1000.0, wishlist = true)

        val summary = CollectionLogic.summarize(listOf(batman, grail))

        assertEquals(1, summary.totalItems)
        assertEquals(15.0, summary.portfolioValue, 0.0)
        assertEquals(mapOf("FunkoPop" to 1), summary.categoryBreakdown)
    }

    @Test
    fun filter_keepsWishlistItemsInTheList() {
        // Wishlist items are left out of the stats but still shown in the list.
        val grail = item("Grail", wishlist = true)

        assertEquals(listOf(grail, batman), CollectionLogic.filterAndSort(listOf(batman, grail), CATEGORY_ALL, SORT_OLDEST))
    }

    // --- filterAndSort ---

    @Test
    fun filter_all_keepsEveryItem() {
        assertEquals(3, CollectionLogic.filterAndSort(items, CATEGORY_ALL, SORT_NEWEST).size)
    }

    @Test
    fun filter_category_isCaseInsensitive() {
        val result = CollectionLogic.filterAndSort(items, "FUNKOPOP", SORT_NEWEST)

        assertEquals(listOf(batman, alien), result)
    }

    @Test
    fun filter_unknownCategory_returnsEmpty() {
        assertTrue(CollectionLogic.filterAndSort(items, "Stamps", SORT_NEWEST).isEmpty())
    }

    @Test
    fun sort_newest_ordersByTimestampDescending() {
        assertEquals(listOf(charizard, batman, alien), CollectionLogic.filterAndSort(items, CATEGORY_ALL, SORT_NEWEST))
    }

    @Test
    fun sort_oldest_ordersByTimestampAscending() {
        assertEquals(listOf(alien, batman, charizard), CollectionLogic.filterAndSort(items, CATEGORY_ALL, SORT_OLDEST))
    }

    @Test
    fun sort_name_isAlphabeticalIgnoringCase() {
        assertEquals(listOf(alien, batman, charizard), CollectionLogic.filterAndSort(items, CATEGORY_ALL, SORT_NAME))
    }

    @Test
    fun sort_unknownOrder_fallsBackToNewest() {
        assertEquals(listOf(charizard, batman, alien), CollectionLogic.filterAndSort(items, CATEGORY_ALL, "Price"))
    }
}
