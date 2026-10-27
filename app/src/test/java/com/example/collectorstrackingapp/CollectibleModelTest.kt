package com.example.collectorstrackingapp

import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectibleModelTest {

    @Test
    fun testCollectibleCreation() {
        val collectible = Collectible(
            id = 1,
            name = "Test Item",
            category = "FunkoPop",
            description = "A rare collectible",
            imagePath = "content://image.jpg"
        )
        assertEquals(1, collectible.id)
        assertEquals("Test Item", collectible.name)
        assertEquals("FunkoPop", collectible.category)
        assertEquals("A rare collectible", collectible.description)
        assertEquals("content://image.jpg", collectible.imagePath)
    }

    @Test
    fun testCategoryCreation() {
        val category = Category(id = 1, name = "Action Figures")
        assertEquals(1, category.id)
        assertEquals("Action Figures", category.name)
    }

    @Test
    fun testUserCreation() {
        val user = User(id = 1, username = "user@example.com", passwordHash = "password123")
        assertEquals(1, user.id)
        assertEquals("user@example.com", user.username)
        assertEquals("password123", user.passwordHash)
    }
}
