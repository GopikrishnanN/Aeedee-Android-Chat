package com.prng.aeedee_android_chat.view.chat_message.model

import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseFileData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData

data class MessageDataResponse(
    val _id: String,
    val unique_id: String? = "",
    val userId: String,
    val receiverId: String,
    var read_status: Int,
    val message: String,
    var status: Int,
    val link: String,
    val files: MutableList<FileData>? = mutableListOf(),
    var dateTime: String? = "",
    var createdAt: String,
    val updatedAt: String,
    val position: String? = "start",
    val msgType: String? = "",
    val repliedId: String? = "",
    val replymsg: String? = "",
    val timezone: String? = "",
    val chat_type: String? = "",
    var reaction: MutableList<DatabaseReactionData>? = mutableListOf(),
    var isSelected: Boolean = false,
    var isSelectEnable: Boolean = false,
)

fun List<MessageDataResponse>.asDatabaseModel(): List<DatabaseMessageModel> {
    return map {
        DatabaseMessageModel(
            _id = it._id,
            userId = it.userId,
            uniqueId = it.unique_id,
            receiverId = it.receiverId,
            readStatus = it.read_status,
            message = it.message,
            updatedAt = it.updatedAt,
            createdAt = it.createdAt,
            status = it.status,
            dateTime = it.dateTime,
            link = it.link,
            files = it.files?.map { data ->
                DatabaseFileData(url = data.url ?: "", type = data.type)
            }?.toMutableList(),
            position = it.position,
            msgType = it.msgType,
            repliedId = it.repliedId,
            replyMsg = it.replymsg,
            timezone = it.timezone,
            chatType = it.chat_type,
            reaction = it.reaction,
        )
    }
}