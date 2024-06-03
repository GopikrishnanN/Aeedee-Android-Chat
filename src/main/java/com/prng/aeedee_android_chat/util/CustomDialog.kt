package com.prng.aeedee_android_chat.util

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

class CustomDialog<T : ViewBinding>(
    context: Context,
    private val bindingInflater: (LayoutInflater) -> T
) : Dialog(context) {

    private lateinit var binding: T

    var configureDialog: ((T) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindingInflater(layoutInflater)
        setContentView(binding.root)

        configureDialog?.invoke(binding)
    }
}