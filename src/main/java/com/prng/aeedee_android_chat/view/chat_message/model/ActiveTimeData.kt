package com.prng.aeedee_android_chat.view.chat_message.model

import com.google.gson.annotations.SerializedName

data class ActiveTimeData(
    @SerializedName("last_seen_time") val lastSeenTime: String? = "",
    @SerializedName("chat_status") val chatStatus: String? = "",
    @SerializedName("user_id") val userId: String? = "",
    @SerializedName("receiver_id") val receiverId: String? = "",
)
