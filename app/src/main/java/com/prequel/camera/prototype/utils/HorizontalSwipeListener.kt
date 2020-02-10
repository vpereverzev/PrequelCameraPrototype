package com.prequel.camera.prototype.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs


open class HorizontalSwipeListener : GestureDetector.SimpleOnGestureListener() {

    override fun onFling(
        event1: MotionEvent,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        val diffY = event2.y - event1.y
        val diffX = event2.x - event1.x

        val isCorrespondingToSwipe =
            abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD

        val isHorizontalSwipe = abs(diffX) > abs(diffY)

        if (isCorrespondingToSwipe && isHorizontalSwipe) {

            if (diffX > 0) {
                onSwipeRight()
            } else {
                onSwipeLeft()
            }
            return true
        }
        return false
    }

    open fun onSwipeRight() {}

    open fun onSwipeLeft() {}

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}