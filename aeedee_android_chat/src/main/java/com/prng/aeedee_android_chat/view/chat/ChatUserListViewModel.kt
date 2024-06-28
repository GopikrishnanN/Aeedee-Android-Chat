package com.prng.aeedee_android_chat.view.chat

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import com.prng.aeedee_android_chat.view.chat.model.DeleteUserRequest
import com.prng.aeedee_android_chat.view.chat_message.model.ReadStatusData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatUserListViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<ChatUserResponse>? = MutableLiveData(null)

    private var deleteLiveData: MutableLiveData<DeleteMessageResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    // Delete Message
    var onDeleteMessageListener: ((DeleteMessageRequest) -> Unit)? = null

    // Reaction Message
    var onReactionMessageListener: ((DatabaseReactionData) -> Unit)? = null

    // Read Status
    var onReadStatusListener: ((ReadStatusData) -> Unit)? = null

    fun getChatUserList(request: ChatUserRequest): LiveData<ChatUserResponse>? {
        usersLiveData = ChatActivityRepository.getChatUserListApiCall(userId = userID, request)
        return usersLiveData
    }

    fun deleteChatUserList(request: DeleteUserRequest): LiveData<DeleteMessageResponse> {
        deleteLiveData = ChatActivityRepository.deleteUserChatApiCall(userID, request)
        return deleteLiveData as MutableLiveData<DeleteMessageResponse>
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

        ChatRepository.onDeleteMessageUpdateListener = {
            onDeleteMessageListener?.invoke(it)
        }

        ChatRepository.onReactionMessageUpdateListener = {
            onReactionMessageListener?.invoke(it)
        }

        ChatRepository.onReadStatusMessageUpdateListener = {
            onReadStatusListener?.invoke(it)
        }

    }

    fun initSocket(activity: Activity) {
        ChatRepository.initSocket(activity)
    }

    fun emitChatConnection() {
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

    fun updateChildEntityInParent(
        chatDao: ChatDao, childIds: List<String>, receiverId: String,
        reaction: DatabaseReactionData? = null, readStatus: ReadStatusData? = null, ifData: Int
    ) = CoroutineScope(Dispatchers.IO).launch {
        val idsSet = childIds.toSet()
        val parentWithChildren = chatDao.getParentWithChildren(receiverId)

        if (parentWithChildren != null) {
            if (parentWithChildren.parent?.response != null) {

                val childToUpdate: MutableList<DatabaseMessageModel> =
                    parentWithChildren.parent.response.map {
                        if (ifData == 1 && idsSet.contains(it.uniqueId)) it.status = 0
                        if (ifData == 2 && idsSet.contains(it.uniqueId)) it.reaction =
                            arrayListOf(reaction!!)
                        if (ifData == 3 && idsSet.contains(it.uniqueId)) it.readStatus =
                            readStatus!!.readStatus!!.toInt()
                        chatDao.updateChildren(it)
                        it
                    }.toMutableList()

                if (childToUpdate != null) {
                    try {
                        val parentToUpdate = parentWithChildren.parent
                        parentToUpdate.let {
                            it.receiverId = receiverId
                            it.response = childToUpdate
                            chatDao.updateParent(it)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

}