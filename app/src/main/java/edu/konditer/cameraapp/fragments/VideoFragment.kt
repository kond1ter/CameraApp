package edu.konditer.cameraapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.konditer.cameraapp.ui.screens.VideoScreen

class VideoFragment : Fragment() {

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().finish()
        }
    }

    private fun onNavigateToCamera() {
        findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToCameraFragment()
        )
    }

    private fun onNavigateToGallery() {
        findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToGalleryFragment()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VideoScreen(
                    onNavigateToCamera = { onNavigateToCamera() },
                    onNavigateToGallery = { onNavigateToGallery() }
                )
            }
        }
    }
}
