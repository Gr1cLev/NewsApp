package com.example.newsapp.ui.detail

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.databinding.FragmentArticleDetailBinding
import com.example.newsapp.model.NewsArticle
import java.util.Locale

class ArticleDetailFragment : Fragment() {

    private var _binding: FragmentArticleDetailBinding? = null
    private val binding get() = _binding!!

    private var articleId: Int = -1
    private var article: NewsArticle? = null
    private var isBookmarked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        articleId = savedInstanceState?.getInt(ARG_ARTICLE_ID)
            ?: arguments?.getInt(ARG_ARTICLE_ID)
            ?: -1

        if (articleId != -1) {
            article = NewsRepository.getArticleById(requireContext(), articleId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        renderArticle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() = with(binding.detailToolbar) {
        setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun renderArticle() {
        val article = article
        if (article == null) {
            Toast.makeText(requireContext(), R.string.toast_article_not_found, Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        binding.categoryChip.text = article.tag.uppercase(Locale.getDefault())
        binding.titleText.text = article.title
        binding.metaText.text = getString(R.string.detail_meta_format, article.publishedAt, article.source)
        binding.articleSummary.text = article.summary
        binding.articleContent.text = article.content.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n\n")
            ?: article.summary

        applyHeroBackground(article.accentColor)
        isBookmarked = NewsRepository.isArticleBookmarked(requireContext(), article.id)
        updateBookmarkButton()

        binding.bookmarkButton.setOnClickListener {
            val context = requireContext()
            isBookmarked = NewsRepository.toggleBookmark(context, article.id)
            updateBookmarkButton()
            Toast.makeText(
                context,
                if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun applyHeroBackground(accentColor: Int) {
        val heroBackground = GradientDrawable(
            GradientDrawable.Orientation.BL_TR,
            intArrayOf(accentColor, ColorUtils.blendARGB(accentColor, 0xFF0F172A.toInt(), 0.3f))
        )
        binding.heroContainer.background = heroBackground

        val chipBackground = GradientDrawable().apply {
            cornerRadius = resources.displayMetrics.density * 18f
            setColor(ColorUtils.blendARGB(accentColor, 0xFFFFFFFF.toInt(), 0.65f))
        }
        binding.categoryChip.background = chipBackground
        binding.categoryChip.setTextColor(ColorUtils.blendARGB(accentColor, 0xFF0F172A.toInt(), 0.2f))
        binding.categoryChip.setPadding(16.dp(), 6.dp(), 16.dp(), 6.dp())
    }

    private fun updateBookmarkButton() {
        val iconRes = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        binding.bookmarkButton.setImageResource(iconRes)
        binding.bookmarkButton.contentDescription = getString(
            if (isBookmarked) R.string.bookmark_added else R.string.bookmark_removed
        )
    }

    companion object {
        private const val ARG_ARTICLE_ID = "arg_article_id"

        fun newInstance(articleId: Int): ArticleDetailFragment {
            return ArticleDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ARTICLE_ID, articleId)
                }
            }
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
