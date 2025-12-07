package com.example.newsapp.data.firebase

import com.example.newsapp.model.firebase.User
import com.example.newsapp.model.firebase.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Firebase Authentication
 * Handles user authentication and user document management
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    // Lazy injection to avoid circular dependency
    @Inject
    lateinit var preferenceTracker: com.example.newsapp.ml.ML_UserPreferenceTracker
    
    /**
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean = firebaseAuth.currentUser != null
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim()
            val result = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }
    
    /**
     * Register with email and password
     */
    suspend fun registerWithEmail(
        email: String, 
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                // Create user document in Firestore
                createUserDocument(firebaseUser, displayName)
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("Registration failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in anonymously
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInAnonymously().await()
            result.user?.let { firebaseUser ->
                createUserDocument(firebaseUser, "Guest User", isAnonymous = true)
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("Anonymous sign-in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("No user logged in"))
            
            // Delete user document from Firestore
            firestore.collection("users").document(userId).delete().await()
            
            // Delete Firebase Auth user
            firebaseAuth.currentUser?.delete()?.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create user document in Firestore
     */
    private suspend fun createUserDocument(
        firebaseUser: FirebaseUser,
        displayName: String = "",
        isAnonymous: Boolean = false
    ) {
        val user = User(
            userId = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = displayName.ifBlank { firebaseUser.displayName ?: "User" },
            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
            isAnonymous = isAnonymous,
            preferences = UserPreferences()
        )
        
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .await()
        
        // Initialize balanced ML preferences for new user
        preferenceTracker.initializeBalancedPreferences(firebaseUser.uid)
    }
    
    /**
     * Create or update user document (for Google sign-in)
     */
    private suspend fun createOrUpdateUserDocument(firebaseUser: FirebaseUser) {
        val userDoc = firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()
        
        if (!userDoc.exists()) {
            createUserDocument(firebaseUser, firebaseUser.displayName ?: "User")
        } else {
            // Update existing user info
            firestore.collection("users")
                .document(firebaseUser.uid)
                .update(
                    mapOf(
                        "email" to (firebaseUser.email ?: ""),
                        "displayName" to (firebaseUser.displayName ?: "User"),
                        "avatarUrl" to (firebaseUser.photoUrl?.toString() ?: "")
                    )
                )
                .await()
        }
    }
    
    /**
     * Get user document from Firestore
     */
    suspend fun getUserDocument(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            document.toObject(User::class.java)?.let {
                Result.success(it)
            } ?: Result.failure(Exception("User document not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("preferences", preferences)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(
        userId: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            displayName?.let { updates["displayName"] = it }
            avatarUrl?.let { updates["avatarUrl"] = it }
            
            if (updates.isNotEmpty()) {
                firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile details (first/last/email) in Firestore user document.
     * Note: does not update Firebase Auth email; only Firestore profile fields.
     */
    /**
     * Update profile details and optionally password (requires current password).
     * Email is not changed here; only displayName + optional password update.
     */
    suspend fun updateUserProfileWithPassword(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        currentPassword: String,
        newPassword: String?
    ): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            val currentEmail = user.email ?: throw Exception("Email not available")

            // Re-authenticate with current email + provided current password
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            user.reauthenticate(credential).await()

            // Update password if provided
            if (!newPassword.isNullOrBlank()) {
                user.updatePassword(newPassword).await()
            }

            val displayName = listOf(firstName, lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "User" }

            // Update display name in Auth
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()

            // Update Firestore doc (displayName + email for consistency)
            val updates = mapOf(
                "displayName" to displayName,
                "email" to email
            )

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    private fun mapAuthException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthInvalidUserException ->
                Exception("Account not found. Please register first.")
            is FirebaseAuthInvalidCredentialsException ->
                Exception("Incorrect password. Please check your password.")
            else -> e
        }
    }
}
