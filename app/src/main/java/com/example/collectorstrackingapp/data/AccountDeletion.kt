package com.example.collectorstrackingapp.data

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** The signed-in Firebase account, behind an interface so [AccountDeletion] can be tested without Firebase. */
interface AccountService {
    /** Confirms the password; Firebase only deletes an account that signed in recently. */
    suspend fun reauthenticate(password: String): Boolean

    /** Deletes the sign-in itself. */
    suspend fun deleteSignIn(): Boolean
}

/**
 * Deletes an account for real: the cloud collection and photos, then the sign-in, then this phone's copy.
 *
 * It stops at the first step that fails and says which, so nobody is told their account is gone while
 * their data is still in the cloud. Only a complete deletion signs out and clears the phone.
 */
class AccountDeletion(
    private val account: AccountService,
    private val cloud: CloudSync,
    private val clearPhone: suspend () -> Unit,
) {
    enum class Result { Deleted, WrongPassword, CloudDataKept, SignInKept }

    suspend fun delete(password: String): Result {
        if (!account.reauthenticate(password)) return Result.WrongPassword
        // Data first: once the sign-in is gone, nothing could reach the data to delete it.
        if (!cloud.deleteEverything()) return Result.CloudDataKept
        if (!account.deleteSignIn()) return Result.SignInKept
        clearPhone()
        return Result.Deleted
    }
}

class FirebaseAccountService : AccountService {

    override suspend fun reauthenticate(password: String): Boolean = attempt("re-authenticate") {
        val user = FirebaseAuth.getInstance().currentUser ?: return@attempt false
        val email = user.email ?: return@attempt false
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        true
    }

    override suspend fun deleteSignIn(): Boolean = attempt("delete the sign-in") {
        val user = FirebaseAuth.getInstance().currentUser ?: return@attempt false
        user.delete().await()
        true
    }

    private suspend fun attempt(what: String, block: suspend () -> Boolean): Boolean =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AccountDeletion", "$what failed", e)
            false
        }
}
