package com.example.newsapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.newsapp.R
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

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

    private fun populateFields() = with(binding) {
        val defaults = UserPreferences.Profile(
            firstName = getString(R.string.profile_first_name_default),
            lastName = getString(R.string.profile_last_name_default),
            email = getString(R.string.profile_email_default),
            password = ""
        )
        val profile = UserPreferences.getProfile(requireContext(), defaults)
        firstNameInput.setText(profile.firstName)
        lastNameInput.setText(profile.lastName)
        emailInput.setText(profile.email)
        passwordInput.setText(profile.password)
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

        UserPreferences.saveProfile(
            requireContext(),
            UserPreferences.Profile(firstName, lastName, email, password)
        )

        Toast.makeText(requireContext(), R.string.toast_profile_saved, Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
