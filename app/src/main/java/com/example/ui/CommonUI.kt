package com.example.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FullscreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    userRole: String?,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                offset += offsetChange
            }

            Image(
                painter = rememberAsyncImagePainter(imagePath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
                if (userRole == "BENDAHARA" || userRole == "ADMIN" || userRole == "DEVELOPER") {
                    IconButton(onClick = onDownload, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                        Icon(Icons.Default.Download, contentDescription = "Unduh", tint = Color.White)
                    }
                }
            }
        }
    }
}

fun formatRupiah(number: Double): String {
    val isNegative = number < 0
    val absoluteValue = kotlin.math.abs(number)
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val formatted = formatter.format(absoluteValue.toLong())
    return if (isNegative) "-Rp $formatted" else "Rp $formatted"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun parseDateToMillis(input: Any?): Long {
    if (input == null) return 0L
    if (input is Long) return input
    val str = input.toString()
    return try {
        str.toLong()
    } catch (e: Exception) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            sdf.parse(str)?.time ?: 0L
        } catch (e2: Exception) {
            0L
        }
    }
}


