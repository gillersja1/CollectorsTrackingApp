package com.example.collectorstrackingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentFirstBinding
import androidx.fragment.app.viewModels
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel
import java.util.Locale

/**
 * Fragment as the default destination in navigation displaying collection overview, stats, filters, sorting, and collectibles list.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val collectibleViewModel: CollectibleViewModel by viewModels()

    private var allItems: List<Collectible> = emptyList()
    private var allCategoriesList: List<String> = emptyList()
    private var selectedCategory: String = "All"
    private var selectedSortOrder: String = "Newest"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CollectibleAdapter { collectible ->
            val bundle = Bundle().apply {
                putInt("collectibleId", collectible.id)
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
        binding.recyclerView.adapter = adapter

        // Setup Sort Spinner
        val sortOptions = arrayOf("Newest", "Oldest", "Name (A-Z)")
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortOrder.adapter = sortAdapter
        binding.spinnerSortOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSortOrder = sortOptions[position]
                filterAndSortList(adapter)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Observe Categories for Filter Spinner
        collectibleViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            val catNames = mutableListOf("All")
            categories?.forEach { catNames.add(it.name) }
            allCategoriesList = catNames

            val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, catNames)
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategoryFilter.adapter = catAdapter
            binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedCategory = catNames[position]
                    filterAndSortList(adapter)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        // Observe Collectibles for Stats and List
        collectibleViewModel.allCollectibles.observe(viewLifecycleOwner) { collectibles ->
            collectibles?.let {
                allItems = it
                updateStats(it)
                filterAndSortList(adapter)
            }
        }
    }

    private fun updateStats(collectibles: List<Collectible>) {
        val total = collectibles.size
        val portfolioValue = collectibles.sumOf { it.estimatedValue }
        binding.textViewTotalItems.text = String.format(Locale.getDefault(), "Total Items: %d | Portfolio: $%.2f", total, portfolioValue)

        val breakdown = collectibles.groupingBy { it.category }.eachCount()
        val breakdownText = if (breakdown.isEmpty()) {
            "Categories: None"
        } else {
            breakdown.entries.joinToString(prefix = "Breakdown: ", separator = ", ") { "${it.key} (${it.value})" }
        }
        binding.textViewCategoryBreakdown.text = breakdownText
    }

    private fun filterAndSortList(adapter: CollectibleAdapter) {
        var filtered = if (selectedCategory == "All") {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        filtered = when (selectedSortOrder) {
            "Oldest" -> filtered.sortedBy { it.timestamp }
            "Name (A-Z)" -> filtered.sortedBy { it.name.lowercase() }
            else -> filtered.sortedByDescending { it.timestamp } // Newest
        }

        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
