package com.prng.aeedee_android_chat.roomdb.entity_model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataUsers

@Entity(tableName = "DatabaseMessageData")
data class DatabaseMessageData(
    @PrimaryKey val _id: String,
    @ColumnInfo(name = "receiverId") val receiverId: String,
    @ColumnInfo(name = "response") val response: List<DatabaseMessageModel>,
)

fun List<DatabaseMessageData>.asDatabaseModel(): List<MessageDataUsers> {
    return map {
        MessageDataUsers(
            _id = it._id,
            receiverId = it.receiverId,
            response = it.response.map { data ->
                DatabaseMessageModel(
                    _id = data._id,
                    userId = data.userId,
                    uniqueId = data.uniqueId,
                    receiverId = data.receiverId,
                    readStatus = data.readStatus,
                    message = data.message,
                    updatedAt = data.updatedAt,
                    createdAt = data.createdAt,
                    status = data.status,
                    dateTime = data.dateTime,
                    link = data.link,
                    position = data.position,
                    files = data.files,
                    msgType = data.msgType,
                    repliedId = data.repliedId,
                    replyMsg = data.replyMsg,
                    timezone = data.timezone,
                    reaction = data.reaction,
                )
            }
        )
    }
}