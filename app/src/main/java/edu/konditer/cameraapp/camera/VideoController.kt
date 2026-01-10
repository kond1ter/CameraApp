package edu.konditer.cameraapp.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoController(private val context: Context) {
    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    
    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1080, 1920),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
                
                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.fromOrderedList(
                        listOf(Quality.FHD, Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    ))
                    .build()
                
                videoCapture = VideoCapture.Builder(recorder)
                    .build()
                
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun startRecording(
        onRecordingStarted: () -> Unit,
        onRecordingStopped: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val videoCapture = videoCapture ?: run {
            onError(Exception("Camera not initialized"))
            return
        }
        
        recording?.let {
            onError(Exception("Recording already in progress"))
            return
        }
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraApp")
            }
        }
        
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        
        val mainExecutor = ContextCompat.getMainExecutor(context)
        
        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        mainExecutor.execute {
                            onRecordingStarted()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            val uri = event.outputResults.outputUri
                            mainExecutor.execute {
                                onRecordingStopped(uri.toString())
                            }
                        } else {
                            mainExecutor.execute {
                                onError(Exception("Recording failed: ${event.error}"))
                            }
                        }
                        recording = null
                    }
                }
            }
    }
    
    fun stopRecording() {
        recording?.stop()
        recording = null
    }
    
    fun isRecording(): Boolean {
        return recording != null
    }
    
    fun switchCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        if (isRecording()) {
            stopRecording()
        }

        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera(previewView, lifecycleOwner)
    }
    
    fun cleanup() {
        recording?.stop()
        recording = null
        cameraExecutor.shutdown()
    }
}

