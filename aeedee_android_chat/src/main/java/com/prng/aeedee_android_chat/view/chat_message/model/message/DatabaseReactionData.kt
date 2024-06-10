package com.prng.aeedee_android_chat.view.chat_message.model.message

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DatabaseReactionData(
    @SerializedName("message") var message: String = "",
    @SerializedName("messageId") var messageId: String? = "",
    @SerializedName("user_id") var userId: String? = "",
    @SerializedName("receiver_id") var receiverId: String? = "",
) : Parcelable