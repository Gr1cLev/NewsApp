package com.example.newsapp.util

/**
 * Wrapper class untuk handle state dari network/database operations
 * Menggunakan sealed class untuk type-safe state management
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    
    fun getDataOrNull(): T? = (this as? Success)?.data
}

/**
 * Extension function untuk convert Response ke Resource
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(apiCall())
    } catch (e: Exception) {
        Resource.Error(
            message = e.message ?: "Unknown error occurred",
            exception = e
        )
    }
}
