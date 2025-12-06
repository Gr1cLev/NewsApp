package com.example.newsapp.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newsapp.R
import com.example.newsapp.data.NewsRepository
import com.example.newsapp.data.firebase.UserInteractionRepository
import com.example.newsapp.di.FirebaseEntryPoint
import com.example.newsapp.model.NewsArticle
import com.example.newsapp.work.ArticlePdfDownloadWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: Int,
    newsRepository: NewsRepository,
    onBack: () -> Unit,
    onBookmarkChanged: () -> Unit
) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val workManager = remember(context) { WorkManager.getInstance(context) }

    // Get UserInteractionRepository from Hilt for tracking
    val userInteractionRepository = remember {
        val appContext = context.applicationContext
        EntryPointAccessors.fromApplication(
            appContext,
            FirebaseEntryPoint::class.java
        ).userInteractionRepository()
    }

    val article by produceState<NewsArticle?>(initialValue = null, articleId) {
        value = withContext(Dispatchers.IO) { newsRepository.getArticleById(articleId) }
    }

    // Track reading time
    var readingStartTime by remember { mutableStateOf(0L) }
    
    // Track article click when article is loaded
    LaunchedEffect(article) {
        article?.let { currentArticle ->
            // Start reading time tracking
            readingStartTime = System.currentTimeMillis()
            
            // Check bookmark status
            isBookmarked = withContext(Dispatchers.IO) {
                newsRepository.isArticleBookmarked(currentArticle.id)
            }
            
            // Track article click to Firebase
            withContext(Dispatchers.IO) {
                try {
                    userInteractionRepository.trackArticleClick(
                        articleId = currentArticle.id.toString(),
                        title = currentArticle.title,
                        category = currentArticle.category,
                        source = currentArticle.source
                    )
                    android.util.Log.d("ArticleDetail", "Article click tracked: ${currentArticle.title}")
                } catch (e: Exception) {
                    android.util.Log.e("ArticleDetail", "Failed to track article click: ${e.message}")
                }
            }
        }
    }
    
    // Track reading time when leaving the screen
    androidx.compose.runtime.DisposableEffect(article) {
        onDispose {
            article?.let { currentArticle ->
                if (readingStartTime > 0) {
                    val readingDuration = (System.currentTimeMillis() - readingStartTime) / 1000
                    if (readingDuration > 2) { // Only track if user spent more than 2 seconds
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                userInteractionRepository.trackReadingTime(
                                    articleId = currentArticle.id.toString(),
                                    durationSeconds = readingDuration
                                )
                                android.util.Log.d("ArticleDetail", "Reading time tracked: ${readingDuration}s for ${currentArticle.title}")
                            } catch (e: Exception) {
                                android.util.Log.e("ArticleDetail", "Failed to track reading time: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (article == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Article not found.",
                style = MaterialTheme.typography.bodyLarge
            )
            FilledTonalButton(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onBack
            ) {
                Text("Go Back")
            }
        }
        return
    }

    val currentArticle = article!!
    val detailSurfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
    val enqueueDownload = remember(currentArticle, workManager, context) {
        {
            val request = OneTimeWorkRequestBuilder<ArticlePdfDownloadWorker>()
                .setInputData(
                    workDataOf(ArticlePdfDownloadWorker.KEY_ARTICLE_ID to currentArticle.id)
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueueUniqueWork(
                "download-article-${currentArticle.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Toast.makeText(
                context,
                context.getString(R.string.toast_download_starting),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    fun needsLegacyStoragePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enqueueDownload()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.toast_download_storage_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (needsLegacyStoragePermission()) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                enqueueDownload()
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.toast_notification_permission_needed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val startDownloadWithPermissions = remember(currentArticle) {
        {
            when {
                needsNotificationPermission() ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                needsLegacyStoragePermission() ->
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else -> enqueueDownload()
            }
        }
    }

    DetailBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            HeroHeader(article = currentArticle)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                color = detailSurfaceColor,
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = currentArticle.tag.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentArticle.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${currentArticle.publishedAt} • ${currentArticle.source}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AiInsightSection(article = currentArticle)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val paragraphs = currentArticle.content.takeIf { it.isNotEmpty() }
                            ?: listOf(currentArticle.summary)
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    ActionRow(
                        isBookmarked = isBookmarked,
                        onBookmarkToggle = {
                            coroutineScope.launch {
                                val bookmarked = withContext(Dispatchers.IO) {
                                    newsRepository.toggleBookmark(currentArticle.id)
                                }
                                isBookmarked = bookmarked
                                onBookmarkChanged()
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        if (bookmarked) R.string.bookmark_added else R.string.bookmark_removed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDownload = { startDownloadWithPermissions() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(article: NewsArticle) {
    val accentColor = Color(article.accentColor)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(article.heroImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = article.title,
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f)),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiInsightSection(article: NewsArticle) {
    val labels = remember(article) { generateAiTags(article) }
    if (labels.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.article_ai_section_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { label ->
                AssistChip(
                    onClick = {},
                    label = { Text(label) },
                    shape = RoundedCornerShape(18.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onDownload: () -> Unit
) {
    val bookmarkLabel = stringResource(
        if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onBookmarkToggle,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = bookmarkLabel,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = bookmarkLabel,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        FilledTonalButton(
            onClick = onDownload,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_download_news))
        }
    }
}

private fun generateAiTags(article: NewsArticle): List<String> {
    val tags = mutableListOf<String>()
    tags += "Sedang tren"
    when (article.category.lowercase(Locale.getDefault())) {
        "sports", "sport" -> {
            tags += listOf("Favorit fans", "Analisis laga")
        }
        "business" -> {
            tags += listOf("Briefing pagi", "Radar pasar")
        }
        "technology", "tech" -> {
            tags += listOf("Innovation pulse", "Feature spotlight")
        }
        else -> tags += "Special highlight"
    }
    if (article.summary.contains("analysis", ignoreCase = true) ||
        article.summary.contains("analisis", ignoreCase = true)
    ) {
        tags += "Ulasan mendalam"
    }
    return tags.distinct().take(5)
}

@Composable
private fun DetailBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val backdrop = remember {
        try {
            context.resources.openRawResource(R.raw.imagebghome).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (error: Exception) {
            null
        }
    }
    val baseColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        if (backdrop != null) {
            Image(
                bitmap = backdrop.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.16f
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.92f),
                            baseColor.copy(alpha = 0.86f),
                            baseColor.copy(alpha = 0.94f)
                        )
                    )
                )
        )
        content()
    }
}
