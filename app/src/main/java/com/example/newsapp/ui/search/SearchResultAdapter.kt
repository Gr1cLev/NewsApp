package com.example.newsapp.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.databinding.ItemSearchResultBinding
import com.example.newsapp.model.NewsArticle

class SearchResultAdapter(
    private val onArticleClick: (NewsArticle) -> Unit
) : ListAdapter<NewsArticle, SearchResultAdapter.ResultViewHolder>(ResultDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultViewHolder(binding, onArticleClick)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ResultViewHolder(
        private val binding: ItemSearchResultBinding,
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
            resultTitle.text = article.title
            resultSummary.text = article.summary
            resultMeta.text = "${article.source} - ${article.publishedAt}"
        }
    }

    private object ResultDiffCallback : DiffUtil.ItemCallback<NewsArticle>() {
        override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean = oldItem == newItem
    }
}
