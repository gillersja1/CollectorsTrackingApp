package com.example.collectorstrackingapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentCategoryDetailsBinding
import com.example.collectorstrackingapp.viewmodel.CollectibleViewModel

class CategoryDetailsFragment : Fragment() {

    private var _binding: FragmentCategoryDetailsBinding? = null
    private val binding get() = _binding!!

    private val collectibleViewModel: CollectibleViewModel by viewModels()
    private lateinit var adapter: CollectibleAdapter
    private var categoryName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryName = arguments?.getString("categoryName") ?: ""
        (activity as? AppCompatActivity)?.supportActionBar?.title = categoryName

        setupRecyclerView()
        setupSearchInput()
        
        binding.buttonBackHome.setOnClickListener {
            findNavController().navigate(R.id.FirstFragment)
        }

        performSearch()
    }

    private fun setupRecyclerView() {
        adapter = CollectibleAdapter { collectible ->
            val bundle = Bundle().apply {
                putInt("collectibleId", collectible.id)
            }
            findNavController().navigate(R.id.SecondFragment, bundle)
        }
        binding.recyclerViewCategoryItems.adapter = adapter
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
        collectibleViewModel.searchCollectibles(query, categoryName).observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
