package com.example.newsapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.FragmentSettingsBinding
import com.example.newsapp.databinding.ViewSettingsExpandableBinding
import com.example.newsapp.navigation.AuthNavigator
import com.example.newsapp.ui.profile.ProfileFragment
import com.example.newsapp.navigation.ProfileNavigator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val profileNavigator: ProfileNavigator?
        get() = activity as? ProfileNavigator

    private val authNavigator: AuthNavigator?
        get() = activity as? AuthNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupToggles()
        setupExpandableSections()

        binding.buttonEditProfile.setOnClickListener {
            profileNavigator?.openEditProfile()
        }

        binding.buttonLogout.setOnClickListener { showLogoutConfirmation() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() = with(binding.settingsToolbar) {
        setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupToggles() = with(binding) {
        val context = requireContext()
        val nightEnabled = UserPreferences.isNightModeEnabled(context)
        switchNightMode.isChecked = nightEnabled

        val notificationsEnabled = UserPreferences.isNotificationsEnabled(context)
        switchNotifications.isChecked = notificationsEnabled

        switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setNightModeEnabled(context, isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            (activity as? AppCompatActivity)?.delegate?.applyDayNight()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setNotificationsEnabled(context, isChecked)
        }
    }

    private fun setupExpandableSections() = with(binding) {
        configureExpandable(cardFaq.root, R.string.settings_faq, R.string.settings_faq_content)
        configureExpandable(cardContact.root, R.string.settings_contact, R.string.settings_contact_content)
        configureExpandable(cardAbout.root, R.string.settings_about, R.string.settings_about_content)
    }

    private fun configureExpandable(root: View, titleRes: Int, contentRes: Int) {
        val binding = ViewSettingsExpandableBinding.bind(root)
        binding.headerTitle.setText(titleRes)
        binding.contentText.setText(contentRes)
        binding.headerIcon.rotation = if (binding.contentText.isVisible) 180f else 0f

        binding.headerRow.setOnClickListener {
            val expanding = !binding.contentText.isVisible
            binding.contentText.isVisible = expanding
            binding.headerIcon.rotation = if (expanding) 180f else 0f
        }
    }

    private fun showLogoutConfirmation() {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout_confirm_yes) { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .setNegativeButton(R.string.logout_confirm_no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.logout(requireContext())
            }
            if (!isAdded) return@launch
            result.onSuccess {
                Toast.makeText(requireContext(), R.string.toast_logout_success, Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult(ProfileFragment.PROFILE_UPDATED_RESULT, android.os.Bundle())
                authNavigator?.openLogin()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: getString(R.string.error_generic),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
