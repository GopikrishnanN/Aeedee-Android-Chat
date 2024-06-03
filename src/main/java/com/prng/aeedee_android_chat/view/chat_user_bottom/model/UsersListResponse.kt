package com.prng.aeedee_android_chat.view.chat_user_bottom.model

data class UsersListResponse(
    val status: Int,
    val message: String,
    val response: List<UsersDataResponse>,
    val count: Int,
    val lastId: String,
    val followersCount: Int,
    val blockCount: Int,
    val requestCount: Int,
)