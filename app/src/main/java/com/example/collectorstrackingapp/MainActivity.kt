package com.example.collectorstrackingapp

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.collectorstrackingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The main activity of the Collectors Tracking application.
 * This activity serves as the entry point and sets up the primary user interface components,
 * including the navigation host, action bar, and floating action button.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportUri ->
            try {
                val db = com.example.collectorstrackingapp.data.AppDatabase.getDatabase(this)
                CoroutineScope(Dispatchers.IO).launch {
                    val list = db.collectibleDao().getAllCollectibles().first()
                    val jsonArray = JSONArray()
                    for (item in list) {
                        val obj = JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("category", item.category)
                            put("description", item.description)
                            put("imagePath", item.imagePath ?: "")
                            put("timestamp", item.timestamp)
                        }
                        jsonArray.put(obj)
                    }
                    contentResolver.openOutputStream(exportUri)?.use { outputStream ->
                        outputStream.write(jsonArray.toString(4).toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Collection exported successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importUri ->
            try {
                contentResolver.openInputStream(importUri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    val db = com.example.collectorstrackingapp.data.AppDatabase.getDatabase(this)
                    val syncManager = com.example.collectorstrackingapp.data.FirebaseSyncManager(this)
                    CoroutineScope(Dispatchers.IO).launch {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val item = com.example.collectorstrackingapp.model.Collectible(
                                id = obj.optInt("id", 0),
                                name = obj.getString("name"),
                                category = obj.getString("category"),
                                description = obj.getString("description"),
                                imagePath = obj.optString("imagePath").takeIf { !it.isNullOrEmpty() },
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                            db.collectibleDao().insert(item)
                            syncManager.syncCollectibleInsert(item)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Collection imported successfully!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Initializes the activity, sets up edge-to-edge display, inflates the layout,
     * configures the toolbar, and sets up navigation with the action bar.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState]. **Note: Otherwise it is null.**
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_CollectorsTrackingApp)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            if (navController.currentDestination?.id == R.id.FirstFragment) {
                navController.navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
        }
    }

    /**
     * Inflates the menu; this adds items to the action bar if it is present.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        return when (item.itemId) {
            R.id.action_search -> {
                navController.navigate(R.id.action_FirstFragment_to_SearchFragment)
                true
            }
            R.id.action_settings -> {
                navController.navigate(R.id.action_FirstFragment_to_CategoryManagementFragment)
                true
            }
            R.id.action_export -> {
                exportLauncher.launch("collectible_backup_${System.currentTimeMillis()}.json")
                true
            }
            R.id.action_import -> {
                importLauncher.launch(arrayOf("application/json", "*/*"))
                true
            }
            R.id.action_delete_account -> {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        navController.navigate(R.id.action_FirstFragment_to_LoginFragment)
                    } else {
                        Toast.makeText(this, "Failed to delete account: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                true
            }
            R.id.action_logout -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                navController.navigate(R.id.action_FirstFragment_to_LoginFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method is called whenever the user chooses to navigate Up within your application's activity hierarchy from the action bar.
     *
     * @return true if Up navigation completed successfully and this Activity was finished, false otherwise.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
