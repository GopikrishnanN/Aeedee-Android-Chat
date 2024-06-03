package com.prng.aeedee_android_chat.view.chat.model

import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse

data class ChatUserResponse(
    val status: Int,
    val message: String,
    val response: List<UserDataResponse>,
    val count: Int,
    val lastId: String,
)