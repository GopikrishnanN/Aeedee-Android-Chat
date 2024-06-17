package com.prng.aeedee_android_chat.view.chat.model

import com.google.gson.annotations.SerializedName

data class UserStatusData(
    @SerializedName("userId", alternate = ["user_id"]) val userId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("status") val isStatus: Boolean,
)
