package com.example.newsapp.ui.news

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.databinding.ItemFeaturedArticleBinding
import com.example.newsapp.model.NewsArticle
import java.util.Locale

class FeaturedArticleAdapter(
    private val items: List<NewsArticle>
) : RecyclerView.Adapter<FeaturedArticleAdapter.FeaturedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FeaturedViewHolder(
        private val binding: ItemFeaturedArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: NewsArticle) = with(binding) {
            featuredTag.text = article.tag.uppercase(Locale.getDefault())
            featuredTitle.text = article.title
            featuredSummary.text = article.summary

            featuredBackground.background = GradientDrawable(
                GradientDrawable.Orientation.TR_BL,
                intArrayOf(article.accentColor, lighten(article.accentColor))
            ).apply {
                cornerRadius = featuredBackground.radius(28f)
            }
        }

        private fun lighten(@ColorInt baseColor: Int): Int {
            return ColorUtils.blendARGB(baseColor, 0xFFFFFFFF.toInt(), 0.35f)
        }

        private fun View.radius(radiusDp: Float): Float {
            return radiusDp * resources.displayMetrics.density
        }
    }
}
