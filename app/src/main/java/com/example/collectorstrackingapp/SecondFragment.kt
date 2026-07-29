package com.example.collectorstrackingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentSecondBinding

import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel

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

        val collectible = if (currentCollectibleId != -1) {
            Collectible(
                id = currentCollectibleId,
                name = name,
                category = category,
                description = description,
                timestamp = currentCollectible?.timestamp ?: System.currentTimeMillis()
            )
        } else {
            Collectible(name = name, category = category, description = description)
        }
        
        collectibleViewModel.insert(collectible)
        
        Toast.makeText(context, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
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
