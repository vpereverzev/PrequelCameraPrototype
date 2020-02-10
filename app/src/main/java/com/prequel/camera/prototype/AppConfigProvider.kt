package com.prequel.camera.prototype

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig


/**
 * @brief Class needed for correct initialization of the CameraX from the version 1.0.0-alpha07
 *
 * @see https://developer.android.com/jetpack/androidx/releases/camera#camera-core-1.0.0-alpha07
 */
class AppConfigProvider : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}