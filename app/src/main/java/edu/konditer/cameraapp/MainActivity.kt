package edu.konditer.cameraapp

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import edu.konditer.cameraapp.activities.BaseActivity
import edu.konditer.cameraapp.camera.UnifiedCameraController

class MainActivity : BaseActivity() {
    
    val cameraController by lazy { UnifiedCameraController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(
            FragmentContainerView(this).apply {
                id = R.id.nav_host_fragment
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        )

        if (savedInstanceState == null) {
            val navHost = NavHostFragment.Companion.create(R.navigation.navigation_graph)
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, navHost)
                .setPrimaryNavigationFragment(navHost)
                .commit()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraController.cleanup()
    }
}