package com.example.newsapp.ml

import com.example.newsapp.model.NewsArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main inference engine for ML-powered recommendations
 * 
 * This is the central component that combines:
 * 1. ML model scores (collaborative filtering via ALS/SVD)
 * 2. Rule-based scores (category preference + recency)
 * 3. Weighted hybrid scoring
 * 
 * Architecture:
 * - Stub implementation for now (returns rule-based + random ML scores)
 * - Will be updated to use real ML inference once model is trained
 * - Non-blocking design (returns immediately with best-effort scores)
 * 
 * Scoring Formula:
 * finalScore = (mlScore * mlWeight) + (ruleScore * ruleWeight) + 
 *              (recencyBoost * recencyWeight) + (trendingBoost * trendingWeight)
 */
@Singleton
class ML_RecommendationEngine @Inject constructor(
    private val ruleBasedEngine: ML_RuleBasedEngine,
    private val modelDownloader: ML_ModelDownloader,
    private val userPreferenceTracker: ML_UserPreferenceTracker
) {
    
    private var currentModel: ML_ModelArtifacts? = null
    private var isModelLoaded = false
    
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotLoaded)
    val modelStatus: Flow<ModelStatus> = _modelStatus.asStateFlow()
    
    /**
     * Initialize the recommendation engine
     * Loads cached model or falls back to rule-based only
     */
    suspend fun initialize() {
        android.util.Log.d(TAG, "Initializing recommendation engine...")
        
        // Try to load cached model first
        val cachedModel = modelDownloader.loadFromCache()
        if (cachedModel != null) {
            currentModel = cachedModel
            isModelLoaded = true
            _modelStatus.value = ModelStatus.Loaded(cachedModel.version)
            android.util.Log.d(TAG, "✅ ML model loaded: version=${cachedModel.version}")
        } else {
            _modelStatus.value = ModelStatus.NotLoaded
            android.util.Log.d(TAG, "⚠️ No ML model available, using rule-based only")
        }
    }
    
    /**
     * Download and load new model from Firebase Storage
     */
    suspend fun updateModel(forceDownload: Boolean = false) {
        _modelStatus.value = ModelStatus.Downloading
        
        val result = modelDownloader.downloadModel(forceDownload)
        
        result.fold(
            onSuccess = { model ->
                currentModel = model
                isModelLoaded = true
                _modelStatus.value = ModelStatus.Loaded(model.version)
                android.util.Log.d(TAG, "✅ Model updated: version=${model.version}")
            },
            onFailure = { error ->
                _modelStatus.value = ModelStatus.Error(error.message ?: "Unknown error")
                android.util.Log.e(TAG, "❌ Model update failed", error)
            }
        )
    }
    
    /**
     * Get personalized recommendations for user
     * 
     * @param userId User ID for personalization
     * @param candidates List of candidate articles to score
     * @param topN Number of top recommendations to return
     * @param weights Custom scoring weights (optional, uses DEFAULT if null)
     * @return Ranked list of recommendations
     */
    suspend fun getRecommendations(
        userId: String,
        candidates: List<NewsArticle>,
        topN: Int = 10,
        weights: ML_ScoringWeights = ML_ScoringWeights.DEFAULT
    ): List<ScoredArticle> {
        
        if (candidates.isEmpty()) {
            return emptyList()
        }
        
        android.util.Log.d(TAG, "Getting recommendations for user=$userId, candidates=${candidates.size}, topN=$topN")
        
        // ALWAYS use rule-based engine for instant category-based recommendations
        // ML model will be used later for fine-tuning after cloud training
        android.util.Log.d(TAG, "Using rule-based engine for category-based recommendations")
        val ruleRecommendations = ruleBasedEngine.getRecommendations(candidates, topN)
        return ruleRecommendations.map { (article, score) ->
            ScoredArticle(
                article = article,
                finalScore = score,
                mlScore = 0.5f,
                ruleScore = score,
                recencyBoost = 0.5f,
                trendingBoost = 0.0f
            )
        }
    }
    
    /**
     * Calculate hybrid score combining ML + Rule-based
     */
    private fun calculateHybridScore(
        userId: String,
        article: NewsArticle,
        weights: ML_ScoringWeights
    ): HybridScore {
        
        // 1. Get ML score (collaborative filtering)
        val mlScore = if (isModelLoaded) {
            calculateMLScore(userId, article)
        } else {
            0.5f // Neutral score when model not available
        }
        
        // 2. Get rule-based score (content-based)
        val ruleScore = ruleBasedEngine.calculateScore(article)
        
        // 3. Calculate recency boost
        val recencyBoost = calculateRecencyBoost(article)
        
        // 4. Calculate trending boost (stub for now)
        val trendingBoost = 0.0f
        
        // 5. Combine with weights
        val finalScore = (mlScore * weights.mlModel) +
                        (ruleScore * weights.ruleBased) +
                        (recencyBoost * weights.recency) +
                        (trendingBoost * weights.trending)
        
        return HybridScore(
            finalScore = finalScore,
            mlScore = mlScore,
            ruleScore = ruleScore,
            recencyBoost = recencyBoost,
            trendingBoost = trendingBoost
        )
    }
    
    /**
     * Calculate ML score using collaborative filtering
     * 
     * Formula: score = userEmbedding · articleEmbedding + userBias + articleBias + globalMean
     * 
     * For now, returns random score (0.3-0.7) as placeholder
     * TODO: Replace with real dot product when model is trained
     */
    private fun calculateMLScore(userId: String, article: NewsArticle): Float {
        val model = currentModel ?: return 0.5f
        
        // Get embeddings (using title as unique identifier since no url field)
        val userEmbedding = model.getUserEmbedding(userId)
        val articleEmbedding = model.getArticleEmbedding(article.title)
        
        // If embeddings not found (cold start), use global mean
        if (userEmbedding == null || articleEmbedding == null) {
            return model.globalMean
        }
        
        // Dot product of embeddings
        var dotProduct = 0.0f
        for (i in userEmbedding.indices) {
            dotProduct += userEmbedding[i] * articleEmbedding[i]
        }
        
        // Add biases (using title as unique identifier)
        val userBias = model.getUserBias(userId)
        val articleBias = model.getArticleBias(article.title)
        
        // Final prediction
        val prediction = dotProduct + userBias + articleBias + model.globalMean
        
        // Normalize to 0-1 range (assuming rating scale 1-5)
        return ((prediction - 1.0f) / 4.0f).coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate recency boost based on article publish date
     * Newer articles get higher boost
     * Note: publishedAt is a String, for now return neutral boost
     * TODO: Parse ISO 8601 or custom date format properly
     */
    private fun calculateRecencyBoost(article: NewsArticle): Float {
        // For now, return neutral boost since publishedAt is String
        // In production, parse timestamp from article.publishedAt
        return 0.5f
    }
    
    /**
     * Check if model is loaded and ready
     */
    fun isModelReady(): Boolean = isModelLoaded
    
    /**
     * Get current model version
     */
    fun getModelVersion(): String? = currentModel?.version
    
    /**
     * Hybrid score breakdown
     */
    data class HybridScore(
        val finalScore: Float,
        val mlScore: Float,
        val ruleScore: Float,
        val recencyBoost: Float,
        val trendingBoost: Float
    )
    
    /**
     * Scored article with recommendation score
     */
    data class ScoredArticle(
        val article: NewsArticle,
        val finalScore: Float,
        val mlScore: Float,
        val ruleScore: Float,
        val recencyBoost: Float,
        val trendingBoost: Float
    )
    
    /**
     * Model loading status
     */
    sealed class ModelStatus {
        object NotLoaded : ModelStatus()
        object Downloading : ModelStatus()
        data class Loaded(val version: String) : ModelStatus()
        data class Error(val message: String) : ModelStatus()
    }
    
    companion object {
        private const val TAG = "ML_RecommendationEngine"
    }
}
