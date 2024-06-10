package com.prng.aeedee_android_chat.view.forward_chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.prng.aeedee_android_chat.MessageType
import com.prng.aeedee_android_chat.extractFirstUrl
import com.prng.aeedee_android_chat.getTimeZone
import com.prng.aeedee_android_chat.getUniqueId
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ForwardUsersViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<ChatUserResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    var onCloseActivity: ((Boolean) -> Unit)? = null

    // Message Type // normal, forward, reply
    private var msgType: String = MessageType.Forward.name.lowercase(Locale.getDefault())

    // Unique Id
    var uniqueId: String = ""

    // MessageDataResponse
    private var mMessageData: MessageDataResponse? = null

    fun initData(messageData: MessageDataResponse?) {
        mMessageData = messageData
    }

    fun getChatUserList(request: ChatUserRequest): LiveData<ChatUserResponse>? {
        usersLiveData = ChatActivityRepository.getChatUserListApiCall(userId = userID, request)
        return usersLiveData
    }

    fun emitSendMessage(receiverId: String, isCloseable: Boolean) {
        ChatRepository.emitSendMessage(sendMessage(receiverId))

//        messageEventListener(uniqueId = uniqueId)

        onCloseActivity?.invoke(isCloseable)
    }

    private fun sendMessage(receiverId: String): JSONObject {
        val sJSONObject = JSONObject()
        runBlocking {
            uniqueId = getUniqueId()

            sJSONObject.put("receiver_id", receiverId)
            sJSONObject.put("user_id", userID)
            sJSONObject.put("read_status", getReadStatus(receiverId))
            sJSONObject.put("unique_id", uniqueId)
            sJSONObject.put("message", getMessageText(mMessageData))
            sJSONObject.put("status", 1)
            sJSONObject.put("link", getFistLink(getMessageText(mMessageData)))
            sJSONObject.put("files", JSONArray(Gson().toJson(mMessageData?.files)))
            sJSONObject.put("msgType", msgType)
            sJSONObject.put("repliedId", "")
            sJSONObject.put("replymsg", "")
            sJSONObject.put("chat_type", msgType)
            sJSONObject.put("timezone", getTimeZone())

            Log.e("TAG", "messages...: $sJSONObject")
        }
        return sJSONObject
    }

    private fun getReadStatus(receiverId: String): Int {
        return if (receiverId == userID) 3 else 1
    }

    private fun getFistLink(messageText: String): String {
        return extractFirstUrl(messageText).toString()
    }

    private fun getMessageText(data: MessageDataResponse?): String {
        if (data != null) {
            if (data.files!!.isNotEmpty()) {
                val list = data.files.filter { it.type == "image" }.map { it.type }
                if (list.isNotEmpty()) {
                    return "Image"
                }
            }
        }
        return data?.message ?: ""
    }

    @Suppress("UNUSED_PARAMETER")
    fun afterSearchTextChanged(char: CharSequence, start: Int, end: Int, count: Int) {
        mSearchText = char.toString()
        val request = ChatUserRequest(limit = 50, search = mSearchText.trim())
        onSearchListener?.invoke(request)
    }

}