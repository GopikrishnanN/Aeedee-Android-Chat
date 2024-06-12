package com.prng.aeedee_android_chat.view.chat_message.model

import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData

data class SendMessageRequest(
    val receiver_id: String? = "",
    val read_status: Int? = 0,
    val status: Int? = 0,
    val unique_id: String? = "",
    val message: String? = "",
    val link: String? = "",
    val repliedId: String? = "",
    val replyImage: String? = "",
    val replymsg: String? = "",
    val files: List<FileData>? = arrayListOf(),
    val msgType: String? = "",

    val createdAt: String? = "",
    val updatedAt: String? = "",
    val timezone: String? = "",

    val chat_type: String? = "",
)