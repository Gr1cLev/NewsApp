package com.example.newsapp.ml

/**
 * Configuration for hybrid recommendation scoring weights
 * 
 * These weights determine how much each component contributes to the final recommendation score.
 * Total should sum to 1.0 for normalized scoring.
 */
data class ML_ScoringWeights(
    val mlModel: Float = 0.50f,        // 50% - Collaborative filtering from trained model
    val ruleBased: Float = 0.30f,      // 30% - Real-time user preference tracking
    val recency: Float = 0.15f,        // 15% - Article freshness (important for news)
    val trending: Float = 0.05f        // 5% - Global popularity
) {
    init {
        val sum = mlModel + ruleBased + recency + trending
        require(sum in 0.99f..1.01f) { 
            "Weights must sum to 1.0 (current sum: $sum)" 
        }
    }
    
    companion object {
        /**
         * Default balanced weights for most users
         */
        val DEFAULT = ML_ScoringWeights()
        
        /**
         * ML-heavy profile for users with lots of interaction data
         */
        val ML_HEAVY = ML_ScoringWeights(
            mlModel = 0.70f,
            ruleBased = 0.20f,
            recency = 0.08f,
            trending = 0.02f
        )
        
        /**
         * Rule-heavy profile for new users (cold start)
         */
        val RULE_HEAVY = ML_ScoringWeights(
            mlModel = 0.20f,
            ruleBased = 0.50f,
            recency = 0.20f,
            trending = 0.10f
        )
        
        /**
         * Recency-focused profile (breaking news priority)
         */
        val RECENCY_FOCUSED = ML_ScoringWeights(
            mlModel = 0.40f,
            ruleBased = 0.30f,
            recency = 0.25f,
            trending = 0.05f
        )
    }
}
