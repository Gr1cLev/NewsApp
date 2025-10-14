package com.example.newsapp.ui.news

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.databinding.FragmentNewsBinding
import com.example.newsapp.model.NewsData
import com.example.newsapp.navigation.ArticleNavigator
import com.example.newsapp.ui.detail.ArticleDetailFragment
import kotlin.math.abs

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var newsData: NewsData
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var featuredAdapter: FeaturedArticleAdapter

    private var selectedCategoryName: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newsData = NewsRepository.getNewsData(requireContext())
        selectedCategoryName = savedInstanceState?.getString(KEY_SELECTED_CATEGORY)
            ?: newsData.categories.firstOrNull()?.name
            ?: "All"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupFeaturedPager()
        setupCategoryList()
        setupArticleList()
        parentFragmentManager.setFragmentResultListener(
            ArticleDetailFragment.BOOKMARK_RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            articleAdapter.refreshBookmarks()
        }

        updateArticlesForCategory(selectedCategoryName)
        binding.categoryRecycler.post {
            categoryAdapter.selectCategoryByName(selectedCategoryName)
        }
    }

    override fun onResume() {
        super.onResume()
        articleAdapter.refreshBookmarks()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_CATEGORY, selectedCategoryName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val articleNavigator: ArticleNavigator?
        get() = activity as? ArticleNavigator

    private fun setupAdapters() {
        featuredAdapter = FeaturedArticleAdapter(newsData.featuredArticles) { article ->
            articleNavigator?.openArticleDetail(article.id)
        }
        articleAdapter = ArticleAdapter(
            showCategory = true,
            onArticleClick = { article ->
                articleNavigator?.openArticleDetail(article.id)
            },
            onBookmarkToggle = { _, isBookmarked ->
                val messageRes = if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed
                Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
            }
        )
        categoryAdapter = CategoryAdapter(newsData.categories) { category ->
            selectedCategoryName = category.name
            updateArticlesForCategory(category.name)
        }
    }

    private fun setupFeaturedPager() {
        binding.featuredPager.isVisible = newsData.featuredArticles.isNotEmpty()
        if (newsData.featuredArticles.isEmpty()) return

        with(binding.featuredPager) {
            adapter = featuredAdapter
            offscreenPageLimit = newsData.featuredArticles.size.coerceAtLeast(3)
            setPadding(48.dpToPx(), 0, 48.dpToPx(), 0)
            clipToPadding = false
            clipChildren = false
            (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            val compositeTransformer = CompositePageTransformer().apply {
                addTransformer(MarginPageTransformer(16.dpToPx()))
                addTransformer { page, position ->
                    val scale = 0.9f + (1 - abs(position)) * 0.1f
                    page.scaleY = scale
                }
            }
            setPageTransformer(compositeTransformer)
        }
    }

    private fun setupCategoryList() = with(binding.categoryRecycler) {
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter = categoryAdapter
        itemAnimator = null
    }

    private fun setupArticleList() = with(binding.articleRecycler) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = articleAdapter
    }

    private fun updateArticlesForCategory(categoryName: String) {
        val filtered = if (categoryName.equals("All", ignoreCase = true)) {
            newsData.articles
        } else {
            newsData.articles.filter { it.category.equals(categoryName, ignoreCase = true) }
        }
        articleAdapter.submitList(filtered)
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    companion object {
        private const val KEY_SELECTED_CATEGORY = "key_selected_category"
    }
}
