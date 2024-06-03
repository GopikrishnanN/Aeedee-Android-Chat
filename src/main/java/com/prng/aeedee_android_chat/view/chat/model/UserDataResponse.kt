package com.prng.aeedee_android_chat.view.chat.model

import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseUsersModel

data class UserDataResponse(
    val _id: String,
    val userName: String? = "",
    val avatar: String? = "",
    var message: String? = "",
    var createdAt: String? = "",
    val updatedAt: String? = "",
    val userId: String? = "",
    var firstChar: String? = "",
)

fun List<UserDataResponse>.asDatabaseModel(): List<DatabaseUsersModel> {
    return map {
        DatabaseUsersModel(
            _id = it._id,
            message = it.message,
            userName = it.userName,
            firstChar = it.firstChar,
            updatedAt = it.updatedAt,
            createdAt = it.createdAt,
            avatar = it.avatar,
            userId = it.userId
        )
    }
}