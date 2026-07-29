package com.example.collectorstrackingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.collectorstrackingapp.databinding.FragmentCategoryManagementBinding
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel

import androidx.navigation.fragment.findNavController

class CategoryManagementFragment : Fragment() {

    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!

    private val collectibleViewModel: CollectibleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CategoryAdapter(
            onItemClicked = { category ->
                val bundle = Bundle().apply {
                    putString("categoryName", category.name)
                }
                findNavController().navigate(
                    R.id.action_CategoryManagementFragment_to_CategoryDetailsFragment,
                    bundle
                )
            },
            onDeleteClicked = { category ->
                collectibleViewModel.deleteCategory(category)
                Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewCategories.adapter = adapter

        collectibleViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            categories?.let { adapter.submitList(it) }
        }

        binding.buttonAddCategory.setOnClickListener {
            val name = binding.editTextNewCategory.text.toString()
            if (name.isNotBlank()) {
                collectibleViewModel.insertCategory(Category(name = name))
                binding.editTextNewCategory.text?.clear()
                Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
