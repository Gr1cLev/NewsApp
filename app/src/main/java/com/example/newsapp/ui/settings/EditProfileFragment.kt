package com.example.newsapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.databinding.FragmentEditProfileBinding
import com.example.newsapp.model.UserProfile
import com.example.newsapp.ui.profile.ProfileFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var currentProfile: UserProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        populateFields()
        binding.buttonSave.setOnClickListener { saveProfile() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() = with(binding.editProfileToolbar) {
        setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun populateFields() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                ProfileRepository.getActiveProfile(requireContext())
            }
            if (!isAdded) return@launch
            if (profile == null) {
                Toast.makeText(requireContext(), R.string.error_profile_missing, Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return@launch
            }
            currentProfile = profile
            binding.firstNameInput.setText(profile.firstName)
            binding.lastNameInput.setText(profile.lastName)
            binding.emailInput.setText(profile.email)
            binding.passwordInput.setText(profile.password)
        }
    }

    private fun saveProfile() = with(binding) {
        val firstName = firstNameInput.text?.toString()?.trim().orEmpty()
        val lastName = lastNameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (firstName.isBlank()) {
            firstNameLayout.error = getString(R.string.error_required_field)
            hasError = true
        } else {
            firstNameLayout.error = null
        }

        if (lastName.isBlank()) {
            lastNameLayout.error = getString(R.string.error_required_field)
            hasError = true
        } else {
            lastNameLayout.error = null
        }

        if (email.isBlank()) {
            emailLayout.error = getString(R.string.error_required_field)
            hasError = true
        } else {
            emailLayout.error = null
        }

        if (hasError) return@with

        val updatedProfile = UserProfile(
            id = currentProfile?.id ?: "",
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = password
        )

        viewLifecycleOwner.lifecycleScope.launch {
            setSaving(true)
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.updateActiveProfile(requireContext(), updatedProfile)
            }
            setSaving(false)
            result.onSuccess { savedProfile ->
                currentProfile = savedProfile
                parentFragmentManager.setFragmentResult(
                    ProfileFragment.PROFILE_UPDATED_RESULT,
                    bundleOf()
                )
                Toast.makeText(requireContext(), R.string.toast_profile_saved, Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: getString(R.string.error_profile_update_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        binding.buttonSave.isEnabled = !saving
        binding.firstNameLayout.isEnabled = !saving
        binding.lastNameLayout.isEnabled = !saving
        binding.emailLayout.isEnabled = !saving
        binding.passwordLayout.isEnabled = !saving
    }
}
