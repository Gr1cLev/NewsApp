package com.example.newsapp.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.newsapp.databinding.FragmentNewsBinding
import com.example.newsapp.model.sampleArticles
import com.example.newsapp.model.sampleCategories
import com.example.newsapp.model.sampleFeaturedArticles
import kotlin.math.abs

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var featuredAdapter: FeaturedArticleAdapter

    private var selectedCategoryName: String = sampleCategories.firstOrNull()?.name ?: "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCategoryName = savedInstanceState?.getString(KEY_SELECTED_CATEGORY) ?: selectedCategoryName
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

        updateArticlesForCategory(selectedCategoryName)
        binding.categoryRecycler.post {
            categoryAdapter.selectCategoryByName(selectedCategoryName)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_CATEGORY, selectedCategoryName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapters() {
        featuredAdapter = FeaturedArticleAdapter(sampleFeaturedArticles)
        articleAdapter = ArticleAdapter(showCategory = true)
        categoryAdapter = CategoryAdapter(sampleCategories) { category ->
            selectedCategoryName = category.name
            updateArticlesForCategory(category.name)
        }
    }

    private fun setupFeaturedPager() = with(binding.featuredPager) {
        adapter = featuredAdapter
        offscreenPageLimit = sampleFeaturedArticles.size.coerceAtLeast(3)
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
            sampleArticles
        } else {
            sampleArticles.filter { it.category.equals(categoryName, ignoreCase = true) }
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
