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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.konditer.cameraapp.camera.CameraController
import edu.konditer.cameraapp.ui.screens.CameraScreen
import edu.konditer.cameraapp.ui.screens.PermissionsScreen

class CameraFragment : Fragment() {

    private var cameraController: CameraController? = null
    private var previewView: PreviewView? = null
    
    private var hasCameraPermission by mutableStateOf(false)
    private var hasStoragePermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        }
        
        if (hasCameraPermission && hasStoragePermission && previewView != null) {
            initializeCamera()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().finish()
        }
    }

    private fun onNavigateToVideo() {
        findNavController().navigate(
            CameraFragmentDirections.actionCameraFragmentToVideoFragment()
        )
    }

    private fun onNavigateToGallery() {
        findNavController().navigate(
            CameraFragmentDirections.actionCameraFragmentToGalleryFragment()
        )
    }
    
    private fun checkPermissions(requestIfMissing: Boolean = true) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        if (requestIfMissing && (!hasCameraPermission || !hasStoragePermission)) {
            val permissionsToRequest = mutableListOf<String>()
            if (!hasCameraPermission) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (!hasStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
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
        
        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missingPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
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
            cameraController = CameraController(requireContext())
            cameraController?.startCamera(
                previewView = view,
                lifecycleOwner = viewLifecycleOwner,
            )
        }
    }

    private fun onTakePhoto() {
        cameraController?.takePhoto(
            onPhotoSaved = { },
            onError = { exception ->
                Toast.makeText(
                    requireContext(),
                    "Ошибка: не удалось сохранить фото",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun onSwitchCamera(preview: PreviewView) {
        preview.let { view ->
            cameraController?.switchCamera(
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
        cameraController?.cleanup()
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
        
        checkPermissions(requestIfMissing = false)
        
        // Если разрешения были предоставлены после возврата из настроек, инициализируем камеру
        val permissionsWereGranted = (!previousCameraPermission && hasCameraPermission) ||
                                     (!previousStoragePermission && hasStoragePermission)
        
        if (permissionsWereGranted && 
            hasCameraPermission && 
            hasStoragePermission && 
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
                
                if (hasCameraPermission && hasStoragePermission && preview != null) {
                    CameraScreen(
                        previewView = preview,
                        onNavigateToVideo = { onNavigateToVideo() },
                        onNavigateToGallery = { onNavigateToGallery() },
                        onTakePhoto = { onTakePhoto() },
                        onSwitchCamera = { onSwitchCamera(preview) }
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

