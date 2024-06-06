package com.prng.aeedee_android_chat.roomdb.entity_model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse

@Entity(tableName = "DatabaseUsersModel")
data class DatabaseUsersModel(
    @PrimaryKey val _id: String,
    @ColumnInfo(name = "userName") val userName: String? = "",
    @ColumnInfo(name = "avatar") val avatar: String? = "",
    @ColumnInfo(name = "message") val message: String? = "",
    @ColumnInfo(name = "createdAt") var createdAt: String? = "",
    @ColumnInfo(name = "updatedAt") val updatedAt: String? = "",
    @ColumnInfo(name = "userId") val userId: String? = "",
    @ColumnInfo(name = "firstChar") var firstChar: String? = "",
    @ColumnInfo(name = "count") var count: Int? = 0,
    @ColumnInfo(name = "status") var status: Int? = 0,
)

fun List<DatabaseUsersModel>.asDatabaseModel(): List<UserDataResponse> {
    return map {
        UserDataResponse(
            _id = it._id,
            message = it.message,
            userName = it.userName,
            firstChar = it.firstChar,
            updatedAt = it.updatedAt,
            createdAt = it.createdAt,
            avatar = it.avatar,
            userId = it.userId,
            count = it.count,
            status = it.status,
        )
    }
}