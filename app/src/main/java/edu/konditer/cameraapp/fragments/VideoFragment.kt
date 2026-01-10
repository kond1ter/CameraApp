package edu.konditer.cameraapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
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
import edu.konditer.cameraapp.camera.VideoController
import edu.konditer.cameraapp.ui.screens.PermissionsScreen
import edu.konditer.cameraapp.ui.screens.VideoScreen

class VideoFragment : Fragment() {

    private var videoController: VideoController? = null
    private var previewView: PreviewView? = null
    
    private var hasCameraPermission by mutableStateOf(false)
    private var hasStoragePermission by mutableStateOf(false)
    private var hasAudioPermission by mutableStateOf(false)
    private var isRecording by mutableStateOf(false)
    private var recordingDuration by mutableStateOf(0L)

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
        
        if (hasCameraPermission && hasStoragePermission && previewView != null) {
            initializeCamera()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isRecording) {
                stopRecording()
            } else {
                requireActivity().finish()
            }
        }
    }

    private fun onNavigateToCamera() {
        if (isRecording) {
            stopRecording()
        }
        findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToCameraFragment()
        )
    }

    private fun onNavigateToGallery() {
        if (isRecording) {
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
            videoController = VideoController(requireContext())
            videoController?.startCamera(
                previewView = view,
                lifecycleOwner = viewLifecycleOwner,
            )
        }
    }
    
    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    private fun startRecording() {
        videoController?.startRecording(
            onRecordingStarted = {
                isRecording = true
                recordingDuration = 0L
            },
            onRecordingStopped = { uri ->
                isRecording = false
                recordingDuration = 0L
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
        videoController?.stopRecording()
        isRecording = false
        recordingDuration = 0L
    }
    
    private fun onSwitchCamera(preview: PreviewView) {
        preview.let { view ->
            videoController?.switchCamera(
                previewView = view,
                lifecycleOwner = viewLifecycleOwner,
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
        if (isRecording) {
            stopRecording()
        }
        videoController?.cleanup()
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
        
        if (hasCameraPermission && previewView != null) {
            initializeCamera()
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
            initializeCamera()
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
                        while (isRecording) {
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
                        isRecording = isRecording,
                        recordingDuration = recordingDuration
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
