package com.example.newsapp.data

import android.content.Context
import android.util.Log
import com.example.newsapp.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

object ProfileRepository {

    private const val ASSET_PROFILE_FILE = "profile_data.json"
    private const val PROFILE_FILE_NAME = "profile_data.json"

    private val lock = Any()

    private val defaultAdminProfile = UserProfile(
        id = "profile-admin",
        firstName = "Admin",
        lastName = "Aplikasi",
        email = "admin@beritakini.id",
        password = "123"
    )

    class InvalidCredentialsException : Exception("Invalid email or password.")
    class EmailAlreadyExistsException : Exception("Email is already registered.")
    class NoActiveSessionException : Exception("No active profile found.")

    fun hasActiveProfile(context: Context): Boolean = synchronized(lock) {
        val store = loadStore(context)
        val activeId = store.activeProfileId ?: return@synchronized false
        store.profiles.any { it.id == activeId }
    }

    fun getActiveProfile(context: Context): UserProfile? = synchronized(lock) {
        val store = loadStore(context)
        val activeId = store.activeProfileId ?: return@synchronized null
        store.profiles.firstOrNull { it.id == activeId }
    }

    fun requireActiveProfile(context: Context): UserProfile =
        getActiveProfile(context) ?: throw NoActiveSessionException()

    fun getProfiles(context: Context): List<UserProfile> = synchronized(lock) {
        loadStore(context).profiles.toList()
    }

    fun authenticate(context: Context, email: String, password: String): Result<UserProfile> =
        runCatching {
            synchronized(lock) {
                val store = loadStore(context)
                val profile = store.profiles.firstOrNull { it.email.equals(email, ignoreCase = true) }
                    ?: throw InvalidCredentialsException()
                if (profile.password != password) {
                    throw InvalidCredentialsException()
                }
                store.activeProfileId = profile.id
                saveStore(context, store)
                profile
            }
        }

    fun registerProfile(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<UserProfile> = runCatching {
        synchronized(lock) {
            val store = loadStore(context)
            if (store.profiles.any { it.email.equals(email, ignoreCase = true) }) {
                throw EmailAlreadyExistsException()
            }
            val newProfile = UserProfile(
                id = generateId(),
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password
            )
            store.profiles.add(newProfile)
            store.activeProfileId = newProfile.id
            saveStore(context, store)
            newProfile
        }
    }

    fun updateActiveProfile(context: Context, profile: UserProfile): Result<UserProfile> =
        runCatching {
            synchronized(lock) {
                val store = loadStore(context)
                val activeId = store.activeProfileId ?: throw NoActiveSessionException()
                val index = store.profiles.indexOfFirst { it.id == activeId }
                if (index == -1) throw NoActiveSessionException()
                val normalized = profile.copy(id = activeId)
                store.profiles[index] = normalized
                saveStore(context, store)
                normalized
            }
        }

    fun deleteProfile(context: Context, profileId: String): Result<Boolean> =
        runCatching {
            synchronized(lock) {
                val store = loadStore(context)
                val removed = store.profiles.removeAll { it.id == profileId }
                if (!removed) {
                    return@synchronized false
                }
                if (store.activeProfileId == profileId) {
                    store.activeProfileId = store.profiles.firstOrNull()?.id
                }
                if (store.profiles.isEmpty()) {
                    store.profiles.add(defaultAdminProfile.copy(id = generateId()))
                    store.activeProfileId = null
                }
                saveStore(context, store)
                true
            }
        }

    fun setActiveProfile(context: Context, profileId: String?): Result<Unit> =
        runCatching {
            synchronized(lock) {
                val store = loadStore(context)
                if (profileId != null && store.profiles.none { it.id == profileId }) {
                    throw IllegalArgumentException("Profile not found.")
                }
                store.activeProfileId = profileId
                saveStore(context, store)
            }
        }

    fun logout(context: Context): Result<Unit> =
        setActiveProfile(context, null)

    private fun loadStore(context: Context): ProfileStore {
        ensureFile(context)
        val file = profileFile(context)
        if (!file.exists()) {
            return ProfileStore(null, mutableListOf(defaultAdminProfile))
        }
        val raw = runCatching { file.readText() }.getOrDefault("")
        if (raw.isBlank()) {
            return ProfileStore(null, mutableListOf(defaultAdminProfile))
        }
        val root = JSONObject(raw)
        var activeId = root.optString("activeProfileId").takeIf { it.isNotBlank() }
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = mutableListOf<UserProfile>()
        for (index in 0 until profilesArray.length()) {
            val obj = profilesArray.optJSONObject(index) ?: continue
            profiles += obj.toUserProfile()
        }

        var changed = false
        if (profiles.none { it.email.equals(defaultAdminProfile.email, ignoreCase = true) }) {
            profiles += defaultAdminProfile
            changed = true
        }
        if (activeId != null && profiles.none { it.id == activeId }) {
            activeId = null
            changed = true
        }
        val store = ProfileStore(activeId, profiles)
        if (changed) {
            saveStore(context, store)
        }
        return store
    }

    private fun saveStore(context: Context, store: ProfileStore) {
        val root = JSONObject().apply {
            put("activeProfileId", store.activeProfileId)
            put("profiles", JSONArray().apply {
                store.profiles.forEach { profile ->
                    put(profile.toJson())
                }
            })
        }
        val file = profileFile(context)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(root.toString(2))
        }.getOrElse { error ->
            throw IOException("Failed to write profile data", error)
        }
        Log.d("ProfileRepository", "Profile data saved to ${file.absolutePath}")
    }

    private fun ensureFile(context: Context) {
        val file = profileFile(context)
        if (file.exists()) return

        runCatching {
            context.assets.open(ASSET_PROFILE_FILE).use { input ->
                file.parentFile?.mkdirs()
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure {
            val store = ProfileStore(
                activeProfileId = null,
                profiles = mutableListOf(defaultAdminProfile)
            )
            saveStore(context, store)
        }
    }

    private fun profileFile(context: Context): File {
        val preferredDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(preferredDir, PROFILE_FILE_NAME)
    }

    private fun JSONObject.toUserProfile(): UserProfile {
        val id = optString("id").takeIf { it.isNotBlank() } ?: generateId()
        return UserProfile(
            id = id,
            firstName = optString("firstName"),
            lastName = optString("lastName"),
            email = optString("email"),
            password = optString("password")
        )
    }

    private fun UserProfile.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("firstName", firstName)
        put("lastName", lastName)
        put("email", email)
        put("password", password)
    }

    private fun generateId(): String = "profile-${UUID.randomUUID()}"

    private data class ProfileStore(
        var activeProfileId: String?,
        val profiles: MutableList<UserProfile>
    )
}
