package com.example.collectorstrackingapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentSearchBinding
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val collectibleViewModel: CollectibleViewModel by viewModels()
    private lateinit var adapter: CollectibleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryFilter()
        setupSearchInput()
        
        performSearch()
    }

    private fun setupRecyclerView() {
        adapter = CollectibleAdapter { collectible ->
            val bundle = Bundle().apply {
                putInt("collectibleId", collectible.id)
            }
            findNavController().navigate(R.id.SecondFragment, bundle)
        }
        binding.recyclerViewResults.adapter = adapter
    }

    private fun setupCategoryFilter() {
        val filterAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.autoCompleteFilter.setAdapter(filterAdapter)
        binding.autoCompleteFilter.setText("All", false)

        collectibleViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            val categoryNames = mutableListOf("All")
            categoryNames.addAll(categories.map { it.name })
            filterAdapter.clear()
            filterAdapter.addAll(categoryNames)
        }

        binding.autoCompleteFilter.setOnItemClickListener { _, _, _, _ ->
            performSearch()
        }
    }

    private fun setupSearchInput() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch() {
        val query = binding.editTextSearch.text.toString().takeIf { it.isNotBlank() }
        val category = binding.autoCompleteFilter.text.toString()

        collectibleViewModel.searchCollectibles(query, category).observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
