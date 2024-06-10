package com.prng.aeedee_android_chat.view.chat_message.model.message

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileData(
    val url: String? = "",
    val type: String? = "",
) : Parcelable
