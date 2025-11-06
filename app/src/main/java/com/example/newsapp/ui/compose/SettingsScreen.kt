package com.example.newsapp.ui.compose

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsapp.R
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var nightMode by remember { mutableStateOf(isDarkTheme) }
    var notificationsEnabled by remember { mutableStateOf(UserPreferences.isNotificationsEnabled(context)) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(isDarkTheme) {
        nightMode = isDarkTheme
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            val result = withContext(Dispatchers.IO) {
                                ProfileRepository.logout(context)
                            }
                            isProcessing = false
                            showLogoutDialog = false
                            result.onSuccess {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_logout_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onLogout()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: context.getString(R.string.error_generic),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.logout_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(stringResource(R.string.logout_confirm_no))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                            MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsHeroCard(isDarkMode = nightMode, notificationsEnabled = notificationsEnabled)

                SettingsToggleCard(
                    nightMode = nightMode,
                    notificationsEnabled = notificationsEnabled,
                    onToggleNightMode = { isChecked ->
                        nightMode = isChecked
                        UserPreferences.setNightModeEnabled(context, isChecked)
                        AppCompatDelegate.setDefaultNightMode(
                            if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                        )
                        onToggleDarkTheme(isChecked)
                    },
                    onToggleNotifications = { isChecked ->
                        notificationsEnabled = isChecked
                        UserPreferences.setNotificationsEnabled(context, isChecked)
                    }
                )

                Text(
                    text = stringResource(R.string.settings_information_section),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                ExpandableInfoCard(
                    title = stringResource(R.string.settings_faq),
                    content = stringResource(R.string.settings_faq_content),
                    icon = Icons.Outlined.HelpOutline
                )
                ExpandableInfoCard(
                    title = stringResource(R.string.settings_contact),
                    content = stringResource(R.string.settings_contact_content),
                    icon = Icons.Outlined.MailOutline
                )
                ExpandableInfoCard(
                    title = stringResource(R.string.settings_about),
                    content = stringResource(R.string.settings_about_content),
                    icon = Icons.Outlined.Info
                )

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditProfile
                ) {
                    Text(stringResource(R.string.settings_edit_profile))
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLogoutDialog = true }
                ) {
                    Text(
                        text = stringResource(R.string.action_logout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroCard(
    isDarkMode: Boolean,
    notificationsEnabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_hero_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.settings_hero_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsHeroChip(
                        text = if (isDarkMode) stringResource(R.string.settings_chip_theme_dark) else stringResource(R.string.settings_chip_theme_light)
                    )
                    SettingsHeroChip(
                        text = if (notificationsEnabled) stringResource(R.string.settings_chip_notification_on) else stringResource(R.string.settings_chip_notification_off)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SettingsToggleCard(
    nightMode: Boolean,
    notificationsEnabled: Boolean,
    onToggleNightMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_night_mode),
                description = stringResource(R.string.settings_night_mode_hint),
                checked = nightMode,
                onCheckedChange = onToggleNightMode
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = stringResource(R.string.settings_notifications),
                description = stringResource(R.string.settings_notifications_hint),
                checked = notificationsEnabled,
                onCheckedChange = onToggleNotifications
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RowWithSwitch(
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowWithSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ExpandableInfoCard(
    title: String,
    content: String,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (expanded) 90f else 0f)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
