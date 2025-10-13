package com.example.newsapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.databinding.FragmentLoginBinding
import com.example.newsapp.navigation.AuthNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authNavigator: AuthNavigator?
        get() = activity as? AuthNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.linkRegister.setOnClickListener { authNavigator?.openRegister() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attemptLogin() = with(binding) {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (!isEmailValid(email)) {
            emailLayout.error = getString(R.string.error_invalid_email)
            hasError = true
        } else {
            emailLayout.error = null
        }

        if (password.isBlank()) {
            passwordLayout.error = getString(R.string.error_required_field)
            hasError = true
        } else {
            passwordLayout.error = null
        }

        if (hasError) return@with

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.authenticate(requireContext(), email, password)
            }
            setLoading(false)
            result.onSuccess { profile ->
                if (!isAdded) return@onSuccess
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_login_success, profile.fullName().ifBlank { profile.email }),
                    Toast.LENGTH_SHORT
                ).show()
                authNavigator?.onAuthenticationSuccess()
            }.onFailure { error ->
                if (!isAdded) return@onFailure
                val message = when (error) {
                    is ProfileRepository.InvalidCredentialsException -> getString(R.string.error_invalid_credentials)
                    else -> error.message ?: getString(R.string.error_generic)
                }
                passwordLayout.error = message
            }
        }
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        buttonLogin.isEnabled = !loading
        emailLayout.isEnabled = !loading
        passwordLayout.isEnabled = !loading
    }

    private fun isEmailValid(email: String): Boolean {
        if (email.isBlank()) return false
        if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) return true
        val parts = email.split("@")
        if (parts.size != 2) return false
        if (parts[0].isBlank() || parts[1].isBlank()) return false
        return parts[1].any { it == '.' } || parts[1].length >= 2
    }
}
