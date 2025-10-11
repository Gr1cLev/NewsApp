package com.example.newsapp.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.databinding.FragmentBookmarksBinding
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.ui.news.ArticleAdapter

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private lateinit var articleAdapter: ArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        renderBookmarks(NewsRepository.getBookmarks(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecycler() = with(binding.bookmarksRecycler) {
        layoutManager = LinearLayoutManager(requireContext())
        articleAdapter = ArticleAdapter(showCategory = true)
        adapter = articleAdapter
    }

    private fun renderBookmarks(bookmarks: List<NewsArticle>) {
        val hasBookmarks = bookmarks.isNotEmpty()
        binding.bookmarksRecycler.isVisible = hasBookmarks
        binding.emptyStateContainer.isVisible = !hasBookmarks
        articleAdapter.submitList(bookmarks)
    }
}
