package com.prng.aeedee_android_chat.view.chat_message.model.message

data class DeleteMessageRequest(
    val ids: List<String>? = arrayListOf(),
    val receiver_id: String? = null,
    val user_id: String? = null
)
