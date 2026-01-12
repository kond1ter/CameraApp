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

enum class CameraMode {
    PHOTO,
    VIDEO
}

class UnifiedCameraController(private val context: Context) {
    
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var recorder: Recorder? = null
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var currentMode = CameraMode.PHOTO
    private var currentZoomRatio = 1f
    
    fun setTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }
    
    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        mode: CameraMode = CameraMode.PHOTO
    ) {
        currentMode = mode
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindUseCases(previewView, lifecycleOwner, mode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun bindUseCases(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        mode: CameraMode
    ) {
        val provider = cameraProvider ?: return
        
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(1080, 1440),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
        
        preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        when (mode) {
            CameraMode.PHOTO -> {
                imageCapture = ImageCapture.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            }
            CameraMode.VIDEO -> {
                if (recorder == null) {
                    recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.fromOrderedList(
                            listOf(Quality.FHD, Quality.HD, Quality.SD),
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                        ))
                        .build()
                }
                
                if (videoCapture == null) {
                    videoCapture = VideoCapture.Builder(recorder!!)
                        .build()
                }
                
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            }
        }
        
        camera?.cameraControl?.setZoomRatio(currentZoomRatio)
    }
    
    fun takePhoto(onPhotoSaved: (String) -> Unit, onError: (Exception) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onError(Exception("Camera not initialized"))
            return
        }
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraApp")
            }
        }
        
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        
        val mainExecutor = ContextCompat.getMainExecutor(context)
        
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    mainExecutor.execute {
                        onPhotoSaved(savedUri.toString())
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    mainExecutor.execute {
                        onError(exception)
                    }
                }
            }
        )
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
            .asPersistentRecording()
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
        lifecycleOwner: LifecycleOwner
    ) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera(previewView, lifecycleOwner, currentMode)
    }
    
    fun focusOnPoint(x: Float, y: Float, previewView: PreviewView) {
        val cameraControl = camera?.cameraControl ?: return
        val meteringPointFactory = previewView.meteringPointFactory
        
        val point = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        cameraControl.startFocusAndMetering(action)
    }
    
    fun setZoomRatio(ratio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val cameraInfo = camera?.cameraInfo ?: return
        
        val zoomState = cameraInfo.zoomState.value
        if (zoomState == null) {
            currentZoomRatio = ratio
            try {
                cameraControl.setZoomRatio(ratio)
            } catch (e: Exception) {
            }
            return
        }
        
        val minZoom = zoomState.minZoomRatio
        val maxZoom = zoomState.maxZoomRatio
        val clampedRatio = ratio.coerceIn(minZoom, maxZoom)
        
        currentZoomRatio = clampedRatio
        cameraControl.setZoomRatio(clampedRatio)
    }
    
    fun getZoomRange(): ClosedFloatingPointRange<Float> {
        val cameraInfo = camera?.cameraInfo ?: return 1f..1f
        val zoomState = cameraInfo.zoomState.value ?: return 1f..1f
        return zoomState.minZoomRatio..zoomState.maxZoomRatio
    }
    
    fun getCurrentZoom(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: currentZoomRatio
    }
    
    fun switchMode(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        newMode: CameraMode
    ) {
        if (isRecording()) {
            stopRecording()
        }
        
        currentMode = newMode
        startCamera(previewView, lifecycleOwner, newMode)
    }
    
    fun cleanup() {
        recording?.stop()
        recording = null
        cameraExecutor.shutdown()
    }
}

