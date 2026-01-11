package edu.konditer.cameraapp.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import edu.konditer.cameraapp.ui.components.EnhancedCameraPreview
import edu.konditer.cameraapp.ui.theme.CameraAppTheme

@Composable
fun CameraScreen(
    previewView: PreviewView,
    onNavigateToVideo: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    onTapToFocus: (Float, Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
    getCurrentZoom: () -> Float,
    getZoomRange: () -> ClosedFloatingPointRange<Float>,
) {
    CameraAppTheme {
        var showFlash by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val flashAlpha by animateFloatAsState(
            targetValue = if (showFlash) 0.8f else 0f,
            animationSpec = tween(durationMillis = 150),
            label = "flash_animation"
        )
        
        fun triggerFlash() {
            showFlash = true
            coroutineScope.launch {
                delay(150)
                showFlash = false
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                EnhancedCameraPreview(
                    previewView = previewView,
                    getCurrentZoom = getCurrentZoom,
                    getZoomRange = getZoomRange,
                    onTapToFocus = onTapToFocus,
                    onZoomChanged = onZoomChanged,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (flashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .alpha(flashAlpha)
                            .background(Color.Black)
                    )
                }
            }
            
            Column (
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
                        onClick = {
                            triggerFlash()
                            onTakePhoto()
                        },
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
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
                                    Color.White,
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToVideo,
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video",
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

