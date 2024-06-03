package com.prng.aeedee_android_chat.view.chat_message.model

import com.google.gson.annotations.SerializedName

data class ReadStatusData(
    @SerializedName("ids") val ids: List<String>? = arrayListOf(),
    @SerializedName("read_status") var readStatus: String? = "",
)
