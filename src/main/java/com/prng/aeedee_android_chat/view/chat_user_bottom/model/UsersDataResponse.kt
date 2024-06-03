package com.prng.aeedee_android_chat.view.chat_user_bottom.model

data class UsersDataResponse(
    val _id: String,
    val status: String? = "",
    var createdAt: String? = "",
    val name: String? = "",
    val email: String? = "",
    val avatar: String? = "",
    val phone: PhoneModel? = null,
    var aeedee_id: String? = "",
    val userId: String? = "",
    val friendId: String? = "",
    var firstChar: String? = "",
) {
    fun getFirstCharUppercase(): String {
        return name.toString()[0].uppercaseChar().toString()
    }
}