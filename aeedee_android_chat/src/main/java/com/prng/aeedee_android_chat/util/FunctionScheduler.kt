package com.prng.aeedee_android_chat.util

import android.os.Handler
import android.os.Looper

class FunctionScheduler(private val apiFunction: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var delayTime = 10000L

    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                apiFunction()
                handler.postDelayed(this, delayTime)
            }
        }
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            handler.post(runnable)
        }
    }

    fun pause() {
        isRunning = false
        handler.removeCallbacks(runnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(runnable)
    }
}
