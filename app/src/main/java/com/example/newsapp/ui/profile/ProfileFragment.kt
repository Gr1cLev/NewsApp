package com.example.newsapp.ui.profile

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.example.newsapp.R
import com.example.newsapp.databinding.FragmentProfileBinding
import com.example.newsapp.databinding.ViewProfileStatBinding
import com.example.newsapp.model.sampleBookmarks

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupProfileCard() = with(binding) {
        // Values are static placeholders, update here when wiring real data
        avatarInitials.text = getString(R.string.profile_initials)
        profileName.text = getString(R.string.profile_name)
        profileEmail.text = getString(R.string.profile_email)
        profileStatus.text = getString(R.string.profile_membership)
    }

    private fun setupStats() = with(binding) {
        applyStat(
            stat = statBookmarks,
            iconRes = R.drawable.ic_nav_bookmarks,
            value = sampleBookmarks.size.toString(),
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
    }

    private fun setupButtons() = with(binding) {
        buttonSettings.setOnClickListener {
            Toast.makeText(requireContext(), R.string.toast_settings_coming_soon, Toast.LENGTH_SHORT).show()
        }
        buttonEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), R.string.toast_edit_profile_unavailable, Toast.LENGTH_SHORT).show()
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
