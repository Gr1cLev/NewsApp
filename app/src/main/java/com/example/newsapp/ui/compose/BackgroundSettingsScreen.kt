package com.example.newsapp.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.newsapp.R
import com.example.newsapp.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsScreen(
    onBack: () -> Unit
){
    val context = LocalContext.current
    var currentUri by remember { mutableStateOf(UserPreferences.getBackgroundUri(context)) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var alphaLevel by remember { mutableStateOf(UserPreferences.getBackgroundAlphaLevel(context)) }
    val scrollState = rememberScrollState()

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingUri = uri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Izin galeri dibutuhkan untuk mengganti background", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPicker() {
        val needsPermission = when {
            android.os.Build.VERSION.SDK_INT >= 33 ->
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ->
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermission) {
            val permission = if (android.os.Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            permissionLauncher.launch(permission)
        } else {
            pickImageLauncher.launch("image/*")
        }
    }

    suspend fun persistUri(uri: Uri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Background Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Background picture", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Pick a photo from gallery to replace the default background.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { launchPicker() }) {
                        Icon(imageVector = Icons.Filled.Image, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Change background picture")
                    }
                    if (pendingUri != null) {
                        Text("Preview", fontWeight = FontWeight.SemiBold)
                        AsyncImage(
                            model = pendingUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    currentUri = pendingUri.toString()
                                    UserPreferences.setBackgroundUri(context, currentUri)
                                    pendingUri = null
                                    Toast.makeText(context, "Background updated", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Use this")
                            }
                            TextButton(onClick = {
                                pendingUri = null
                                launchPicker()
                            }) {
                                Text("Pick another")
                            }
                        }
                    } else {
                        Text("Current background", fontWeight = FontWeight.SemiBold)
                        val current = currentUri
                        val painter = if (current != null) {
                            rememberAsyncImagePainter(model = current)
                        } else {
                            painterResource(R.raw.imagebghome)
                        }
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    TextButton(
                        onClick = {
                            currentUri = null
                            pendingUri = null
                            UserPreferences.setBackgroundUri(context, null)
                            Toast.makeText(context, "Reset to default background", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to default")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Transparency level", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Choose how faint the background should appear.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val options = listOf(
                        "Low" to 0,
                        "Medium" to 1,
                        "High" to 2
                    )
                    options.forEach { (label, level) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = alphaLevel == level,
                                onClick = {
                                    alphaLevel = level
                                    UserPreferences.setBackgroundAlphaLevel(context, level)
                                }
                            )
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
