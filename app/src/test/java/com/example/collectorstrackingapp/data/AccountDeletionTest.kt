package com.example.collectorstrackingapp.data

import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deleting an account deletes everything, in an order where a failure never leaves the user misled. */
class AccountDeletionTest {

    private val steps = mutableListOf<String>()

    private inner class FakeAccount(var passwordWorks: Boolean = true, var deleteWorks: Boolean = true) : AccountService {
        override suspend fun reauthenticate(password: String): Boolean {
            steps += "reauthenticate"
            return passwordWorks && password == "right"
        }

        override suspend fun deleteSignIn(): Boolean {
            steps += "delete sign-in"
            return deleteWorks
        }
    }

    private inner class FakeCloud(var works: Boolean = true) : CloudSync {
        var hasData = true
        override val currentUserId = "ada"
        override suspend fun fetchCollectibles(): List<Collectible>? = null
        override suspend fun fetchCategories(): List<Category>? = null
        override suspend fun upsertCollectible(collectible: Collectible) = false
        override suspend fun deleteCollectible(cloudId: String) = false
        override suspend fun upsertCategory(category: Category) = false
        override suspend fun deleteCategory(category: Category) = false
        override suspend fun uploadImage(localPath: String): String? = null
        override suspend fun deletePhoto(path: String) = false
        override suspend fun deleteEverything(): Boolean {
            steps += "delete cloud data"
            if (works) hasData = false
            return works
        }
    }

    private var phoneCleared = false

    private fun deletion(account: FakeAccount = FakeAccount(), cloud: FakeCloud = FakeCloud()) =
        AccountDeletion(account, cloud) {
            steps += "clear phone"
            phoneCleared = true
        }

    @Test
    fun deletesTheCloudDataThenTheSignInThenThePhone() = runBlocking {
        val cloud = FakeCloud()

        val result = deletion(cloud = cloud).delete("right")

        assertEquals(AccountDeletion.Result.Deleted, result)
        assertEquals(listOf("reauthenticate", "delete cloud data", "delete sign-in", "clear phone"), steps)
        assertFalse("the cloud collection is gone", cloud.hasData)
        assertTrue(phoneCleared)
    }

    @Test
    fun aWrongPasswordDeletesNothing() = runBlocking {
        val cloud = FakeCloud()

        val result = deletion(cloud = cloud).delete("wrong")

        assertEquals(AccountDeletion.Result.WrongPassword, result)
        assertEquals(listOf("reauthenticate"), steps)
        assertTrue(cloud.hasData)
        assertFalse(phoneCleared)
    }

    @Test
    fun ifTheCloudDataCantBeDeletedTheAccountStays() = runBlocking {
        val result = deletion(cloud = FakeCloud(works = false)).delete("right")

        assertEquals(AccountDeletion.Result.CloudDataKept, result)
        assertFalse("the sign-in must stay, or nothing could reach the data to delete it", "delete sign-in" in steps)
        assertFalse(phoneCleared)
    }

    @Test
    fun ifTheSignInCantBeDeletedThePhoneIsNotWipedAndTheUserIsTold() = runBlocking {
        val result = deletion(account = FakeAccount(deleteWorks = false)).delete("right")

        assertEquals(AccountDeletion.Result.SignInKept, result)
        assertFalse(phoneCleared)
    }
}
