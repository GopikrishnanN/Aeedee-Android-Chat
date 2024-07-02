package com.prng.aeedee_android_chat.view.forward_chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.MessageType
import com.prng.aeedee_android_chat.extractFirstUrl
import com.prng.aeedee_android_chat.getCurrentDateTime
import com.prng.aeedee_android_chat.getTimeZone
import com.prng.aeedee_android_chat.getUniqueId
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersListResponse
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ForwardUsersViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<UsersListResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    var onCloseActivity: ((Boolean) -> Unit)? = null

    // Message Type // normal, forward, reply
    private var msgType: String = MessageType.Forward.name.lowercase(Locale.getDefault())

    // Unique Id
    var uniqueId: String = ""

    // Send Data
    private var files: JSONArray? = null
    private var message: String? = ""

    fun initData(files: JSONArray?, message: String?) {
        this.files = files
        this.message = message
    }

    fun getChatUserList(): LiveData<UsersListResponse>? {
        val auth =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImdhbmVzaC5pbmZvLmtAZ21haWwuY29tIiwiaWQiOiI2NWIxZjMzN2M2NWQ3ODc0NWRmMTVjYTEiLCJpYXQiOjE3MDgwMDAwMDl9.w7igawY4BRsbQpXrU6t3HuWv1e2lOzNmogZ345SYi9M"
        usersLiveData = ChatActivityRepository.getFollowersListApiCall(auth = auth, userId = userID)
        return usersLiveData
    }

    fun emitSendMessage(receiverId: String, isCloseable: Boolean) {
        ChatRepository.emitSendMessage(sendMessage(receiverId))

        onCloseActivity?.invoke(isCloseable)
    }

    private fun sendMessage(receiverId: String): JSONObject {
        val sJSONObject = JSONObject()
        runBlocking {
            uniqueId = getUniqueId()

            sJSONObject.put("receiver_id", receiverId)
            sJSONObject.put("user_id", userID)
            sJSONObject.put("unique_id", uniqueId)
            sJSONObject.put("msgType", msgType)
            sJSONObject.put("repliedId", "")
            sJSONObject.put("replymsg", "")
            sJSONObject.put("status", 1)
            sJSONObject.put("createdAt", getCurrentDateTime())
            sJSONObject.put("updatedAt", getCurrentDateTime())
            sJSONObject.put("timezone", getTimeZone())
            sJSONObject.put("read_status", getReadStatus(receiverId))
            sJSONObject.put("chat_type", getChatType())

            // Extra data
            sJSONObject.put("message", getMessageText())
            sJSONObject.put("link", getFistLink(getMessageText()))
            sJSONObject.put("files", files)
            Log.e("TAG", "messages...: $sJSONObject")
        }
        return sJSONObject
    }

    private fun getChatType(): String {
        return if (files != null) {
            val fileDataList: List<FileData> = (0 until files!!.length()).map { index ->
                val json = files!!.getJSONObject(index)
                FileData(url = json.getString("url"), type = json.getString("type"))
            }
            if (fileDataList.isNotEmpty()) "Image" else "Text"
        } else "Text"
    }

    private fun getReadStatus(receiverId: String): Int {
        return if (receiverId == userID) 3 else 1
    }

    private fun getFistLink(messageText: String): String {
        return extractFirstUrl(messageText).toString()
    }

    private fun getMessageText(): String {
        if (files != null) {
            val fileDataList: List<FileData> = (0 until files!!.length()).map { index ->
                val json = files!!.getJSONObject(index)
                FileData(url = json.getString("url"), type = json.getString("type"))
            }
            if (fileDataList.isNotEmpty()) {
                val list = fileDataList.filter { it.type == "image" }.map { it.type }
                if (list.isNotEmpty()) {
                    return "Image"
                }
            }
        }
        return message ?: ""
    }

    @Suppress("UNUSED_PARAMETER")
    fun afterSearchTextChanged(char: CharSequence, start: Int, end: Int, count: Int) {
        mSearchText = char.toString()
        val request = ChatUserRequest(limit = 50, search = mSearchText.trim())
        onSearchListener?.invoke(request)
    }

}