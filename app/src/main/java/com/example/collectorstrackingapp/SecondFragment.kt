package com.example.collectorstrackingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.collectorstrackingapp.databinding.FragmentSecondBinding
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val collectibleViewModel: CollectibleViewModel by viewModels()
    private var currentCollectibleId: Int = -1
    private var currentCollectible: Collectible? = null
    
    private var existingCategories: List<Category> = emptyList()
    private var selectedImageUri: String? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            binding.imageViewPreview.load(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide FAB when on the Add/Edit screen
        (activity as? MainActivity)?.findViewById<View>(R.id.fab)?.visibility = View.GONE

        currentCollectibleId = arguments?.getInt("collectibleId") ?: -1

        setupCategoryDropdown()

        if (currentCollectibleId != -1) {
            setupEditMode()
        }

        binding.buttonSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.buttonScanCode.setOnClickListener {
            startBarcodeScanner()
        }

        binding.buttonSave.setOnClickListener {
            saveCollectible()
        }

        binding.buttonDelete.setOnClickListener {
            deleteCollectible()
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.autoCompleteCategory.setAdapter(adapter)

        // Show dropdown when clicked
        binding.autoCompleteCategory.setOnClickListener {
            binding.autoCompleteCategory.showDropDown()
        }

        collectibleViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            existingCategories = categories
            val categoryNames = categories.map { it.name }
            adapter.clear()
            adapter.addAll(categoryNames)
        }
    }

    private fun setupEditMode() {
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.edit_collectible_title)
        binding.buttonDelete.visibility = View.VISIBLE
        
        collectibleViewModel.getCollectible(currentCollectibleId).observe(viewLifecycleOwner) { collectible ->
            collectible?.let {
                currentCollectible = it
                binding.editTextName.setText(it.name)
                binding.autoCompleteCategory.setText(it.category, false)
                binding.editTextDescription.setText(it.description)
                binding.editTextEstimatedValue.setText(if (it.estimatedValue > 0) it.estimatedValue.toString() else "")
                binding.editTextTags.setText(it.tags)
                binding.switchWishlist.isChecked = it.isWishlist
                selectedImageUri = it.imagePath
                if (it.imagePath != null) {
                    binding.imageViewPreview.load(it.imagePath)
                }
            }
        }
    }

    private fun saveCollectible() {
        val name = binding.editTextName.text.toString()
        val category = binding.autoCompleteCategory.text.toString()
        val description = binding.editTextDescription.text.toString()
        val estimatedValue = binding.editTextEstimatedValue.text.toString().toDoubleOrNull() ?: 0.0
        val tags = binding.editTextTags.text.toString().trim()
        val isWishlist = binding.switchWishlist.isChecked

        if (name.isBlank() || category.isBlank()) {
            Toast.makeText(context, getString(R.string.error_name_category), Toast.LENGTH_SHORT).show()
            return
        }

        // Auto-save new category if it doesn't exist
        if (existingCategories.none { it.name.equals(category, ignoreCase = true) }) {
            collectibleViewModel.insertCategory(Category(name = category))
        }

        val finalImagePath = selectedImageUri?.let { uriString ->
            if (uriString.startsWith("content://")) {
                saveImageToInternalStorage(android.net.Uri.parse(uriString))
            } else {
                uriString
            }
        }

        val collectible = if (currentCollectibleId != -1) {
            Collectible(
                id = currentCollectibleId,
                name = name,
                category = category,
                description = description,
                imagePath = finalImagePath,
                timestamp = currentCollectible?.timestamp ?: System.currentTimeMillis(),
                isWishlist = isWishlist,
                estimatedValue = estimatedValue,
                tags = tags
            )
        } else {
            Collectible(
                name = name, 
                category = category, 
                description = description,
                imagePath = finalImagePath,
                isWishlist = isWishlist,
                estimatedValue = estimatedValue,
                tags = tags
            )
        }
        
        collectibleViewModel.insert(collectible)
        
        Toast.makeText(context, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun saveImageToInternalStorage(uri: android.net.Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, "collectible_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteCollectible() {
        if (currentCollectibleId != -1) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Collectible")
                .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    val collectibleToDelete = currentCollectible ?: Collectible(id = currentCollectibleId, name = "", category = "", description = "")
                    collectibleViewModel.delete(collectibleToDelete)
                    Toast.makeText(context, getString(R.string.msg_deleted), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun startBarcodeScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_DATA_MATRIX
            )
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrEmpty()) {
                    handleScannedData(rawValue)
                } else {
                    Toast.makeText(context, "Scanned code was empty", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCanceledListener {
                Toast.makeText(context, getString(R.string.scan_canceled), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                val errorMsg = e.localizedMessage ?: e.message ?: "Unknown error"
                Toast.makeText(context, "Scan failed: $errorMsg. Ensure Google Play Services is up to date.", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleScannedData(rawText: String) {
        // 1. Try JSON parsing (e.g. {"name": "...", "category": "...", "description": "..."})
        try {
            val json = JSONObject(rawText)
            val name = json.optString("name").takeIf { it.isNotBlank() }
                ?: json.optString("title").takeIf { it.isNotBlank() }
            val category = json.optString("category").takeIf { it.isNotBlank() }
            val description = json.optString("description").takeIf { it.isNotBlank() }
                ?: json.optString("desc").takeIf { it.isNotBlank() }

            if (name != null || category != null || description != null) {
                name?.let { binding.editTextName.setText(it) }
                category?.let { binding.autoCompleteCategory.setText(it, false) }
                description?.let { binding.editTextDescription.setText(it) }
                Toast.makeText(context, getString(R.string.scan_autofilled), Toast.LENGTH_SHORT).show()
                return
            }
        } catch (_: Exception) {
            // Not a JSON object
        }

        // 2. Try Key-Value line parsing (e.g. "Name: ... \n Category: ... \n Description: ...")
        val lines = rawText.lines()
        var parsedName: String? = null
        var parsedCategory: String? = null
        var parsedDescription: String? = null

        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "name", "title", "item" -> parsedName = value
                    "category", "cat", "type" -> parsedCategory = value
                    "description", "desc", "notes" -> parsedDescription = value
                }
            }
        }

        if (parsedName != null || parsedCategory != null || parsedDescription != null) {
            parsedName?.let { binding.editTextName.setText(it) }
            parsedCategory?.let { binding.autoCompleteCategory.setText(it, false) }
            parsedDescription?.let { binding.editTextDescription.setText(it) }
            Toast.makeText(context, getString(R.string.scan_autofilled), Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Fallback: Set barcode string as Name if empty, and append to Description
        if (binding.editTextName.text.isNullOrBlank()) {
            binding.editTextName.setText(rawText)
        }

        val currentDesc = binding.editTextDescription.text.toString()
        if (currentDesc.isBlank()) {
            binding.editTextDescription.setText("Scanned Code: $rawText")
        } else if (!currentDesc.contains(rawText)) {
            binding.editTextDescription.setText("$currentDesc\nScanned Code: $rawText")
        }

        Toast.makeText(context, getString(R.string.scan_autofilled), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show FAB when leaving the Add screen
        (activity as? MainActivity)?.findViewById<View>(R.id.fab)?.visibility = View.VISIBLE
        _binding = null
    }
}
