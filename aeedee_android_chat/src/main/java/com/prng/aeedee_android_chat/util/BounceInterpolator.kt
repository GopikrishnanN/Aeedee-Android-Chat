package com.prng.aeedee_android_chat.util

import android.view.animation.Interpolator

class BounceInterpolator : Interpolator {
    override fun getInterpolation(time: Float): Float {
        return (time * time * ((2.0 + 1) * time - 2.0)).toFloat()
    }
}