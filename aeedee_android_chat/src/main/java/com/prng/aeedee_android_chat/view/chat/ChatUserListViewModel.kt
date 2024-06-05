package com.prng.aeedee_android_chat.view.chat

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import org.json.JSONObject

class ChatUserListViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<ChatUserResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    fun getChatUserList(request: ChatUserRequest): LiveData<ChatUserResponse>? {
        usersLiveData = ChatActivityRepository.getChatUserListApiCall(userId = userID, request)
        return usersLiveData
    }

    init {
        mSearchText = ""
        ChatRepository.onSocketStatus = { isConnected ->
            if (isConnected) {
                ChatRepository.onConnectionListener()
                emitChatConnection()
            }
        }

        ChatRepository.onRefreshListListener = {
            if (it) {
                val request = ChatUserRequest(limit = 50, search = mSearchText.trim())
                onSearchListener?.invoke(request)
            }
        }
    }

    fun initSocket(activity: Activity) {
        ChatRepository.initSocket(activity)
    }

    private fun emitChatConnection() {
        ChatRepository.emitChatConnection(sendChatConnection(true))
    }

    fun emitChatDisconnection() {
        ChatRepository.emitChatDisconnection(sendChatConnection(false))
    }

    private fun sendChatConnection(isConnect: Boolean): JSONObject {
        val cJSONObject = JSONObject()
        cJSONObject.put("receiver_id", "")
        if (isConnect) cJSONObject.put("userId", userID)
        else cJSONObject.put("user_id", userID)
        Log.e("TAG", "connections...: $cJSONObject")
        return cJSONObject
    }

    fun disconnectSocket() {
        SocketHandler.closeConnection()
    }

    fun afterSearchTextChanged(char: CharSequence, start: Int, end: Int, count: Int) {
        mSearchText = char.toString()
        val request = ChatUserRequest(limit = 50, search = mSearchText.trim())
        onSearchListener?.invoke(request)
    }
}