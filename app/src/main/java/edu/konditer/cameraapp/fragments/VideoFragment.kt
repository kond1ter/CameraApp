package edu.konditer.cameraapp.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.konditer.cameraapp.MainActivity
import edu.konditer.cameraapp.camera.CameraMode
import edu.konditer.cameraapp.camera.UnifiedCameraController
import edu.konditer.cameraapp.ui.screens.PermissionsScreen
import edu.konditer.cameraapp.ui.screens.VideoScreen

class VideoFragment : Fragment() {

    private val cameraController: UnifiedCameraController?
        get() = (requireActivity() as? MainActivity)?.cameraController
    
    private var previewView: PreviewView? = null
    
    private var hasCameraPermission by mutableStateOf(false)
    private var hasStoragePermission by mutableStateOf(false)
    private var hasAudioPermission by mutableStateOf(false)
    private var isRecording by mutableStateOf(false)
    private var recordingDuration by mutableStateOf(0L)
    
    private var orientationEventListener: OrientationEventListener? = null
    private var currentOrientation = Surface.ROTATION_0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        } else {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        }
        
        if (hasCameraPermission && hasStoragePermission && hasAudioPermission && previewView != null) {
            initializeCamera()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (cameraController?.isRecording() == true) {
                stopRecording()
            } else {
                requireActivity().finish()
            }
        }
    }

    private fun onNavigateToCamera() {
        if (cameraController?.isRecording() == true) {
            stopRecording()
        }
        cameraController?.switchMode(
            previewView ?: return,
            requireActivity(),
            CameraMode.PHOTO
        )
        findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToCameraFragment()
        )
    }

    private fun onNavigateToGallery() {
        if (cameraController?.isRecording() == true) {
            stopRecording()
        }
        findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToGalleryFragment()
        )
    }
    
    private fun checkPermissions(requestIfMissing: Boolean = true) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        hasAudioPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        if (requestIfMissing && (!hasCameraPermission || !hasStoragePermission || !hasAudioPermission)) {
            val permissionsToRequest = mutableListOf<String>()
            if (!hasCameraPermission) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (!hasAudioPermission) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!hasStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun getMissingPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasCameraPermission) {
            missingPermissions.add(Manifest.permission.CAMERA)
        }
        if (!hasAudioPermission) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missingPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        return missingPermissions
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }
    
    private fun initializeCamera() {
        previewView?.let { view ->
            cameraController?.switchMode(
                previewView = view,
                lifecycleOwner = requireActivity(),
                newMode = CameraMode.VIDEO
            )
            setupOrientationListener()
            updateOrientation()
        }
    }
    
    private fun getRotationFromOrientation(orientation: Int): Int {
        return when {
            orientation == OrientationEventListener.ORIENTATION_UNKNOWN -> Surface.ROTATION_0
            orientation >= 45 && orientation < 135 -> Surface.ROTATION_270 // Landscape reversed
            orientation >= 135 && orientation < 225 -> Surface.ROTATION_180 // Portrait reversed
            orientation >= 225 && orientation < 315 -> Surface.ROTATION_90 // Landscape
            else -> Surface.ROTATION_0 // Portrait
        }
    }
    
    private fun updateOrientation() {
        val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val rotation = display?.rotation ?: Surface.ROTATION_0
        if (rotation != currentOrientation) {
            currentOrientation = rotation
            cameraController?.setTargetRotation(rotation)
        }
    }
    
    private fun setupOrientationListener() {
        orientationEventListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                val newRotation = getRotationFromOrientation(orientation)
                
                if (newRotation != currentOrientation) {
                    currentOrientation = newRotation
                    cameraController?.setTargetRotation(newRotation)
                }
            }
        }
        orientationEventListener?.enable()
    }
    
    private fun toggleRecording() {
        if (cameraController?.isRecording() == true) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    private fun startRecording() {
        cameraController?.startRecording(
            onRecordingStarted = {
                isRecording = true
                recordingDuration = 0L
            },
            onRecordingStopped = { uri ->
                isRecording = false
                recordingDuration = 0L
                Toast.makeText(
                    requireContext(),
                    "Видео сохранено",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { exception ->
                isRecording = false
                recordingDuration = 0L
                Toast.makeText(
                    requireContext(),
                    "Ошибка: не удалось сохранить видео",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    private fun stopRecording() {
        cameraController?.stopRecording()
        isRecording = false
        recordingDuration = 0L
    }
    
    private fun onSwitchCamera(preview: PreviewView) {
        preview.let { view ->
            cameraController?.switchCamera(
                previewView = view,
                lifecycleOwner = viewLifecycleOwner
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        checkPermissions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (cameraController?.isRecording() == true) {
            stopRecording()
        }
        orientationEventListener?.disable()
        orientationEventListener = null
        previewView = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController.let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        if (hasCameraPermission && hasStoragePermission && hasAudioPermission && previewView != null) {
            // При возврате на экран, переключаем камеру в режим видео
            cameraController?.switchMode(
                previewView = previewView!!,
                lifecycleOwner = requireActivity(),
                newMode = CameraMode.VIDEO
            ) ?: initializeCamera()
            setupOrientationListener()
            updateOrientation()
        }
    }
    
    override fun onResume() {
        super.onResume()
        val previousCameraPermission = hasCameraPermission
        val previousStoragePermission = hasStoragePermission
        val previousAudioPermission = hasAudioPermission
        
        checkPermissions(requestIfMissing = false)
        
        val permissionsWereGranted = (!previousCameraPermission && hasCameraPermission) ||
                                     (!previousStoragePermission && hasStoragePermission) ||
                                     (!previousAudioPermission && hasAudioPermission)
        
        if (permissionsWereGranted && 
            hasCameraPermission && 
            hasStoragePermission && 
            hasAudioPermission && 
            previewView != null) {
            cameraController?.switchMode(
                previewView = previewView!!,
                lifecycleOwner = requireActivity(),
                newMode = CameraMode.VIDEO
            ) ?: initializeCamera()
        } else if (hasCameraPermission && hasStoragePermission && hasAudioPermission && previewView != null) {
            // Если разрешения уже были, просто убеждаемся, что камера в правильном режиме
            cameraController?.switchMode(
                previewView = previewView!!,
                lifecycleOwner = requireActivity(),
                newMode = CameraMode.VIDEO
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        previewView = PreviewView(requireContext())
        
        return ComposeView(requireContext()).apply {
            setContent {
                val preview = previewView
                val missingPermissions = getMissingPermissions()
                
                LaunchedEffect(key1 = isRecording) {
                    if (isRecording) {
                        recordingDuration = 0L
                        while (cameraController?.isRecording() == true) {
                            delay(1000)
                            recordingDuration += 1000
                        }
                    } else {
                        recordingDuration = 0L
                    }
                }
                
                if (hasCameraPermission && hasStoragePermission && hasAudioPermission && preview != null) {
                    VideoScreen(
                        previewView = preview,
                        onNavigateToCamera = { onNavigateToCamera() },
                        onNavigateToGallery = { onNavigateToGallery() },
                        onToggleRecording = { toggleRecording() },
                        onSwitchCamera = { onSwitchCamera(preview) },
                        isRecording = cameraController?.isRecording() ?: false,
                        recordingDuration = recordingDuration,
                        onTapToFocus = { x, y ->
                            cameraController?.focusOnPoint(x, y, preview)
                        },
                        onZoomChanged = { zoomRatio ->
                            cameraController?.setZoomRatio(zoomRatio)
                        },
                        getCurrentZoom = { cameraController?.getCurrentZoom() ?: 1f },
                        getZoomRange = { cameraController?.getZoomRange() ?: 1f..10f }
                    )
                } else if (missingPermissions.isNotEmpty()) {
                    PermissionsScreen(
                        missingPermissions = missingPermissions,
                        onOpenSettings = { openAppSettings() }
                    )
                } else {
                    Box { }
                }
            }
        }
    }
}
