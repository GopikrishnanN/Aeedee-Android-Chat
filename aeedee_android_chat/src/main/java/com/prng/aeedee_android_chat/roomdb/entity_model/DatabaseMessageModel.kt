package com.prng.aeedee_android_chat.roomdb.entity_model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseFileData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData

@Entity(
    tableName = "DatabaseMessageModel"
)
data class DatabaseMessageModel(
    @PrimaryKey val _id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("unique_id") val uniqueId: String? = "",
    @SerializedName("receiverId") var receiverId: String,
    @SerializedName("originId") val originId: String,
    @SerializedName("read_status") var readStatus: Int,
    @SerializedName("message") val message: String,
    @SerializedName("status") var status: Int,
    @SerializedName("createdAt") var createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("dateTime") var dateTime: String? = "",
    @SerializedName("link") var link: String,
    @SerializedName("position") val position: String? = "start",
    @SerializedName("msgType") val msgType: String? = "",
    @SerializedName("repliedId") val repliedId: String? = "",
    @SerializedName("replymsg") val replyMsg: String? = "",
    @SerializedName("replyUserid") val replyUserid: String? = "",
    @SerializedName("timezone") val timezone: String? = "",
    @SerializedName("chat_type") val chatType: String? = "",
    @SerializedName("reaction") var reaction: MutableList<DatabaseReactionData>? = mutableListOf(),
    @SerializedName("files") val files: MutableList<DatabaseFileData>? = mutableListOf()
)

fun List<DatabaseMessageModel>.asDatabaseModel(): List<MessageDataResponse> {
    return map {
        MessageDataResponse(
            _id = it._id,
            userId = it.userId,
            unique_id = it.uniqueId,
            receiverId = it.receiverId,
            originId = it.originId,
            read_status = it.readStatus,
            message = it.message,
            updatedAt = it.updatedAt,
            createdAt = it.createdAt,
            status = it.status,
            dateTime = it.dateTime,
            link = it.link,
            position = it.position,
            files = it.files?.map { data ->
                FileData(url = data.url, type = data.type)
            }?.toMutableList(),
            msgType = it.msgType,
            repliedId = it.repliedId,
            replymsg = it.replyMsg,
            replyUserid = it.replyUserid,
            timezone = it.timezone,
            chat_type = it.chatType,
            reaction = it.reaction,
        )
    }
}