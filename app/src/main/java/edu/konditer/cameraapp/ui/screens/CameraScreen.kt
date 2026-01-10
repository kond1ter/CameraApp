package edu.konditer.cameraapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.konditer.cameraapp.ui.theme.CameraAppTheme

@Composable
fun CameraScreen(
    onNavigateToVideo: () -> Unit,
    onNavigateToGallery: () -> Unit,
) {
    CameraAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CameraFragment",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToVideo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Video")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToGallery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Gallery")
            }
        }
    }
}

