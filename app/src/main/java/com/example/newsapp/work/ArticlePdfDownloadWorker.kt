package com.example.newsapp.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.ContentValues
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.drawToBitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newsapp.R
import com.example.newsapp.model.NewsArticle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer

/**
 * Background worker to render an article into a PDF (sans bookmark/download buttons).
 * Runs as a foreground worker to show progress/complete notifications.
 */
class ArticlePdfDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_ARTICLE_ID = "articleId"
        private const val CHANNEL_ID = "article_pdf_download"
        private const val NOTIFICATION_ID = 4221
    }

    override suspend fun doWork(): Result {
        val articleId = inputData.getInt(KEY_ARTICLE_ID, -1)
        if (articleId == -1) return Result.failure()

        setForeground(createForegroundInfo(0, "Preparing download..."))

        val article = entryPoint().newsRepository().getArticleById(articleId)
            ?: return Result.failure(Data.Builder().putString("error", "Article not found").build())

        return try {
            setForeground(createForegroundInfo(40, "Generating PDF..."))
            val pdfUri = writePdf(article)
            setForeground(createForegroundInfo(90, "Almost done..."))
            showCompletedNotification(pdfUri)
            Result.success(Data.Builder().putString("filePath", pdfUri.toString()).build())
        } catch (e: Exception) {
            showErrorNotification(e.message ?: "Failed to create PDF")
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }

    private suspend fun renderArticleToBitmap(article: NewsArticle): Bitmap = withContext(Dispatchers.Main.immediate) {
        val metrics = appContext.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1080)

        val composeView = ComposeView(appContext)
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(Dispatchers.Main.immediate)
        composeView.setParentCompositionContext(recomposer)

        // Start recomposer loop so Compose can run off-window
        val recomposeJob = launch(Dispatchers.Main.immediate + frameClock) {
            recomposer.runRecomposeAndApplyChanges()
        }

        composeView.setContent {
            MaterialTheme {
                PdfArticleContent(article = article)
            }
        }

        // Kick the frame clock and wait for composition to settle
        frameClock.sendFrame(System.nanoTime())
        recomposer.awaitIdle()

        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)

        // If first measure produced zero (can happen before first frame), force a frame and re-measure
        if (composeView.measuredWidth <= 0 || composeView.measuredHeight <= 0) {
            frameClock.sendFrame(System.nanoTime())
            recomposer.awaitIdle()
            composeView.measure(widthSpec, heightSpec)
        }

        // As last resort, clamp to a minimum size to avoid zero-dimension bitmap crashes
        val fallbackHeight = (width * 1.2f).toInt().coerceAtLeast(800)
        val measuredWidth = composeView.measuredWidth.coerceAtLeast(width)
        val measuredHeight = composeView.measuredHeight.coerceAtLeast(fallbackHeight)

        heightSpec = View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, measuredWidth, measuredHeight)

        // One more frame after layout for good measure
        frameClock.sendFrame(System.nanoTime())

        // drawToBitmap may return hardware bitmap; convert to ARGB_8888
        val bitmap = composeView.drawToBitmap().copy(Bitmap.Config.ARGB_8888, false)
        recomposeJob.cancel()
        recomposer.close()
        bitmap
    }

    private suspend fun writePdf(article: NewsArticle): Uri {
        val pageWidth = 1200
        val pageHeight = (pageWidth * 1.414f).toInt() // A4 ratio
        val margin = 64f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 46f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 30f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
        }
        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 132, 255)
            textSize = 28f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var cursorY = margin

        fun ensureSpace(heightNeeded: Float) {
            if (cursorY + heightNeeded > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                cursorY = margin
            }
        }

        fun drawTextBlock(text: String, paint: TextPaint, spacing: Float = 12f) {
            if (text.isBlank()) return
            val layout = android.text.StaticLayout.Builder
                .obtain(text, 0, text.length, paint, contentWidth.toInt())
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
            ensureSpace(layout.height.toFloat())
            canvas.save()
            canvas.translate(margin, cursorY)
            layout.draw(canvas)
            canvas.restore()
            cursorY += layout.height + spacing
        }

        canvas.drawColor(Color.WHITE)

        // Tag
        drawTextBlock(article.tag.uppercase(Locale.getDefault()), tagPaint, 16f)
        // Title
        drawTextBlock(article.title, titlePaint, 20f)
        // Meta
        drawTextBlock("${article.publishedAt} • ${article.source}", metaPaint, 24f)

        // Hero image
        loadHeroBitmap(article.heroImageUrl)?.let { hero ->
            val targetWidth = contentWidth
            val scale = targetWidth / hero.width.toFloat()
            val targetHeight = (hero.height * scale).coerceAtMost(pageHeight - (margin * 2) - cursorY)
            ensureSpace(targetHeight + 20f)
            val destRect = android.graphics.RectF(
                margin,
                cursorY,
                margin + targetWidth,
                cursorY + targetHeight
            )
            canvas.drawBitmap(hero, null, destRect, null)
            cursorY += targetHeight + 24f
        }

        val paragraphs = article.content.takeIf { it.isNotEmpty() } ?: listOf(article.summary)
        paragraphs.forEach { paragraph ->
            drawTextBlock(paragraph, bodyPaint, 18f)
        }

        pdfDocument.finishPage(page)

        val dateStamp = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
        val safeTitle = sanitizeFileName(article.title.ifBlank { "article" })
        val fileName = "${safeTitle}_${dateStamp}.pdf"

        val uri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = appContext.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Failed to create MediaStore entry")

            resolver.openOutputStream(itemUri)?.use { out ->
                pdfDocument.writeTo(out)
            } ?: throw IllegalStateException("Failed to open MediaStore stream")

            // Mark as not pending so it appears to user
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)

            uri = itemUri
        } else {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDir.exists()) publicDir.mkdirs()
            val file = File(publicDir, fileName)
            file.outputStream().use { out ->
                pdfDocument.writeTo(out)
            }
            uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
        }

        pdfDocument.close()
        return uri
    }

    private suspend fun loadHeroBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val request = ImageRequest.Builder(appContext)
                .data(url)
                .allowHardware(false)
                .build()
            val result = appContext.imageLoader.execute(request)
            val drawable = (result as? coil.request.SuccessResult)?.drawable
            drawable?.toBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        createChannelIfNeeded()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading PDF")
            .setContentText(message)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletedNotification(fileUri: Uri) {
        createChannelIfNeeded()
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            data = fileUri
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            // Ensure URI perms flow through to chooser/viewer
            clipData = android.content.ClipData.newRawUri("pdf", fileUri)
        }

        // Explicitly grant URI permission to all handlers (including chooser)
        val resolved = appContext.packageManager.queryIntentActivities(openIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        resolved.forEach { info ->
            appContext.grantUriPermission(
                info.activityInfo.packageName,
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        // Also grant to common PDF viewers (Drive)
        listOf("com.google.android.apps.docs").forEach { pkg ->
            try {
                appContext.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText("PDF saved to Downloads. Open via Files or Drive.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "PDF saved in the Downloads folder. Open it via your file manager or Drive."
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID + 1, notification)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, appContext.getString(R.string.toast_download_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorNotification(message: String) {
        createChannelIfNeeded()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID + 2, notification)
    }

    private fun createChannelIfNeeded() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Article PDF",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Article PDF download notifications"
        }
        manager.createNotificationChannel(channel)
    }

    private fun sanitizeFileName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9_\\- ]"), "")
            .trim()
            .replace(" ", "_")
            .take(60)

    private fun entryPoint(): WorkerEntryPoint =
        EntryPointAccessors.fromApplication(appContext, WorkerEntryPoint::class.java)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun newsRepository(): com.example.newsapp.data.NewsRepository
}

@Composable
private fun PdfArticleContent(article: NewsArticle) {
    val context = LocalContext.current
    val dateLabel = remember(article) { article.publishedAt.ifBlank { "" } }

    Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.heroImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = article.tag.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dateLabel • ${article.source}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val paragraphs = article.content.takeIf { it.isNotEmpty() } ?: listOf(article.summary)
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
