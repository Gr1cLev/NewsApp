package com.example.newsapp.ui.news

import android.graphics.drawable.GradientDrawable
import androidx.core.view.isVisible
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.newsapp.databinding.ItemFeaturedArticleBinding
import com.example.newsapp.model.NewsArticle
import java.util.Locale

class FeaturedArticleAdapter(
    private val items: List<NewsArticle>,
    private val onArticleClick: (NewsArticle) -> Unit
) : RecyclerView.Adapter<FeaturedArticleAdapter.FeaturedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedViewHolder(binding, onArticleClick)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FeaturedViewHolder(
        private val binding: ItemFeaturedArticleBinding,
        private val onArticleClick: (NewsArticle) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundArticle: NewsArticle? = null

        init {
            binding.root.setOnClickListener {
                boundArticle?.let(onArticleClick)
            }
        }

        fun bind(article: NewsArticle) = with(binding) {
            boundArticle = article
            featuredTag.text = article.tag.uppercase(Locale.getDefault())
            featuredTitle.text = article.title
            featuredSummary.text = article.summary
            val backgroundDrawable = GradientDrawable(
                GradientDrawable.Orientation.TR_BL,
                intArrayOf(article.accentColor, lighten(article.accentColor))
            ).apply {
                cornerRadius = featuredBackground.radius(28f)
            }
            val hasHeroImage = !article.heroImageUrl.isNullOrBlank()
            if (hasHeroImage) {
                featuredImage.isVisible = true
                featuredImage.load(article.heroImageUrl) {
                    crossfade(true)
                    placeholder(com.example.newsapp.R.drawable.bg_detail_placeholder)
                    error(com.example.newsapp.R.drawable.bg_detail_placeholder)
                }
                backgroundDrawable.alpha = (255 * 0.82f).toInt()
            } else {
                featuredImage.setImageDrawable(null)
                featuredImage.isVisible = false
                backgroundDrawable.alpha = 255
            }
            featuredBackground.background = backgroundDrawable
        }

        private fun lighten(@ColorInt baseColor: Int): Int {
            return ColorUtils.blendARGB(baseColor, 0xFFFFFFFF.toInt(), 0.35f)
        }

        private fun View.radius(radiusDp: Float): Float {
            return radiusDp * resources.displayMetrics.density
        }
    }
}
