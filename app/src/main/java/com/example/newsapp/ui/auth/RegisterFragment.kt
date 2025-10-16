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
import com.example.newsapp.databinding.FragmentRegisterBinding
import com.example.newsapp.navigation.AuthNavigator
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null

    private val authNavigator: AuthNavigator?
        get() = activity as? AuthNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeBackground()
        binding.buttonRegister.setOnClickListener { attemptRegistration() }
        binding.linkLogin.setOnClickListener { authNavigator?.openLogin() }
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        player?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }

    private fun attemptRegistration() = with(binding) {
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

        if (!isEmailValid(email)) {
            emailLayout.error = getString(R.string.error_invalid_email)
            hasError = true
        } else {
            emailLayout.error = null
        }

        if (password.length < 3) {
            passwordLayout.error = getString(R.string.error_password_too_short)
            hasError = true
        } else {
            passwordLayout.error = null
        }

        if (hasError) return@with

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.registerProfile(
                    context = requireContext(),
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password
                )
            }
            setLoading(false)
            result.onSuccess { profile ->
                if (!isAdded) return@onSuccess
                val displayName = profile.fullName().ifBlank { profile.email }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_register_success, displayName),
                    Toast.LENGTH_SHORT
                ).show()
                authNavigator?.onAuthenticationSuccess()
            }.onFailure { error ->
                if (!isAdded) return@onFailure
                when (error) {
                    is ProfileRepository.EmailAlreadyExistsException -> {
                        emailLayout.error = getString(R.string.error_email_exists)
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            error.message ?: getString(R.string.error_generic),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        buttonRegister.isEnabled = !loading
        firstNameLayout.isEnabled = !loading
        lastNameLayout.isEnabled = !loading
        emailLayout.isEnabled = !loading
        passwordLayout.isEnabled = !loading
    }

    private fun initializeBackground() {
        val context = requireContext()
        val backgroundPlayer = ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                val uri = RawResourceDataSource.buildRawResourceUri(R.raw.auth_background)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
        binding.backgroundPlayer.player = backgroundPlayer
        player = backgroundPlayer
    }

    private fun releasePlayer() {
        player?.release()
        player = null
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
