package com.example.newsapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.FragmentProfileBinding
import com.example.newsapp.navigation.AuthNavigator
import com.example.newsapp.navigation.ProfileNavigator
import com.example.newsapp.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileNavigator: ProfileNavigator?
        get() = activity as? ProfileNavigator

    private val authNavigator: AuthNavigator?
        get() = activity as? AuthNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            PROFILE_UPDATED_RESULT,
            this
        ) { _, _ ->
            if (isAdded && _binding != null) {
                loadProfile()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfile()
        updatePreferences()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
        updatePreferences()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                ProfileRepository.getActiveProfile(requireContext())
            }
            if (!isAdded) return@launch
            if (profile == null) {
                authNavigator?.openLogin()
                displayGuestProfile()
                updatePreferences()
                return@launch
            }
            renderProfile(profile)
            updatePreferences()
        }
    }

    private fun renderProfile(profile: UserProfile) = with(binding) {
        val displayName = profile.fullName().ifBlank { profile.email.substringBefore("@") }
        val resolvedEmail = profile.email

        profileName.text = displayName
        profileEmail.text = resolvedEmail
        avatarInitials.text = displayName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.firstOrNull()?.uppercase(Locale.getDefault()) ?: "" }
    }

    private fun displayGuestProfile() = with(binding) {
        val placeholder = getString(R.string.profile_guest_label)
        profileName.text = placeholder
        profileEmail.text = getString(R.string.profile_guest_email_placeholder)
        avatarInitials.text = placeholder.take(2).uppercase(Locale.getDefault())
    }

    private fun updatePreferences() = with(binding) {
        if (!isAdded) {
            return@with
        }
        val notificationsEnabled = UserPreferences.isNotificationsEnabled(requireContext())
        val nightModeEnabled = UserPreferences.isNightModeEnabled(requireContext())
        profileNotifications.text = if (notificationsEnabled) {
            getString(R.string.profile_pref_notifications_on)
        } else {
            getString(R.string.profile_pref_notifications_off)
        }
        profileReadingMode.text = if (nightModeEnabled) {
            getString(R.string.profile_pref_reading_on)
        } else {
            getString(R.string.profile_pref_reading_off)
        }
    }

    private fun setupButtons() = with(binding) {
        buttonSettings.setOnClickListener {
            profileNavigator?.openSettings()
        }
    }

    companion object {
        const val PROFILE_UPDATED_RESULT = "profile_updated_result"
    }
}
