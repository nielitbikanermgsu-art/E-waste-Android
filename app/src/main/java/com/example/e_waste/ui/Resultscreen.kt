package com.example.e_waste.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter

@Composable
fun ResultScreen(
    photoUri: String,
    onConfirmHandover: (sessionId: String) -> Unit,
    onRetry: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Kick off the upload once, when the screen first appears — not on every recomposition.
    LaunchedEffect(photoUri) {
        viewModel.detect(photoUri)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Detection result") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is DetectionUiState.Idle,
                is DetectionUiState.Uploading -> UploadingContent(photoUri)

                is DetectionUiState.Success -> SuccessContent(
                    photoUri = photoUri,
                    state = s,
                    onConfirmHandover = { onConfirmHandover(s.sessionId) }
                )

                is DetectionUiState.Error -> ErrorContent(
                    message = s.message,
                    retryable = s.retryable,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun UploadingContent(photoUri: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(photoUri),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text("Identifying materials…")
    }
}

@Composable
private fun SuccessContent(
    photoUri: String,
    state: DetectionUiState.Success,
    onConfirmHandover: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Photo with bounding boxes drawn on top. Boxes arrive in normalized 0..1 coordinates
        // already mapped to the ORIGINAL image — this screen just scales by the rendered
        // image size, it never touches the model's 640x640 inference space.
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Canvas(Modifier.fillMaxSize()) {
                state.detections.forEach { detection ->
                    val topLeft = Offset(
                        x = detection.xMinNorm * size.width,
                        y = detection.yMinNorm * size.height
                    )
                    val boxSize = Size(
                        width = (detection.xMaxNorm - detection.xMinNorm) * size.width,
                        height = (detection.yMaxNorm - detection.yMinNorm) * size.height
                    )
                    drawRect(
                        color = Color.Green,
                        topLeft = topLeft,
                        size = boxSize,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Detected items", style = MaterialTheme.typography.titleMedium) }
            items(state.detections) { detection ->
                Text("• ${detection.categoryName} (${(detection.confidence * 100).toInt()}%)")
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Recoverable materials", style = MaterialTheme.typography.titleMedium)
            }
            items(state.materials.filter { it.pricingTier == "primary" }) { material ->
                Text("• ${material.name}")
            }
            val traceMaterials = state.materials.filter { it.pricingTier == "trace" }
            if (traceMaterials.isNotEmpty()) {
                item {
                    Text(
                        "Also contains trace amounts: " +
                                traceMaterials.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Nearby authorised recyclers", style = MaterialTheme.typography.titleMedium)
            }
            items(state.recyclers) { recycler ->
                Text("• ${recycler.name} — ${recycler.distanceKm} km")
            }
        }

        Button(
            onClick = onConfirmHandover,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Confirm handover")
        }
    }
}

@Composable
private fun ErrorContent(message: String, retryable: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (retryable) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}