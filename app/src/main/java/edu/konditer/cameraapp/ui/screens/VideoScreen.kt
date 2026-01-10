package edu.konditer.cameraapp.ui.screens

import android.annotation.SuppressLint
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.konditer.cameraapp.ui.components.CameraPreview
import edu.konditer.cameraapp.ui.theme.CameraAppTheme
import java.util.concurrent.TimeUnit

@Composable
fun VideoScreen(
    previewView: PreviewView,
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onToggleRecording: () -> Unit,
    onSwitchCamera: () -> Unit,
    isRecording: Boolean,
    recordingDuration: Long,
) {
    CameraAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CameraPreview(
                    previewView = previewView,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Red, shape = CircleShape)
                        )
                        Text(
                            text = formatDuration(recordingDuration),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNavigateToGallery,
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Button(
                        onClick = onToggleRecording,
                        modifier = Modifier.size(96.dp),
                        shape = if (isRecording) RoundedCornerShape(16.dp) else CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color.Red,
                                    shape = if (isRecording) RoundedCornerShape(8.dp) else CircleShape
                                )
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatDuration(milliseconds: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
