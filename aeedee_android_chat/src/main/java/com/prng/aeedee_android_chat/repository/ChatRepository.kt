package com.prng.aeedee_android_chat.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.prng.aeedee_android_chat.ACTIVE_TIME
import com.prng.aeedee_android_chat.CHAT_CONNECT
import com.prng.aeedee_android_chat.CHAT_DISCONNECT
import com.prng.aeedee_android_chat.DELETE_MESSAGE
import com.prng.aeedee_android_chat.NEW_MESSAGE
import com.prng.aeedee_android_chat.REACTION
import com.prng.aeedee_android_chat.READ_STATUS
import com.prng.aeedee_android_chat.START_TYPING
import com.prng.aeedee_android_chat.STOP_TYPING
import com.prng.aeedee_android_chat.getOrDefault
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.toDataClass
import com.prng.aeedee_android_chat.view.chat.model.UserStatusData
import com.prng.aeedee_android_chat.view.chat_message.model.ActiveTimeData
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.ReadStatusData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData
import com.prng.aeedee_android_chat.view.chat_message.model.typing.TypingData
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

@SuppressLint("StaticFieldLeak")
object ChatRepository {
    private lateinit var mSocket: Socket

    private lateinit var mActivity: Activity

    var onNewMessageListener: ((MessageDataResponse) -> Unit)? = null

    var onRefreshListListener: ((Boolean) -> Unit)? = null

    var onTypingListener: ((TypingData) -> Unit)? = null

    var onUserOnlineListener: ((UserStatusData) -> Unit)? = null

    var onSocketStatus: ((Boolean) -> Unit)? = null

    var onActiveTimeListener: ((ActiveTimeData) -> Unit)? = null

    var onReadStatusListener: ((ReadStatusData) -> Unit)? = null

    var onReactionDataListener: ((DatabaseReactionData) -> Unit)? = null

    var onDeleteMessageListener: ((DeleteMessageRequest) -> Unit)? = null

    var onDeleteMessageUpdateListener: ((DeleteMessageRequest) -> Unit)? = null

    var onReactionMessageUpdateListener: ((DatabaseReactionData) -> Unit)? = null

    var onReadStatusMessageUpdateListener: ((ReadStatusData) -> Unit)? = null

    fun initSocket(activity: Activity) {
        mActivity = activity
        try {
            SocketHandler.setSocket(activity)
            mSocket = SocketHandler.getSocket()
            SocketHandler.onConnection()
            SocketHandler.onDisconnection()
            SocketHandler.establishConnection()
            onSocketListener()
        } catch (e: URISyntaxException) {
            Log.d("myTag", e.message!!)
        }
    }

    private fun onSocketListener() {
        SocketHandler.onSocketStatus = {
            onSocketStatus?.invoke(it)
        }
    }

    fun onChatConnect() {
        onActiveTime()
        onListenerChat()
    }

    private fun onListenerChat() {
        offChatEvents()
        Handler(Looper.getMainLooper()).postDelayed({
            mSocket.on(START_TYPING, onStartTyping)
            mSocket.on(STOP_TYPING, onStopTyping)
        }, 500)
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError)
    }

    private fun offChatEvents() {
        mSocket.off(START_TYPING, onStartTyping)
        mSocket.off(STOP_TYPING, onStopTyping)
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError)
    }

    fun onConnectionListener() {
        offEventsConnection()
        mSocket.on(CHAT_CONNECT, onChatConnect)
        mSocket.on(CHAT_DISCONNECT, onChatDisconnect)
        mSocket.on(NEW_MESSAGE, onNewMessage)
        mSocket.on(DELETE_MESSAGE, onDeleteMessage)
        mSocket.on(REACTION, onReaction)
        onReadStatusListener()
    }

    fun emitReadStatusListener(data: JSONObject) {
        mSocket.emit(READ_STATUS, data)
    }

    private fun onReadStatusListener() {
//        mSocket.off(READ_STATUS, onReadStatus)
        mSocket.on(READ_STATUS, onReadStatus)
    }

    fun emitActiveTime(data: JSONObject) {
        if (mSocket.connected()) mSocket.emit(ACTIVE_TIME, data)
    }

    private fun onActiveTime() {
        mSocket.off(ACTIVE_TIME, onActiveTime)
        mSocket.on(ACTIVE_TIME, onActiveTime)
    }

    fun emitReaction(data: JSONObject) {
        mSocket.emit(REACTION, data)
    }

    fun emitChatConnection(data: JSONObject) {
        mSocket.emit(CHAT_CONNECT, data)
    }

    fun emitChatDisconnection(data: JSONObject) {
        mSocket.emit(CHAT_DISCONNECT, data)
    }

    fun emitStartStop(isStart: Boolean, data: JSONObject) {
        if (isStart) {
            mSocket.emit(START_TYPING, data)
        } else {
            mSocket.emit(STOP_TYPING, data)
        }
    }

    fun emitSendMessage(data: JSONObject) {
        mSocket.emit(NEW_MESSAGE, data)
//        mSocket.emit(NEW_MESSAGE, data, Ack { args ->
//            mActivity.runOnUiThread {
//                try {
//                    val response = JSONObject(args[0].toString())
//                    Log.e("TAG", "emitSendMessage: $response")
//                    onUserIdListener?.invoke(response.getString("_id"))
//                } catch (_: Exception) {
//                }
//            }
//        })
    }

    fun emitDeleteMessage(data: JSONObject) {
        mSocket.emit(DELETE_MESSAGE, data)
    }

    private val onStartTyping = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            val json = JSONObject(args[0].toString())
            val receiverId = json.getOrDefault("receiver_id", String())
            val data = TypingData(receiverId = receiverId, isStatus = true)
            onTypingListener?.invoke(data)
        }
    }

    private val onStopTyping = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            val json = JSONObject(args[0].toString())
            val receiverId = json.getOrDefault("receiver_id", String())
            val data = TypingData(receiverId = receiverId, isStatus = false)
            onTypingListener?.invoke(data)
        }
    }

    private val onNewMessage = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            try {
                val json = JSONObject(args[0].toString())
                Log.e("onNewMessage", "onNewMessage: $json")
                val response = MessageDataResponse(
                    _id = json.getOrDefault("_id", String()),
                    unique_id = json.getString("unique_id"),
                    userId = json.getString("userId"),
                    receiverId = json.getString("receiverId"),
                    read_status = json.getInt("read_status"),
                    message = json.getString("message"),
                    status = json.getInt("status"),
                    repliedId = json.getString("repliedId"),
                    replymsg = json.getString("replymsg"),
                    replyImage = json.getString("replyImage"),
                    timezone = json.getOrDefault("timezone", String()),
                    msgType = json.getString("msgType"),
                    link = json.getString("link"),
                    chat_type = json.getOrDefault("chat_type", String()),
                    files = jsonArrayToFileDataList(json.getJSONArray("files")),
                    createdAt = json.getString("createdAt"),
                    updatedAt = json.getString("updatedAt"),
                    originId = json.getOrDefault("userId", String()),
                )
                onNewMessageListener?.invoke(response)
                onRefreshListListener?.invoke(true)
            } catch (_: Exception) {
                Log.e("onError", "onNewMessage: ${args[0]}")
            }
        }
    }

    private fun jsonArrayToFileDataList(jsonArray: JSONArray): MutableList<FileData> {
        val fileDataList = mutableListOf<FileData>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            val url = jsonObject.getString("url")
            val type = jsonObject.getString("type")

            val fileData = FileData(url, type)
            fileDataList.add(fileData)
        }

        return fileDataList
    }

    private val onChatConnect = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            val json = JSONObject(args[0].toString())
            val receiverId = json.getOrDefault("receiver_id", String())
            if (receiverId.isNotEmpty()) {
                val data = JSONObject(args[0].toString()).toDataClass<UserStatusData?>()
                if (data != null) {
                    onUserOnlineListener?.invoke(data.copy(isStatus = true))
                }
            }
        }
    }

    private val onChatDisconnect = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            val json = JSONObject(args[0].toString())
            val receiverId = json.getOrDefault("receiver_id", String())
            if (receiverId.isNotEmpty()) {
                val data = JSONObject(args[0].toString()).toDataClass<UserStatusData?>()
                if (data != null) {
                    onUserOnlineListener?.invoke(data.copy(isStatus = false))
                }
            }
        }
    }

    private val onReadStatus = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            Log.e("TAG", "...ReadStatus... ${args[0]}")
            val data = JSONObject(args[0].toString()).toDataClass<ReadStatusData?>()
            if (data != null) {
                onReadStatusListener?.invoke(data)
                onReadStatusMessageUpdateListener?.invoke(data)
            }
        }
    }

    private val onActiveTime = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            val data = JSONObject(args[0].toString()).toDataClass<ActiveTimeData?>()
            if (data != null) {
                onActiveTimeListener?.invoke(data)
            }
            Log.e("TAG", "...ActiveTime... ${args[0]}")
        }
    }

    private val onReaction = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            Log.e("TAG", "...Reaction... ${args[0]}")
            val data = JSONObject(args[0].toString()).toDataClass<DatabaseReactionData?>()
            if (data != null) {
                onReactionDataListener?.invoke(data)
                onReactionMessageUpdateListener?.invoke(data)
            }
        }
    }

    private val onDeleteMessage = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            Log.e("TAG", "...DeleteMessage... ${args[0]}")
            val data = JSONObject(args[0].toString()).toDataClass<DeleteMessageRequest?>()
            if (data != null) {
                onDeleteMessageListener?.invoke(data)
                onDeleteMessageUpdateListener?.invoke(data)
            }
        }
    }

    private val onConnectionError = Emitter.Listener { args ->
        mActivity.runOnUiThread {
            Log.e("TAG", "...Socket Error... ${args[0]}")
        }
    }

    fun offEvents() {
        mSocket.off(START_TYPING, onStartTyping)
        mSocket.off(STOP_TYPING, onStopTyping)
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError)
    }

    fun offEventsConnection() {
        mSocket.off(CHAT_CONNECT, onChatConnect)
        mSocket.off(CHAT_DISCONNECT, onChatDisconnect)
        mSocket.off(NEW_MESSAGE, onNewMessage)
        mSocket.off(DELETE_MESSAGE, onDeleteMessage)
        mSocket.off(REACTION, onReaction)
        mSocket.off(READ_STATUS, onReadStatus)
    }
}