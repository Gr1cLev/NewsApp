package com.example.newsapp.ml

import com.example.newsapp.model.NewsArticle

/**
 * Extension utilities for ML layer.
 * Provides a stable article key and a url alias for backward compatibility.
 */
val NewsArticle.url: String
    get() = this.heroImageUrl ?: this.title

val NewsArticle.keyForModel: String
    get() = this.title.ifBlank { this.url }
