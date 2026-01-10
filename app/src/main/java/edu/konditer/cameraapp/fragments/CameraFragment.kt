package edu.konditer.cameraapp.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.konditer.cameraapp.ui.screens.CameraScreen

class CameraFragment : Fragment() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController?.let { controller ->
            // Скрываем status bar (но он появляется при свайпе)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            // Настраиваем navigation bar: черный фон, белые элементы
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.BLACK
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.isAppearanceLightStatusBars = false // Темные иконки status bar (белые)
                controller.isAppearanceLightNavigationBars = false // Белые элементы navigation bar
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CameraScreen(
                    onNavigateToVideo = { onNavigateToVideo() },
                    onNavigateToGallery = { onNavigateToGallery() },
                    onTakePhoto = { /* TODO: реализовать позже */ }
                )
            }
        }
    }
}

