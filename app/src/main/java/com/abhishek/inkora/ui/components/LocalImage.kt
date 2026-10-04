package com.abhishek.inkora.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downsampled local image with zero image-loading dependencies.
 * Thumbnails cap at [maxDim] px; full view caps at 2048 px.
 * Missing/unreadable files render a placeholder — never a crash.
 */
suspend fun loadDownsampled(file: File, maxDim: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun LocalImageThumb(file: File?, modifier: Modifier = Modifier) {
    val bitmap = produceState<ImageBitmap?>(null, file) {
        value = if (file?.exists() == true) loadDownsampled(file, 512) else null
    }.value
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Attached image", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.BrokenImage, contentDescription = "Image unavailable", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun LocalImageFull(file: File?, modifier: Modifier = Modifier) {
    val bitmap = produceState<ImageBitmap?>(null, file) {
        value = if (file?.exists() == true) loadDownsampled(file, 2048) else null
    }.value
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Attached image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.BrokenImage, contentDescription = "Image unavailable", modifier = Modifier.size(48.dp))
        }
    }
}
