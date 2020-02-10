package com.prequel.camera.prototype.models

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.prequel.camera.prototype.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


/**
 * @brief The class is the convenient manager for the handling all internal CameraX stuff
 *        like use-cases, bindings, saving pictures, etc.
 */
class CameraManager(ctx: Context) {

    var currentCameraLensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    private var mContext: Context = ctx
    private var mCamera: Camera? = null
    private var mCameraControl: CameraControl? = null
    private var mCameraInfo: CameraInfo? = null
    private var mPreview: Preview? = null
    private var mImageCapture: ImageCapture? = null

    val outputDirectory: File = getOutputDirectory(mContext)

    fun isImageStorageEmpty(): Boolean =
        outputDirectory.listFiles()?.isNotEmpty() == false

    /** @brief Declare and bind preview, capture use cases */
    fun bindCamera(lifecycleOwner: LifecycleOwner,
                   cameraLensFacing: Int,
                   aspectRatio: Int,
                   rotation: Int,
                   previewSurfaceProvider: Preview.PreviewSurfaceProvider) {

        currentCameraLensFacing = cameraLensFacing

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(currentCameraLensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)
        cameraProviderFuture.addListener(Runnable {

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            mPreview = Preview.Builder()
                // We request aspect ratio but no resolution to match preview config
                // CameraX will optimize for whatever specific resolution best fits requested capture mode
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build()

            mPreview?.previewSurfaceProvider = previewSurfaceProvider

            mImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config
                // CameraX will optimize for whatever specific resolution best fits requested capture mode
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                mCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, mPreview, mImageCapture
                )

                mCameraControl = mCamera?.cameraControl
                mCameraInfo = mCamera?.cameraInfo

            } catch(exc: Exception) {
                Log.e(TAG, "Camera use cases binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(mContext))
    }

    fun adjustZoom(scaleFactor: Float) {
        val currentZoomRatio: Float = mCameraInfo?.zoomRatio?.value ?: 0F

        val newZoomRatio = currentZoomRatio * scaleFactor

        mCameraControl?.setZoomRatio(min(newZoomRatio, ZOOM_RATIO_UPPER_LIMIT))
    }

    /**
     * @brief Perform image capturing operation and save the result to the separate file
     *
     * @see [PHOTO_EXTENSION], [FILENAME_FORMAT]
     */
    fun captureImage(imageSavedCallback: ImageCapture.OnImageSavedCallback) {
        // Use a stable reference of the modifiable image capture use case
        mImageCapture?.let { imageCapture ->

            // Prepare new empty output file to save the image data
            val photoFile = createFile(outputDirectory, FILENAME_FORMAT, PHOTO_EXTENSION)

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {

                // Mirror image when using the front camera
                isReversedHorizontal = currentCameraLensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(photoFile, metadata, ContextCompat.getMainExecutor(mContext), imageSavedCallback)
        }
    }

    companion object {

        private const val TAG = "PrequelCameraPrototype"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"

        private const val ZOOM_RATIO_UPPER_LIMIT = 2F

        /** @brief Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.ROOT)
                .format(System.currentTimeMillis()) + extension)

        /** @brief Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }

            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }
    }
}