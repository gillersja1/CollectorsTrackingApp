package com.example.collectorstrackingapp

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * The app end to end on a device, against the Firebase emulators: register, add an item, see it in the
 * list and in the cloud, delete it, log out. Runs in CI on an Android emulator (see android.yml).
 */
@RunWith(AndroidJUnit4::class)
class CollectionJourneyTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private val email = "ui-${System.currentTimeMillis()}@example.com"
    private val password = "correct-horse-42"
    private val itemName = "UI Test Batman ${System.currentTimeMillis() % 10_000}"

    @Before
    fun startSignedOut() {
        FirebaseAuth.getInstance().signOut()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario.close()
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun registerAddAnItemSeeItInTheCloudDeleteItAndLogOut() {
        // Register.
        waitFor(withId(R.id.registerButton))
        onView(withId(R.id.usernameEditText)).perform(replaceText(email))
        onView(withId(R.id.passwordEditText)).perform(replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.registerButton)).perform(click())

        // Home, then add an item.
        waitFor(homeScreen)
        onView(withId(R.id.fab)).perform(click())
        waitFor(withId(R.id.editTextName))
        onView(withId(R.id.editTextName)).perform(replaceText(itemName))
        onView(withId(R.id.autoCompleteCategory)).perform(replaceText("FunkoPop"), closeSoftKeyboard())
        onView(withId(R.id.button_save)).perform(click())

        // It's in the list, and the sync engine sent it to the cloud under a random cloud id.
        waitFor(withText(itemName))
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val cloud = eventually { cloudItems(uid).takeIf { it.isNotEmpty() } }
        assertEquals(listOf(itemName), cloud.map { it["name"] })
        assertTrue("cloud id is a UUID", (cloud.single()["cloudId"] as String).matches(Regex("[0-9a-f-]{36}")))

        // Delete it: gone from the list and from the cloud.
        onView(withText(itemName)).perform(click())
        waitFor(withId(R.id.button_delete))
        onView(withId(R.id.button_delete)).perform(click())
        onView(withText("Delete")).perform(click())
        waitFor(homeScreen)
        eventually { if (cloudItems(uid).isEmpty()) true else null }
        onView(withText(itemName)).check(doesNotExist())

        // Log out: back to the login screen.
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText("Logout")).perform(click())
        onView(withText("Logout")).perform(click())
        waitFor(withId(R.id.loginButton))
    }

    @Test
    fun aWrongPasswordKeepsYouOnTheLoginScreen() {
        waitFor(withId(R.id.loginButton))
        onView(withId(R.id.usernameEditText)).perform(replaceText("nobody-${System.currentTimeMillis()}@example.com"))
        onView(withId(R.id.passwordEditText)).perform(replaceText("wrong-password"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        Thread.sleep(3_000) // long enough for a sign-in round trip to the emulator to come back
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
        assertEquals(null, FirebaseAuth.getInstance().currentUser)
    }

    /** The home screen's stats heading: always shown, unlike the list, which has no height while empty. */
    private val homeScreen = withId(R.id.textViewStatsTitle)

    private fun cloudItems(uid: String): List<Map<String, Any?>> =
        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid).collection("collectibles").get(), 10, TimeUnit.SECONDS)
            .documents.map { it.data.orEmpty() }

    /** Firebase and navigation are asynchronous: wait (up to 20 s) until the view is on screen. */
    private fun waitFor(matcher: Matcher<View>, timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                if (e !is NoMatchingViewException && e !is AssertionError) throw e
                if (System.currentTimeMillis() > deadline) throw e
                Thread.sleep(250)
            }
        }
    }

    /** Retries [block] until it returns something, for up to 20 s. */
    private fun <T : Any> eventually(timeoutMs: Long = 20_000, block: () -> T?): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            block()?.let { return it }
            if (System.currentTimeMillis() > deadline) error("Gave up waiting after ${timeoutMs / 1000} s")
            Thread.sleep(500)
        }
    }
}
