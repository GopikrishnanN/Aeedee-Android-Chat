package com.prng.aeedee_android_chat.view.chat_message.model

data class MessageRequest(
    val receiverId: String,
    val lastId: String,
    val limit: Int,
    val recent: Int
)
