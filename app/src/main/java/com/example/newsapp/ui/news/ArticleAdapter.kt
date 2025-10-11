package com.example.newsapp.ui.news

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.databinding.ItemNewsArticleBinding
import com.example.newsapp.model.NewsArticle
import java.util.Locale

class ArticleAdapter(
    private val showCategory: Boolean = true
) : ListAdapter<NewsArticle, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemNewsArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArticleViewHolder(binding, showCategory)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ArticleViewHolder(
        private val binding: ItemNewsArticleBinding,
        private val showCategory: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: NewsArticle) = with(binding) {
            articleCategory.text = article.category.lowercase(Locale.getDefault())
            articleCategory.isVisible = showCategory

            articleTitle.text = article.title
            articleSummary.text = article.summary
            articleSource.text = article.source
            articleTimestamp.text = article.publishedAt

            articleAccent.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(article.accentColor, lightenColor(article.accentColor))
            ).apply {
                cornerRadius = articleAccent.radius(18f)
            }
        }

        private fun lightenColor(@ColorInt baseColor: Int): Int {
            return ColorUtils.blendARGB(baseColor, 0xFFFFFFFF.toInt(), 0.45f)
        }

        private fun View.radius(radiusDp: Float): Float {
            return radiusDp * resources.displayMetrics.density
        }
    }

    private object ArticleDiffCallback : DiffUtil.ItemCallback<NewsArticle>() {
        override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean = oldItem == newItem
    }
}
