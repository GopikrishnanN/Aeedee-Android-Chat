package com.prng.aeedee_android_chat.view.chat_message.model.message

import com.google.gson.annotations.SerializedName

data class DatabaseReactionData(
    @SerializedName("message") val message: String = "",
    @SerializedName("messageId") val messageId: String? = "",
    @SerializedName("user_id") val userId: String? = "",
    @SerializedName("receiver_id") val receiverId: String? = "",
)