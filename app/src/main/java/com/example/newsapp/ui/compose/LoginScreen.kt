package com.example.newsapp.ui.compose

import android.app.Activity
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.graphics.luminance
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.firebase.FirebaseAuthRepository
import com.example.newsapp.di.FirebaseEntryPoint
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Get FirebaseAuthRepository from Hilt
    val firebaseAuthRepository = remember {
        val appContext = context.applicationContext
        EntryPointAccessors.fromApplication(
            appContext,
            FirebaseEntryPoint::class.java
        ).firebaseAuthRepository()
    }

    // Google Sign-In setup
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    scope.launch {
                        isLoading = true
                        val authResult = firebaseAuthRepository.signInWithGoogle(idToken)
                        isLoading = false
                        authResult.onSuccess {
                            toastMessage = "Login dengan Google berhasil!"
                            onAuthenticated()
                        }.onFailure { error ->
                            toastMessage = "Google Sign-In gagal: ${error.message}"
                        }
                    }
                } ?: run {
                    toastMessage = "ID Token tidak ditemukan"
                }
            } catch (e: ApiException) {
                toastMessage = "Google Sign-In gagal: ${e.message}"
            }
        }
    }

    toastMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    fun validateForm(): Boolean {
        emailError = null
        passwordError = null

        val emailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!emailValid) {
            emailError = context.getString(R.string.error_invalid_email)
        }
        if (password.isBlank()) {
            passwordError = context.getString(R.string.error_required_field)
        }
        return emailValid && password.isNotBlank()
    }

    fun attemptEmailLogin() {
        if (!validateForm()) return
        scope.launch {
            isLoading = true
            
            // Try Firebase Auth first
            val firebaseResult = firebaseAuthRepository.signInWithEmail(email.trim(), password)
            
            if (firebaseResult.isSuccess) {
                isLoading = false
                toastMessage = "Login berhasil!"
                onAuthenticated()
                return@launch
            }
            
            // Fallback to ProfileRepository for backward compatibility
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.authenticate(context, email.trim(), password)
            }
            isLoading = false
            result.onSuccess { profile ->
                toastMessage = context.getString(
                    R.string.toast_login_success,
                    profile.fullName().ifBlank { profile.email }
                )
                onAuthenticated()
            }.onFailure { error ->
                passwordError = when (error) {
                    is ProfileRepository.InvalidCredentialsException ->
                        context.getString(R.string.error_invalid_credentials)
                    else -> error.message ?: context.getString(R.string.error_generic)
                }
            }
        }
    }

    fun attemptAnonymousLogin() {
        scope.launch {
            isLoading = true
            val result = firebaseAuthRepository.signInAnonymously()
            isLoading = false
            result.onSuccess {
                toastMessage = "Masuk sebagai Guest berhasil!"
                onAuthenticated()
            }.onFailure { error ->
                toastMessage = "Anonymous login gagal: ${error.message}"
            }
        }
    }

    fun attemptGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    AuthBackgroundLayout(
        title = context.getString(R.string.auth_login_title),
        subtitle = context.getString(R.string.auth_login_subtitle),
        actionText = context.getString(R.string.auth_login_button),
        footerText = context.getString(R.string.auth_login_register_prompt),
        onPrimaryAction = ::attemptEmailLogin,
        onSecondaryAction = onNavigateToRegister,
        isActionEnabled = !isLoading,
        isLoading = isLoading
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = context.getString(R.string.hint_email),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                isError = emailError != null,
                supportingText = emailError
            )

            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = context.getString(R.string.hint_password),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = passwordError
            )

            // Divider with "atau" text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "atau",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            // Google Sign-In Button
            OutlinedButton(
                onClick = ::attemptGoogleSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Masuk dengan Google")
            }

            // Anonymous Mode Button
            OutlinedButton(
                onClick = ::attemptAnonymousLogin,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Guest",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Lanjutkan sebagai Guest")
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    toastMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    fun validate(): Boolean {
        firstNameError = if (firstName.isBlank()) context.getString(R.string.error_required_field) else null
        lastNameError = if (lastName.isBlank()) context.getString(R.string.error_required_field) else null
        emailError = when {
            email.isBlank() -> context.getString(R.string.error_required_field)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                context.getString(R.string.error_invalid_email)
            else -> null
        }
        passwordError = when {
            password.length < 3 -> context.getString(R.string.error_password_too_short)
            else -> null
        }
        return listOf(firstNameError, lastNameError, emailError, passwordError).all { it == null }
    }

    fun attemptRegister() {
        if (!validate()) return
        scope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                ProfileRepository.registerProfile(
                    context = context,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = email.trim(),
                    password = password
                )
            }
            isLoading = false
            result.onSuccess { profile ->
                toastMessage = context.getString(
                    R.string.toast_register_success,
                    profile.fullName().ifBlank { profile.email }
                )
                onRegistered()
            }.onFailure { error ->
                when (error) {
                    is ProfileRepository.EmailAlreadyExistsException -> {
                        emailError = context.getString(R.string.error_email_exists)
                    }
                    else -> {
                        toastMessage = error.message ?: context.getString(R.string.error_generic)
                    }
                }
            }
        }
    }

    AuthBackgroundLayout(
        title = context.getString(R.string.auth_register_title),
        subtitle = context.getString(R.string.auth_register_subtitle),
        actionText = context.getString(R.string.auth_register_button),
        footerText = context.getString(R.string.auth_register_login_prompt),
        onPrimaryAction = ::attemptRegister,
        onSecondaryAction = onNavigateToLogin,
        isActionEnabled = !isLoading,
        isLoading = isLoading
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    if (firstNameError != null) firstNameError = null
                },
                label = context.getString(R.string.hint_first_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = firstNameError != null,
                supportingText = firstNameError
            )
            AuthTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    if (lastNameError != null) lastNameError = null
                },
                label = context.getString(R.string.hint_last_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = lastNameError != null,
                supportingText = lastNameError
            )
            AuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = context.getString(R.string.hint_email),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                isError = emailError != null,
                supportingText = emailError
            )
            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = context.getString(R.string.hint_password),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = passwordError
            )
        }
    }
}

@Composable
private fun AuthBackgroundLayout(
    title: String,
    subtitle: String,
    actionText: String,
    footerText: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    isActionEnabled: Boolean,
    isLoading: Boolean,
    content: @Composable (PaddingValues) -> Unit
) {
    val contentScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        AuthVideoBackground(
            modifier = Modifier.fillMaxSize(),
            videoRes = R.raw.auth_background
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(contentScrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content(PaddingValues(0.dp))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = isActionEnabled,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    TextButton(onClick = onSecondaryAction, enabled = !isLoading) {
                        Text(text = footerText, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    isError: Boolean,
    supportingText: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { text ->
            {
                Text(text = text, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun AuthVideoBackground(
    modifier: Modifier,
    @RawRes videoRes: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                val uri = RawResourceDataSource.buildRawResourceUri(videoRes)
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }

            override fun onPause(owner: LifecycleOwner) {
                exoPlayer.pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                exoPlayer.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                this.player = exoPlayer
            }
        }
    )
}
