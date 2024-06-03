package com.prng.aeedee_android_chat.view.chat_message.model

import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageData
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel

data class MessageDataUsers(
    val _id: String,
    val receiverId: String,
    val response: List<DatabaseMessageModel>,
)

fun List<MessageDataUsers>.asDatabaseModel(): List<DatabaseMessageData> {
    return map {
        DatabaseMessageData(
            _id = it._id,
            receiverId = it.receiverId,
            response = it.response.map { data ->
                DatabaseMessageModel(
                    _id = data._id,
                    userId = data.userId,
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