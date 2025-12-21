package com.example.newsapp.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.newsapp.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newsapp.model.UserProfile
import com.example.newsapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val profile by profileViewModel.userProfile.collectAsState()
    val isLoadingProfile by profileViewModel.isLoadingProfile.collectAsState()
    val isSaving by profileViewModel.isSavingProfile.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        profileViewModel.loadRemoteProfile()
    }

    LaunchedEffect(profile) {
        profile?.let { p ->
            firstName = p.firstName
            lastName = p.lastName
            email = p.email
            currentPassword = ""
            newPassword = ""
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            profileViewModel.clearError()
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
        currentPasswordError = if (currentPassword.isBlank()) context.getString(R.string.error_required_field) else null
        newPasswordError = if (newPassword.isNotBlank() && newPassword.length < 6) {
            "New password must be at least 6 characters"
        } else null

        return listOf(firstNameError, lastNameError, emailError, currentPasswordError, newPasswordError).all { it == null }
    }

    fun saveProfile() {
        if (!validate()) return
        coroutineScope.launch {
            val result = profileViewModel.saveProfile(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                currentPassword = currentPassword,
                newPassword = newPassword.ifBlank { null }
            )
            result.onSuccess {
                Toast.makeText(context, context.getString(R.string.toast_profile_saved), Toast.LENGTH_SHORT).show()
                onProfileSaved()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: context.getString(R.string.error_profile_update_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_edit_profile),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.logout_confirm_no)
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isLoadingProfile && !isSaving,
                        onClick = { saveProfile() }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    if (firstNameError != null) firstNameError = null
                },
                label = { Text(stringResource(R.string.hint_first_name)) },
                isError = firstNameError != null,
                supportingText = firstNameError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingProfile && !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    if (lastNameError != null) lastNameError = null
                },
                label = { Text(stringResource(R.string.hint_last_name)) },
                isError = lastNameError != null,
                supportingText = lastNameError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingProfile && !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    // Email not editable
                },
                label = { Text(stringResource(R.string.hint_email)) },
                isError = emailError != null,
                supportingText = emailError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                )
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = {
                    currentPassword = it
                    if (currentPasswordError != null) currentPasswordError = null
                },
                label = { Text(stringResource(R.string.hint_password)) },
                isError = currentPasswordError != null,
                supportingText = currentPasswordError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingProfile && !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    if (newPasswordError != null) newPasswordError = null
                },
                label = { Text("New password (optional)") },
                placeholder = { Text("Leave blank to keep current password") },
                isError = newPasswordError != null,
                supportingText = newPasswordError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingProfile && !isSaving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = { saveProfile() },
                enabled = !isLoadingProfile && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
