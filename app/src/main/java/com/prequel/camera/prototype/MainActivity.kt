package com.prequel.camera.prototype

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootContainer = findViewById(R.id.fragment_container)
    }

    override fun onResume() {
        super.onResume()
        // We must wait a bit to let UI settle before setting full screen flags, otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        rootContainer.postDelayed({
            rootContainer.systemUiVisibility = FULLSCREEN_FLAGS
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    companion object {

        /** @brief Combination of all flags required to put activity into immersive mode */
        private const val FULLSCREEN_FLAGS =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }
}
