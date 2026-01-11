package edu.konditer.cameraapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.konditer.cameraapp.ui.screens.GalleryScreen
import edu.konditer.cameraapp.ui.screens.MediaItem

class GalleryFragment : Fragment() {

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!findNavController().navigateUp()) {
                requireActivity().finish()
            }
        }
    }

    private fun onNavigateBack() {
        if (!findNavController().navigateUp()) {
            requireActivity().finish()
        }
    }
    
    private fun onNavigateToCamera() {
        findNavController().navigate(
            GalleryFragmentDirections.actionGalleryFragmentToCameraFragment()
        )
    }
    
    private fun onNavigateToVideo() {
        findNavController().navigate(
            GalleryFragmentDirections.actionGalleryFragmentToVideoFragment()
        )
    }
    
    private fun onDeleteItem(item: MediaItem) {
        try {
            val deleted = requireContext().contentResolver.delete(item.uri, null, null)
            if (deleted <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Не удалось удалить файл",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Нет разрешения на удаление файла",
                Toast.LENGTH_SHORT
                ).show()
        } catch (e: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
    
    private fun updateStatusBarForFullscreen(isFullscreen: Boolean) {
        val window = requireActivity().window
        val view = view ?: return
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController.let { controller ->
            if (isFullscreen) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = false
            } else {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isSystemInDarkTheme(requireContext())
            }
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        updateStatusBarForFullscreen(false)
    }
    
    private fun isSystemInDarkTheme(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                GalleryScreen(
                    onNavigateBack = { onNavigateBack() },
                    onNavigateToCamera = { onNavigateToCamera() },
                    onNavigateToVideo = { onNavigateToVideo() },
                    onDeleteItem = { item -> onDeleteItem(item) },
                    onUpdateStatusBar = { isFullscreen -> updateStatusBarForFullscreen(isFullscreen) }
                )
            }
        }
    }
}
