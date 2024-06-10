package com.prng.aeedee_android_chat.view.chat

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatUserListViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<ChatUserResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    // Delete Message
    var onDeleteMessageListener: ((DeleteMessageRequest) -> Unit)? = null

    // Reaction Message
    var onReactionMessageListener: ((DatabaseReactionData) -> Unit)? = null

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

        ChatRepository.onDeleteMessageUpdateListener = {
            onDeleteMessageListener?.invoke(it)
        }

        ChatRepository.onReactionMessageUpdateListener = {
            onReactionMessageListener?.invoke(it)
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

    fun updateChildEntityInParent(
        chatDao: ChatDao, parentId: String, childId: String, ifData: Int,
        reaction: DatabaseReactionData? = null
    ) =
        CoroutineScope(Dispatchers.IO).launch {

            val parentWithChildren = chatDao.getParentWithChildren(parentId)

            if (parentWithChildren != null) {
                if (parentWithChildren.children != null) {
                    val childToUpdate =
                        parentWithChildren.children.find { it.uniqueId == childId }

                    if (ifData == 1)
                        childToUpdate?.status = 0
                    else if (ifData == 2)
                        childToUpdate?.reaction = arrayListOf(reaction!!)

                    if (childToUpdate != null) {
                        chatDao.updateChildren(childToUpdate)

                        val index =
                            parentWithChildren.children.indexOfFirst { it.uniqueId == childId }
                        val childrenList = parentWithChildren.children.toMutableList()
                        if (index != -1) {
                            val parentToUpdate = parentWithChildren.parent
                            childrenList[index] = childToUpdate
                            parentToUpdate?.let {
                                it.receiverId = parentId
                                it.response = childrenList
                                chatDao.updateParent(it)
                            }
                        }
                    }
                }
            }
        }

//    private suspend fun getReactionUpdates(
//        reactionList: MutableList<DatabaseReactionData>?, reaction: DatabaseReactionData?
//    ): MutableList<DatabaseReactionData>? =
//        suspendCoroutine { continuation ->
//            CoroutineScope(Dispatchers.IO).launch {
//                if (reactionList != null) {
//                    if (reactionList.isNotEmpty()) {
//                        reactionList.forEachIndexed { index, data ->
//                            if (data.userId == reaction?.userId) {
//                                reactionList[index].message = reaction?.message.toString()
//                            }
//                        }
//                        continuation.resume(reactionList)
//                    } else {
//                        reaction?.let {
//                            reactionList.add(it)
//                            continuation.resume(reactionList)
//                        }
//                    }
//                } else {
//                    reaction?.let {
//                        continuation.resume(arrayListOf(it))
//                    }
//                }
//            }
//        }
}