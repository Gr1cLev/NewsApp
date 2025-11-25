package com.example.newsapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.ProfileRepository
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
 * Manages user authentication state and profile data
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context
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

    init {
        checkAuthStatus()
    }

    /**
     * Check current authentication status
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val profile = ProfileRepository.getActiveProfile(context)
                if (profile != null) {
                    _userProfile.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                } else {
                    _authState.value = AuthState.Guest
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Guest
            }
        }
    }

    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            
            try {
                // Validate inputs
                if (email.isBlank() || !email.contains("@")) {
                    _errorMessage.value = "Please enter a valid email"
                    _authState.value = AuthState.Guest
                    return@launch
                }
                
                if (password.length < 3) {
                    _errorMessage.value = "Password must be at least 3 characters"
                    _authState.value = AuthState.Guest
                    return@launch
                }
                
                // Attempt login
                val result = ProfileRepository.authenticate(context, email, password)
                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        _authState.value = AuthState.Authenticated(profile)
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Invalid email or password"
                        _authState.value = AuthState.Guest
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed"
                _authState.value = AuthState.Guest
            }
        }
    }

    /**
     * Register new user
     */
    fun register(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            
            try {
                // Validate inputs
                if (firstName.isBlank() || lastName.isBlank()) {
                    _errorMessage.value = "Please enter your name"
                    _authState.value = AuthState.Guest
                    return@launch
                }
                
                if (email.isBlank() || !email.contains("@")) {
                    _errorMessage.value = "Please enter a valid email"
                    _authState.value = AuthState.Guest
                    return@launch
                }
                
                if (password.length < 3) {
                    _errorMessage.value = "Password must be at least 3 characters"
                    _authState.value = AuthState.Guest
                    return@launch
                }
                
                // Register new profile
                val result = ProfileRepository.registerProfile(
                    context = context,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password
                )
                
                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        _authState.value = AuthState.Authenticated(profile)
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Registration failed"
                        _authState.value = AuthState.Guest
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Registration failed"
                _authState.value = AuthState.Guest
            }
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                ProfileRepository.logout(context)
                _userProfile.value = null
                _authState.value = AuthState.Guest
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Logout failed"
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(firstName: String, lastName: String, email: String, password: String?) {
        viewModelScope.launch {
            _errorMessage.value = null
            
            try {
                val currentProfile = _userProfile.value ?: return@launch
                
                // Validate inputs
                if (firstName.isBlank() || lastName.isBlank()) {
                    _errorMessage.value = "Please enter your name"
                    return@launch
                }
                
                if (email.isBlank() || !email.contains("@")) {
                    _errorMessage.value = "Please enter a valid email"
                    return@launch
                }
                
                // Create updated profile
                val updatedProfile = currentProfile.copy(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = if (!password.isNullOrBlank()) password else currentProfile.password
                )
                
                val result = ProfileRepository.updateActiveProfile(context, updatedProfile)
                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        _authState.value = AuthState.Authenticated(profile)
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Profile update failed"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Profile update failed"
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
        checkAuthStatus()
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
