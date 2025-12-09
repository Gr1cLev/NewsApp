package com.example.newsapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile/Auth
 * Manages user authentication state and profile data using Firebase
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuthRepository: FirebaseAuthRepository
) : ViewModel() {

    // Auth state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // User profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check current authentication status from Firebase
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val currentUser = firebaseAuthRepository.getCurrentUser()
            if (currentUser != null) {
                // Create UserProfile from Firebase User
                val profile = UserProfile(
                    id = currentUser.uid,
                    firstName = currentUser.displayName?.split(" ")?.getOrNull(0) ?: "",
                    lastName = currentUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                    email = currentUser.email ?: "",
                    password = "" // Not stored for security
                )
                _userProfile.value = profile
                _authState.value = AuthState.Authenticated(profile)

                // Try to refresh from Firestore document for latest data
                loadRemoteProfile()
            } else {
                _authState.value = AuthState.Guest
            }
        }
    }

    /**
     * Load profile data from Firestore users collection
     */
    fun loadRemoteProfile() {
        viewModelScope.launch {
            val uid = firebaseAuthRepository.getCurrentUserId() ?: return@launch
            _isLoadingProfile.value = true
            val result = firebaseAuthRepository.getUserDocument(uid)
            result.onSuccess { user ->
                val parts = user.displayName.split(" ")
                val first = parts.getOrNull(0) ?: ""
                val last = parts.drop(1).joinToString(" ")
                _userProfile.value = UserProfile(
                    id = user.userId,
                    firstName = first,
                    lastName = last,
                    email = user.email,
                    password = "" // not used
                )
                _authState.value = AuthState.Authenticated(_userProfile.value!!)
            }.onFailure { e ->
                _errorMessage.value = e.message
            }
            _isLoadingProfile.value = false
        }
    }

    /**
     * Update profile in Firestore users collection
     */
    suspend fun saveProfile(
        firstName: String,
        lastName: String,
        email: String,
        currentPassword: String,
        newPassword: String?
    ): Result<Unit> {
        val uid = firebaseAuthRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))
        _isSavingProfile.value = true
        val result = firebaseAuthRepository.updateUserProfileWithPassword(
            userId = uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            currentPassword = currentPassword,
            newPassword = newPassword
        )
        result.onSuccess {
            _userProfile.value = UserProfile(
                id = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = "" // not used
            )
            _authState.value = AuthState.Authenticated(_userProfile.value!!)
        }.onFailure { e ->
            _errorMessage.value = e.message
        }
        _isSavingProfile.value = false
        return result
    }

    /**
     * Refresh auth state (call after login/register from LoginScreen)
     */
    fun refreshAuthState() {
        checkAuthStatus()
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuthRepository.signOut()
                _userProfile.value = null
                _authState.value = AuthState.Guest
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Logout failed"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh profile data
     */
    fun refresh() {
        viewModelScope.launch {
            val uid = firebaseAuthRepository.getCurrentUserId()
            if (uid == null) {
                _authState.value = AuthState.Guest
                _userProfile.value = null
            } else {
                loadRemoteProfile()
            }
        }
    }
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object Loading : AuthState()
    object Guest : AuthState()
    data class Authenticated(val profile: UserProfile) : AuthState()
}
