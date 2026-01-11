package edu.konditer.cameraapp.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
@Composable
fun EnhancedCameraPreview(
    previewView: PreviewView,
    getCurrentZoom: () -> Float,
    getZoomRange: () -> ClosedFloatingPointRange<Float>,
    onTapToFocus: ((Float, Float) -> Unit)? = null,
    onZoomChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Используем rememberUpdatedState для актуальных callback'ов
    val onZoomChangedState = rememberUpdatedState(onZoomChanged)
    val onTapToFocusState = rememberUpdatedState(onTapToFocus)
    val getCurrentZoomState = rememberUpdatedState(getCurrentZoom)
    val getZoomRangeState = rememberUpdatedState(getZoomRange)
    
    // Локальное состояние для отображения
    var displayZoom by remember { mutableStateOf(getCurrentZoom()) }
    
    // Состояние жестов
    val gestureState = remember {
        object {
            var isZooming = false
            var initialDistance = 0f
            var gestureStartZoom = 1f
            var tapDownX = 0f
            var tapDownY = 0f
        }
    }
    
    // Создаем touch listener
    val touchListener = remember(onZoomChangedState, onTapToFocusState, getCurrentZoomState, getZoomRangeState) {
        View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    gestureState.isZooming = false
                    gestureState.initialDistance = 0f
                    gestureState.tapDownX = event.x
                    gestureState.tapDownY = event.y
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        gestureState.isZooming = true
                        // Получаем актуальное значение зума прямо из контроллера
                        gestureState.gestureStartZoom = getCurrentZoomState.value()
                        gestureState.initialDistance = getDistance(event)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (gestureState.isZooming && event.pointerCount == 2) {
                        val currentDistance = getDistance(event)
                        if (gestureState.initialDistance > 0f) {
                            val scale = currentDistance / gestureState.initialDistance
                            val zoomRange = getZoomRangeState.value()
                            // Ограничиваем значение зума допустимым диапазоном
                            val newZoom = (gestureState.gestureStartZoom * scale).coerceIn(
                                zoomRange.start,
                                zoomRange.endInclusive
                            )
                            // Обновляем displayZoom при изменении зума
                            displayZoom = newZoom
                            onZoomChangedState.value?.invoke(newZoom)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 1) {
                        gestureState.isZooming = false
                        gestureState.initialDistance = 0f
                        // Обновляем displayZoom из контроллера после окончания жеста
                        displayZoom = getCurrentZoomState.value()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!gestureState.isZooming && event.pointerCount <= 1) {
                        onTapToFocusState.value?.invoke(gestureState.tapDownX, gestureState.tapDownY)
                    }
                    gestureState.isZooming = false
                    gestureState.initialDistance = 0f
                    // Обновляем displayZoom из контроллера после окончания жеста
                    displayZoom = getCurrentZoomState.value()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    gestureState.isZooming = false
                    gestureState.initialDistance = 0f
                    // Обновляем displayZoom из контроллера при отмене жеста
                    displayZoom = getCurrentZoomState.value()
                    true
                }
                else -> false
            }
        }
    }
    
    Box(modifier = modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
        AndroidView(
            factory = { 
                previewView.apply {
                    isClickable = false
                    isFocusable = false
                    setOnTouchListener(touchListener)
                    post {
                        setOnTouchListener(touchListener)
                    }
                }
            },
            update = { view ->
                view.isClickable = false
                view.isFocusable = false
                view.post {
                    view.setOnTouchListener(touchListener)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Индикатор зума
        if (abs(displayZoom - 1.0f) > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable {
                        onZoomChangedState.value?.invoke(1.0f)
                        displayZoom = 1.0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%.1fx".format(displayZoom),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getDistance(event: MotionEvent): Float {
    val dx = event.getX(0) - event.getX(1)
    val dy = event.getY(0) - event.getY(1)
    return sqrt(dx * dx + dy * dy)
}
