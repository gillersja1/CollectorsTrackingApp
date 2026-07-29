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
                timestamp = currentCollectible?.timestamp ?: System.currentTimeMillis()
            )
        } else {
            Collectible(
                name = name, 
                category = category, 
                description = description,
                imagePath = finalImagePath
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
        currentCollectible?.let {
            collectibleViewModel.delete(it)
            Toast.makeText(context, getString(R.string.msg_deleted), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show FAB when leaving the Add screen
        (activity as? MainActivity)?.findViewById<View>(R.id.fab)?.visibility = View.VISIBLE
        _binding = null
    }
}
