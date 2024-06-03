package com.prng.aeedee_android_chat.view.chat_message.model

import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse

data class MessageListResponse(
    val status: Int,
    val message: String,
    val response: List<MessageDataResponse>,
    val count: Int,
    val lastId: String?,
)