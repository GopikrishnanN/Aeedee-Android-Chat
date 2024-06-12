package com.prng.aeedee_android_chat.view.chat_message.model

import android.os.Parcelable
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseFileData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDataResponse(
    val _id: String,
    var unique_id: String? = "",
    val userId: String,
    val receiverId: String,
    var originId: String? = "",
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
    val replyImage: String? = "",
    val replymsg: String? = "",
    val replyUserid: String? = "",
    val timezone: String? = "",
    val chat_type: String? = "",
    var reaction: MutableList<DatabaseReactionData>? = mutableListOf(),
    var isSelected: Boolean = false,
    var isSelectEnable: Boolean = false,
) : Parcelable {
    fun getReactionData(): String {
        return if (reaction != null) {
            if (reaction!!.isNotEmpty()) {
                reaction!!.first().message
            } else ""
        } else ""
    }
}

fun List<MessageDataResponse>.asDatabaseModel(): List<DatabaseMessageModel> {
    return map {
        DatabaseMessageModel(
            _id = it._id,
            userId = it.userId,
            uniqueId = it.unique_id,
            receiverId = it.receiverId,
            originId = it.originId,
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
            replyUserid = it.replyUserid,
            replyImage = it.replyImage,
            timezone = it.timezone,
            chatType = it.chat_type,
            reaction = it.reaction,
        )
    }
}