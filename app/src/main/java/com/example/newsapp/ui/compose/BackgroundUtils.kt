package com.example.newsapp.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.newsapp.R
import com.example.newsapp.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberBackgroundBitmap(context: Context): Bitmap? {
    val uriString = UserPreferences.getBackgroundUri(context)
    val state = produceState<Bitmap?>(initialValue = null, key1 = uriString) {
        value = loadBackgroundBitmap(context, uriString)
    }
    return state.value
}

suspend fun loadBackgroundBitmap(context: Context, uriString: String?): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (!uriString.isNullOrBlank()) {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { return@withContext it }
        }
    } catch (_: Exception) {
        // ignore and fallback
    }
    return@withContext try {
        context.resources.openRawResource(R.raw.imagebghome).use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }
}

fun imageAlphaForLevel(level: Int): Float = when (level.coerceIn(0, 2)) {
    0 -> 0.6f // low transparency (more visible)
    1 -> 0.4f // medium
    else -> 0.2f // high transparency (least visible)
}
