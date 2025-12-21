package com.example.newsapp.ml

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and manages ML model artifacts from Firestore (FREE!)
 * 
 * Responsibilities:
 * - Fetch model from Firestore collection
 * - Cache model locally
 * - Check for updates
 * - Manage model versions
 * 
 * Advantages over Firebase Storage:
 * - ✅ FREE on Spark Plan (1 GB + 50K reads/day)
 * - ✅ No file download needed (direct query)
 * - ✅ Real-time updates
 * - ✅ Simpler code
 */
@Singleton
class ML_ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    
    private val modelCache: File by lazy {
        File(context.cacheDir, MODEL_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Download latest model from Firestore (FREE!)
     * 
     * @param forceDownload If true, download even if cached version exists
     * @return Downloaded model artifacts or null if download fails
     */
    suspend fun downloadModel(forceDownload: Boolean = false): Result<ML_ModelArtifacts> {
        return try {
            // Check cache first
            if (!forceDownload) {
                val cachedModel = loadFromCache()
                if (cachedModel != null) {
                    android.util.Log.d(TAG, "Using cached model version: ${cachedModel.version}")
                    return Result.success(cachedModel)
                }
            }
            
            android.util.Log.d(TAG, "Fetching model from Firestore...")
            
            // Get model document from Firestore
            val modelDoc = firestore.collection(MODEL_COLLECTION)
                .document(MODEL_DOCUMENT_ID)
                .get()
                .await()
            
            if (!modelDoc.exists()) {
                android.util.Log.e(TAG, "Model document not found in Firestore")
                return Result.failure(Exception("Model not found"))
            }
            
            android.util.Log.d(TAG, "Model fetched from Firestore")
            
            // Parse model data
            val model = ML_ModelArtifacts.fromFirestore(modelDoc.data)
            
            if (model == null) {
                android.util.Log.e(TAG, "Failed to parse model from Firestore")
                return Result.failure(Exception("Invalid model format"))
            }
            
            // Validate model
            if (!model.validate()) {
                android.util.Log.e(TAG, "Model validation failed")
                return Result.failure(Exception("Model validation failed"))
            }
            
            // Save to local cache for offline use
            saveToCache(model)
            
            // Save version info
            saveVersionInfo(model.version, model.trainedAt)
            
            android.util.Log.d(TAG, "✅ Model ready: version=${model.version}, users=${model.userCount}, articles=${model.articleCount}")
            
            Result.success(model)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to download model", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if newer model version is available in Firestore
     */
    suspend fun isUpdateAvailable(): Boolean {
        return try {
            val modelDoc = firestore.collection(MODEL_COLLECTION)
                .document(MODEL_DOCUMENT_ID)
                .get()
                .await()
            
            if (!modelDoc.exists()) return false
            
            val remoteVersion = modelDoc.getString("version") ?: return false
            val localVersion = getLocalVersion()
            
            remoteVersion != localVersion
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check for updates", e)
            false
        }
    }
    
    /**
     * Load model from local cache
     */
    fun loadFromCache(): ML_ModelArtifacts? {
        val localFile = File(modelCache, MODEL_FILENAME)
        
        if (!localFile.exists()) {
            android.util.Log.d(TAG, "No cached model found")
            return null
        }
        
        return try {
            val json = localFile.readText()
            val model = ML_ModelArtifacts.fromJson(json)
            
            if (model != null && model.validate()) {
                android.util.Log.d(TAG, "Loaded model from cache: version=${model.version}")
                model
            } else {
                android.util.Log.e(TAG, "Cached model is invalid")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load cached model", e)
            null
        }
    }
    
    /**
     * Delete cached model
     */
    fun clearCache() {
        val localFile = File(modelCache, MODEL_FILENAME)
        if (localFile.exists()) {
            localFile.delete()
            android.util.Log.d(TAG, "Model cache cleared")
        }
        
        clearVersionInfo()
    }
    
    /**
     * Save model to local cache
     */
    private fun saveToCache(model: ML_ModelArtifacts) {
        try {
            val localFile = File(modelCache, MODEL_FILENAME)
            val json = model.toJson()
            localFile.writeText(json)
            android.util.Log.d(TAG, "Model saved to cache: ${localFile.length()} bytes")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save model to cache", e)
        }
    }
    
    /**
     * Get locally cached model version
     */
    private fun getLocalVersion(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MODEL_VERSION, null)
    }
    
    /**
     * Save version info to SharedPreferences
     */
    private fun saveVersionInfo(version: String, trainedAt: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL_VERSION, version)
            .putLong(KEY_MODEL_TRAINED_AT, trainedAt)
            .apply()
    }
    
    /**
     * Clear version info
     */
    private fun clearVersionInfo() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MODEL_VERSION)
            .remove(KEY_MODEL_TRAINED_AT)
            .apply()
    }
    
    /**
     * Check if model is cached locally
     */
    fun isModelCached(): Boolean {
        val localFile = File(modelCache, MODEL_FILENAME)
        return localFile.exists()
    }
    
    /**
     * Get cached model size in bytes
     */
    fun getCachedModelSize(): Long {
        val localFile = File(modelCache, MODEL_FILENAME)
        return if (localFile.exists()) localFile.length() else 0L
    }
    
    companion object {
        private const val TAG = "ML_ModelDownloader"
        
        // Firestore collection and document
        private const val MODEL_COLLECTION = "ml_models"
        private const val MODEL_DOCUMENT_ID = "recommendation_model_v1"
        
        // Local cache
        private const val MODEL_CACHE_DIR = "ml_models"
        private const val MODEL_FILENAME = "model.json"
        
        // SharedPreferences
        private const val PREFS_NAME = "ml_model_downloader"
        private const val KEY_MODEL_VERSION = "model_version"
        private const val KEY_MODEL_TRAINED_AT = "model_trained_at"
    }
}
