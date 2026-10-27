package com.mudeer.mvt
import androidx.activity.compose.setContent

import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MvtApp() }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun MvtApp() {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("No video selected") }
    var framing by remember { mutableStateOf("Fill 9:16") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a video to get started.") }
    var progress by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected video"
            status = "Video selected. Tap Create to make a random 16-30 second Short."
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Mudeer Video Transformer", style = MaterialTheme.typography.headlineSmall)
                Text("MVT • Long video → vertical YouTube Short", style = MaterialTheme.typography.bodyMedium)
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Select source video", style = MaterialTheme.typography.titleMedium)
                        Text(selectedName, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { picker.launch("video/*") }, enabled = !busy) {
                            Text("Choose video")
                        }
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Random Short", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Every export picks a new random start point and a random length between 16 and 30 seconds.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("3. Vertical framing", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { framing = "Fill 9:16" },
                                enabled = !busy
                            ) { Text(if (framing == "Fill 9:16") "✓ Fill" else "Fill") }
                            OutlinedButton(
                                onClick = { framing = "Fit whole video" },
                                enabled = !busy
                            ) { Text(if (framing == "Fit whole video") "✓ Fit" else "Fit") }
                        }
                        Text(
                            if (framing == "Fill 9:16")
                                "Fills a 9:16 canvas by cropping the sides when needed."
                            else
                                "Keeps the whole picture visible; empty space may appear.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Export", style = MaterialTheme.typography.titleMedium)
                        Text(status)
                        if (busy) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                Text("Exporting… $progress%")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val uri = selectedUri
                                    if (uri == null) {
                                        Toast.makeText(context, "Choose a video first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        exportVideo(
                                            context = context,
                                            sourceUri = uri,
                                            fill = framing == "Fill 9:16",
                                            onStarted = { plan -> busy = true; progress = 0; status = plan },
                                            onProgress = { progress = it },
                                            onFinished = { message -> busy = false; status = message },
                                            onError = { message -> busy = false; status = "Export failed: $message" }
                                        )
                                    }
                                },
                                enabled = !busy
                            ) { Text("Create vertical video") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Exports a random 16-30 second vertical 1080×1920 clip with the source audio. Best-moment detection and logo masking are coming next.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun fmt(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@androidx.annotation.OptIn(UnstableApi::class)
private fun exportVideo(
    context: android.content.Context,
    sourceUri: Uri,
    fill: Boolean,
    onStarted: (String) -> Unit,
    onProgress: (Int) -> Unit,
    onFinished: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Find the source length so the random clip stays inside the video.
        val retriever = MediaMetadataRetriever()
        val totalMs = try {
            retriever.setDataSource(context, sourceUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }

        // Random length 16-30 s, random start anywhere that fits.
        val wantedMs = Random.nextLong(16_000L, 30_001L)
        val clipMs = if (totalMs in 1 until wantedMs) totalMs else wantedMs
        val maxStartMs = (totalMs - clipMs).coerceAtLeast(0L)
        val startMs = if (maxStartMs > 0) Random.nextLong(0L, maxStartMs + 1) else 0L
        val endMs = startMs + clipMs
        val planText = "Clip ${fmt(startMs)} - ${fmt(endMs)} (${clipMs / 1000}s). Exporting…"
        onStarted(planText)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputName = "MVT_$stamp.mp4"
        val outputFile = java.io.File(context.getExternalFilesDir(null), outputName)

        val presentationMode = if (fill) {
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        } else {
            Presentation.LAYOUT_SCALE_TO_FIT
        }
        val presentation = Presentation.createForWidthAndHeight(1080, 1920, presentationMode)
        val effects = Effects(emptyList(), listOf(presentation))
        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        val editedItem = EditedMediaItem.Builder(mediaItem).setEffects(effects).build()

        val handler = Handler(Looper.getMainLooper())
        var finished = false

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    finished = true
                    onProgress(100)
                    // Copy to the public Movies collection so the result is visible to the user.
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, outputName)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MudeerVideoTransformer")
                        }
                        val resolver = context.contentResolver
                        val publicUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        if (publicUri != null) {
                            outputFile.inputStream().use { input ->
                                resolver.openOutputStream(publicUri)?.use { output -> input.copyTo(output) }
                            }
                            outputFile.delete()
                            onFinished("Done. Clip ${fmt(startMs)} - ${fmt(endMs)} saved to Movies/MudeerVideoTransformer/$outputName")
                        } else {
                            onFinished("Done at app storage: ${outputFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        onFinished("Done at app storage: ${outputFile.absolutePath}")
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    finished = true
                    outputFile.delete()
                    onError(exportException.message ?: "Unknown Media3 error")
                }
            })
            .build()

        transformer.start(editedItem, outputFile.absolutePath)

        // Live progress: poll Media3 twice a second until the export ends.
        val holder = ProgressHolder()
        val poller = object : Runnable {
            override fun run() {
                if (finished) return
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress)
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(poller)
    } catch (e: Exception) {
        onError(e.message ?: "Could not start export")
    }
}
