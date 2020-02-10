package com.prequel.camera.prototype.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.prequel.camera.prototype.R
import com.prequel.camera.prototype.databinding.FragmentCameraBinding
import com.prequel.camera.prototype.models.CameraManager
import com.prequel.camera.prototype.utils.HorizontalSwipeListener
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @brief Main fragment with the camera preview and UI controls for capturing new images or to open the gallery with captured photos
 */
class CameraFragment : Fragment() {

    val captureButtonColor = ObservableInt(Color.WHITE)
    val galleryPreviewUri = ObservableField<String>(Uri.parse("R.drawable.ic_mountains_photo").toString())

    private lateinit var mRootContainer: ConstraintLayout
    private lateinit var mViewFinder: PreviewView
    private lateinit var mCameraManager: CameraManager

    override fun onResume() {
        super.onResume()

        // Ensure that all permissions are still granted
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentCameraBinding>(inflater, R.layout.fragment_camera, container, false)
        binding.cameraHandler = this

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRootContainer = view as ConstraintLayout
        mViewFinder = mRootContainer.findViewById(R.id.view_finder)

        mCameraManager = CameraManager(requireContext())

        // Wait for the views to be properly laid out
        mViewFinder.postDelayed({

            // Open back camera by default
            setUpCamera(CameraSelector.LENS_FACING_FRONT)
        }, 500L)

        setUpGestureDetectors()
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun setUpCamera(cameraLensFacing: Int) {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { mViewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        // Initial screen rotation
        val rotation = mViewFinder.display.rotation

        // Bind use cases
        mCameraManager.bindCamera(
            this as LifecycleOwner,
            cameraLensFacing,
            screenAspectRatio,
            rotation,
            mViewFinder.previewSurfaceProvider
        )
    }

    private fun setUpGestureDetectors() {

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val delta = detector.scaleFactor
                mCameraManager.adjustZoom(delta)
                return true
            }
        })

        val swipeGestureDetector = GestureDetector(requireContext(), object : HorizontalSwipeListener() {
            override fun onSwipeRight() {
                captureButtonColor.set(nextCaptureButtonColor())
            }

            override fun onSwipeLeft() {
                captureButtonColor.set(previousCaptureButtonColor())
            }
        })

        val tapGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                val cameraLensFacing = if (CameraSelector.LENS_FACING_FRONT == mCameraManager.currentCameraLensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }

                setUpCamera(cameraLensFacing)
                return true
            }
        })

        view?.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            swipeGestureDetector.onTouchEvent(event)
            tapGestureDetector.onTouchEvent(event)
            true
        }
    }

    fun takePicture() {
        mCameraManager.captureImage(imageSavedListener)

        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Display flash animation to indicate that photo was captured
            mRootContainer.postDelayed({
                mRootContainer.foreground = ColorDrawable(Color.WHITE)
                mRootContainer.postDelayed(
                    { mRootContainer.foreground = null }, FLASH_ANIMATION_MILLIS
                )
            }, FLASH_ANIMATION_MILLIS)
        }
    }

    fun openGallery() {
        // It makes sense to open the gallary only if we have images to show
        if (!mCameraManager.isImageStorageEmpty()) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToGallery(mCameraManager.outputDirectory.absolutePath))
        }
    }

    private fun nextCaptureButtonColor(): Int {
        var index = COLOR_LIST.indexOf(captureButtonColor.get())

        return if (index == COLOR_LIST.lastIndex) COLOR_LIST[0] else COLOR_LIST[++index]
    }

    private fun previousCaptureButtonColor(): Int {
        var index = COLOR_LIST.indexOf(captureButtonColor.get())

        return if (index == 0) COLOR_LIST[COLOR_LIST.lastIndex] else COLOR_LIST[--index]
    }

    /** @brief Define callback that will be triggered after a photo has been taken and saved to disk */
    private val imageSavedListener = object : ImageCapture.OnImageSavedCallback {
        override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
            Log.e(TAG, "Photo capture failed: $message", cause)
        }

        override fun onImageSaved(photoFile: File) {
            Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")

            galleryPreviewUri.set(Uri.fromFile(photoFile).toString())
        }
    }

    companion object {

        private const val TAG = "PrequelCameraPrototype"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val FLASH_ANIMATION_MILLIS = 100L
        private val COLOR_LIST = listOf(Color.WHITE, Color.RED, Color.GREEN, Color.BLUE)

        @JvmStatic @BindingAdapter("app:srcFile")
        fun setGalleryThumbnail(view: View, fileUrl: String) {
            // Load thumbnail into circular button using Glide
            Glide.with(view as ImageButton)
                .load(fileUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(view)
        }
    }
}