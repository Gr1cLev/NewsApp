package com.example.newsapp.ui.profile

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.FragmentProfileBinding
import com.example.newsapp.databinding.ViewProfileStatBinding
import com.example.newsapp.navigation.ProfileNavigator
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileNavigator: ProfileNavigator?
        get() = activity as? ProfileNavigator

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
        setupProfileCard()
        setupStats()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        setupProfileCard()
        setupStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupProfileCard() = with(binding) {
        val defaultProfile = UserPreferences.Profile(
            firstName = getString(R.string.profile_first_name_default),
            lastName = getString(R.string.profile_last_name_default),
            email = getString(R.string.profile_email_default),
            password = ""
        )
        val profile = UserPreferences.getProfile(requireContext(), defaultProfile)
        val displayName = listOf(profile.firstName, profile.lastName)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")

        val resolvedName = if (displayName.isBlank()) defaultProfile.firstName else displayName

        profileName.text = resolvedName
        profileEmail.text = if (profile.email.isBlank()) defaultProfile.email else profile.email
        avatarInitials.text = resolvedName.split(" ")
            .take(2)
            .joinToString("") { it.firstOrNull()?.uppercase(Locale.getDefault()) ?: "" }
    }

    private fun setupStats() = with(binding) {
        val bookmarks = NewsRepository.getBookmarks(requireContext())
        applyStat(
            stat = statBookmarks,
            iconRes = R.drawable.ic_nav_bookmarks,
            value = bookmarks.size.toString(),
            labelRes = R.string.profile_stat_bookmarks,
            startColor = requireContext().getColor(R.color.primary_blue_light),
            endColor = requireContext().getColor(R.color.primary_blue)
        )

        applyStat(
            stat = statTopics,
            iconRes = R.drawable.ic_topics,
            value = "12",
            labelRes = R.string.profile_stat_topics,
            startColor = requireContext().getColor(R.color.accent_teal),
            endColor = requireContext().getColor(R.color.accent_blue)
        )

        applyStat(
            stat = statFollowing,
            iconRes = R.drawable.ic_person_add,
            value = "8",
            labelRes = R.string.profile_stat_following,
            startColor = requireContext().getColor(R.color.accent_magenta),
            endColor = requireContext().getColor(R.color.accent_violet)
        )

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

    private fun applyStat(
        stat: ViewProfileStatBinding,
        iconRes: Int,
        value: String,
        labelRes: Int,
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ) {
        stat.statIcon.setImageResource(iconRes)
        stat.statValue.text = value
        stat.statLabel.setText(labelRes)
        stat.statIconContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TR_BL,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = 18f * resources.displayMetrics.density
        }
    }
}
