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
import com.example.newsapp.navigation.ArticleNavigator
import com.example.newsapp.ui.detail.ArticleDetailFragment

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var resultAdapter: SearchResultAdapter
    private lateinit var recommendationAdapter: SearchResultAdapter
    private lateinit var allArticles: List<NewsArticle>
    private lateinit var recommendedArticles: List<NewsArticle>

    private val articleNavigator: ArticleNavigator?
        get() = activity as? ArticleNavigator

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
        recommendedArticles = allArticles.filter { it.category.equals("Sport", ignoreCase = true) }
            .ifEmpty { allArticles.take(4) }
        setupAdapters()
        setupListeners()
        setupBookmarkListener()
        showRecommendations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapters() = with(binding) {
        recommendationAdapter = SearchResultAdapter { article ->
            articleNavigator?.openArticleDetail(article.id)
        }
        recommendationsRecycler.layoutManager = LinearLayoutManager(requireContext())
        recommendationsRecycler.adapter = recommendationAdapter
        recommendationAdapter.submitList(recommendedArticles)

        resultAdapter = SearchResultAdapter { article ->
            articleNavigator?.openArticleDetail(article.id)
        }
        resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        resultsRecycler.adapter = resultAdapter
    }

    private fun setupListeners() = with(binding) {
        searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString().orEmpty()
            if (query.isBlank()) {
                showRecommendations()
            } else {
                showResultsForQuery(query)
            }
        }

        searchInputLayout.setEndIconOnClickListener {
            searchInput.setText("")
            showRecommendations()
        }
    }

    private fun setupBookmarkListener() {
        parentFragmentManager.setFragmentResultListener(
            ArticleDetailFragment.BOOKMARK_RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            allArticles = NewsRepository.getArticles(requireContext())
            recommendedArticles = allArticles.filter { it.category.equals("Sport", ignoreCase = true) }
                .ifEmpty { allArticles.take(4) }
            if (binding.searchInput.text.isNullOrBlank()) {
                showRecommendations()
            } else {
                showResultsForQuery(binding.searchInput.text.toString())
            }
        }
    }

    private fun showResultsForQuery(query: String) {
        val trimmed = query.trim()
        val filtered = allArticles.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.summary.contains(trimmed, ignoreCase = true) ||
                it.category.contains(trimmed, ignoreCase = true)
        }
        resultAdapter.submitList(filtered)
        binding.resultsRecycler.isVisible = filtered.isNotEmpty()
        binding.searchSectionTitle.isVisible = filtered.isNotEmpty()
        binding.emptyResultText.isVisible = filtered.isEmpty()
        binding.recommendationsRecycler.isVisible = false
        binding.recommendationTitle.isVisible = false
    }

    private fun showRecommendations() = with(binding) {
        searchInput.clearFocus()
        recommendationAdapter.submitList(recommendedArticles)
        recommendationsRecycler.isVisible = recommendedArticles.isNotEmpty()
        recommendationTitle.isVisible = recommendedArticles.isNotEmpty()
        searchSectionTitle.isVisible = false
        resultsRecycler.isVisible = false
        emptyResultText.isVisible = false
    }
}
