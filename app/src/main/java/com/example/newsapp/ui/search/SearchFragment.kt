package com.example.newsapp.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.databinding.FragmentSearchBinding
import com.example.newsapp.model.NewsArticle

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var suggestionAdapter: SuggestionAdapter
    private lateinit var resultAdapter: SearchResultAdapter
    private lateinit var allArticles: List<NewsArticle>
    private lateinit var suggestionItems: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        allArticles = NewsRepository.getArticles(requireContext())
        suggestionItems = NewsRepository.getSearchSuggestions(requireContext())
        setupAdapters()
        setupListeners()
        showSuggestions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapters() = with(binding) {
        suggestionAdapter = SuggestionAdapter { suggestion ->
            searchInput.setText(suggestion)
            searchInput.setSelection(suggestion.length)
            showResultsForQuery(suggestion)
        }
        suggestionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        suggestionsRecycler.adapter = suggestionAdapter
        suggestionAdapter.submitList(suggestionItems)

        resultAdapter = SearchResultAdapter()
        resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        resultsRecycler.adapter = resultAdapter
    }

    private fun setupListeners() = with(binding) {
        searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString().orEmpty()
            if (query.isBlank()) {
                showSuggestions()
            } else {
                showResultsForQuery(query)
            }
        }

        searchInputLayout.setEndIconOnClickListener {
            searchInput.setText("")
            showSuggestions()
        }
    }

    private fun showResultsForQuery(query: String) {
        val trimmed = query.trim()
        val filtered: List<NewsArticle> = allArticles.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.summary.contains(trimmed, ignoreCase = true) ||
                it.category.contains(trimmed, ignoreCase = true)
        }
        resultAdapter.submitList(filtered)
        binding.resultsRecycler.isVisible = filtered.isNotEmpty()
        binding.emptyResultText.isVisible = filtered.isEmpty()
        binding.suggestionsRecycler.isVisible = false
        binding.searchSectionTitle.setText(R.string.title_results)
    }

    private fun showSuggestions() = with(binding) {
        searchSectionTitle.setText(R.string.title_suggestions)
        suggestionsRecycler.isVisible = true
        resultsRecycler.isVisible = false
        emptyResultText.isVisible = false
        suggestionAdapter.submitList(suggestionItems)
    }
}
