package com.prng.aeedee_android_chat.view.chat_message

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.prng.aeedee_android_chat.MessageType
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.emojiList
import com.prng.aeedee_android_chat.extractFirstUrl
import com.prng.aeedee_android_chat.getCurrentDateTime
import com.prng.aeedee_android_chat.getTimeZone
import com.prng.aeedee_android_chat.getUniqueId
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.isNetworkConnection
import com.prng.aeedee_android_chat.messageMenuList
import com.prng.aeedee_android_chat.msgDateTimeConvert
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat_message.adapter.EmojiItemsAdapter
import com.prng.aeedee_android_chat.view.chat_message.adapter.MenuItemsAdapter
import com.prng.aeedee_android_chat.view.chat_message.adapter.MessageItemListAdapter
import com.prng.aeedee_android_chat.view.chat_message.model.ActiveTimeData
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadRequest
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageListResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageMenuData
import com.prng.aeedee_android_chat.view.chat_message.model.MessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.MessageResponse
import com.prng.aeedee_android_chat.view.chat_message.model.ReadStatusData
import com.prng.aeedee_android_chat.view.chat_message.model.SendMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.asDatabaseModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageResponse
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData
import com.prng.aeedee_android_chat.visible
import io.github.douglasjunior.androidSimpleTooltip.OverlayView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatViewModel : ViewModel() {

    val chatText = MutableLiveData<String>()
    private val mChatText: LiveData<String> get() = chatText

    private val replyVisibility = MutableLiveData<Boolean>()
    val mReplyVisibility: LiveData<Boolean> get() = replyVisibility

    private val mReplyUserMessage = MutableLiveData<String>()
    val replyUserMessage: LiveData<String> get() = mReplyUserMessage

    private val mReplyUserName = MutableLiveData<String>()
    val replyUserName: LiveData<String> get() = mReplyUserName

    private val mMessageReadStatus = MutableLiveData<Int>()
    private val messageReadStatus: LiveData<Int> get() = mMessageReadStatus

    var photoFile: File? = null
    private val requestImageCode = 1
    private val qAbovePermission = arrayOf(permission.CAMERA, permission.ACCESS_MEDIA_LOCATION)
    private val qBelowPermission = arrayOf(
        permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE
    )

    @SuppressLint("StaticFieldLeak")
    private lateinit var mActivity: Activity

    private var receiverId: String = ""
    private var userId: String = ""
    private val auth =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImdhbmVzaC5pbmZvLmtAZ21haWwuY29tIiwiaWQiOiI2NWIxZjMzN2M2NWQ3ODc0NWRmMTVjYTEiLCJpYXQiOjE3MDgwMDAwMDl9.w7igawY4BRsbQpXrU6t3HuWv1e2lOzNmogZ345SYi9M"

    // List Update Listener
    var onItemClickListListener: ((MessageDataResponse) -> Unit)? = null

    // Api Send Chat Message Listener
    var onSendMessageApiListener: ((Boolean) -> Unit)? = null

    // Typing Status
    var onTypingListener: ((Boolean) -> Unit)? = null

    // User Online Status
    var onUserOnlineListener: ((Boolean) -> Unit)? = null

    // Active Time
    var onActiveTimeListener: ((ActiveTimeData) -> Unit)? = null

    // Read Status
    var onReadStatusListener: ((ReadStatusData) -> Unit)? = null

    // Emoji Updates
    var onEmojiUpdatesListener: ((MessageDataResponse) -> Unit)? = null

    // Reaction
    var onReactionDataListener: ((DatabaseReactionData) -> Unit)? = null

    // Delete Message
    var onDeleteMessageListener: ((DeleteMessageRequest) -> Unit)? = null

    // Message Text
    var onMessageTextListener: ((String) -> Unit)? = null

    // Message Type // normal, forward, reply
    private var msgType: String = MessageType.Normal.name.lowercase(Locale.getDefault())

    // Replied Id
    private var repliedId: String = ""

    // Reply Message
    private var replyMsg: String = ""

    // Reply Image
    private var replyImage: String = ""

    // Unique Id
    var uniqueId: String = ""

    var currentPhotoPath: String? = null

    private var mediaFiles = mutableListOf<FileData>()

    init {
        chatText.value = ""
        replyVisibility.value = false
        mediaFiles = mutableListOf()

        ChatRepository.onNewMessageListener = {
            if (it.receiverId == userID && it.userId == receiverId) {
                onItemClickListListener?.invoke(it)
                if (ChatActivity.isActivity) {
                    val unreadStatus: List<String?> = arrayListOf(it.unique_id ?: it._id)
                    ChatRepository.emitReadStatusListener(sendUnreadData(unreadStatus))
                }
            }
        }

        ChatRepository.onTypingListener = {
            if (it.receiverId == receiverId) {
                onTypingListener?.invoke(it.isStatus)
            }
        }

        ChatRepository.onUserOnlineListener = {
            onUserOnlineListener?.invoke(it)
        }

        ChatRepository.onActiveTimeListener = {
            if (it.receiverId == receiverId) {
                mMessageReadStatus.value = getReadStatus(it.chatStatus)
                onActiveTimeListener?.invoke(it)
            }
        }

        ChatRepository.onReadStatusListener = {
            onReadStatusListener?.invoke(it)
        }

        ChatRepository.onReactionDataListener = {
            onReactionDataListener?.invoke(it)
        }

        ChatRepository.onDeleteMessageListener = {
            onDeleteMessageListener?.invoke(it)
        }
    }

    private fun getReadStatus(chatStatus: String?): Int {
        val cs = chatStatus.toString().replace("_", "")
        return if (cs == "offline") 1 else 2
    }

    fun initData(receiverId: String, userId: String) {
        this.receiverId = receiverId
        this.userId = userId
    }

    // Api Request and Response Chat List Data
    fun getChatUserList(request: MessageRequest): LiveData<MessageListResponse> {
        return ChatActivityRepository.getChatListApiCall(auth, userID, request)
    }

    // Api Request and Response Upload Image
    fun uploadImageApi(request: ImageUploadRequest): LiveData<ImageUploadResponse> {
        return ChatActivityRepository.uploadImageApi(auth, userID, request)
    }

    // Send message Api Request and Response Data
    fun sendChatMessage(): LiveData<MessageResponse> {
        uniqueId = getUniqueId()
        val request = SendMessageRequest(
            receiver_id = receiverId,
            unique_id = uniqueId,
            message = getMessageText(),
            read_status = messageReadStatus.value,
            status = 1,
            link = getFistLink(getMessageText()),
            msgType = msgType,
            replymsg = replyMsg,
            repliedId = repliedId,
            replyImage = replyImage,
            files = mediaFiles,
            createdAt = getCurrentDateTime(),
            updatedAt = getCurrentDateTime(),
            chat_type = getChatType(),
            timezone = getTimeZone(),
        )
        Log.e("TAG", "sendChatMessage: ${Gson().toJson(request)}")
        return ChatActivityRepository.sendMessageApiCall(auth, userId, request)
    }

    private fun getFistLink(messageText: String): String {
        return extractFirstUrl(messageText).toString()
    }

    private fun getChatType(): String {
        return if (mediaFiles.isNotEmpty()) {
            "Image"
        } else {
            "Text"
        }
    }

    // Api Request and Response Delete Message Items
    fun deleteMessageItems(request: DeleteMessageRequest): LiveData<DeleteMessageResponse> {
        return ChatActivityRepository.deleteMessageApiData(auth, userId, request)
    }

    fun initSocket(activity: Activity) {
        mActivity = activity
        ChatRepository.onChatConnect()
    }

    fun emitActiveTime() {
        ChatRepository.emitActiveTime(sendChatConnection(receiverId, userId))
    }

    fun emitReaction(message: String, data: MessageDataResponse) {
        val messageId = data.unique_id?.ifEmpty { data._id }
        ChatRepository.emitReaction(sendReaction(message, messageId.toString()))

        val result = DatabaseReactionData(
            messageId = messageId,
            message = message,
            userId = data.userId,
            receiverId = data.receiverId,
        )
        data.reaction = arrayListOf(result)
        onEmojiUpdatesListener?.invoke(data)
    }

    private var typingJob: Job? = null

    private val typingDelay = 2000L

    fun onTextChanged(char: CharSequence, start: Int, end: Int, count: Int) {
        chatText.value = char.toString()

        typingJob?.cancel()

        startTyping()
        typingJob = CoroutineScope(Dispatchers.Main).launch {
            delay(typingDelay)
            clearTyping()
        }
    }

    fun afterTextChanged(s: Editable) {
        onMessageTextListener?.invoke(s.toString())
        if (s.isEmpty()) {
            clearTyping()
        }
    }

    private fun startTyping() {
        ChatRepository.emitStartStop(true, sendTyping(receiverId, userId))
    }

    fun clearTyping() {
        ChatRepository.emitStartStop(false, sendTyping(receiverId, userId))
    }

    fun setMedias(url: String, type: String, isOnFile: Boolean) {
        val file = FileData(url = url, type = type)
        if (isOnFile) {
            mediaFiles.clear()
            mediaFiles.add(file)
        }
    }

    fun messageType(type: String) {
        msgType = type
    }

    fun messageType(
        repliedId: String, replyMessage: String, files: MutableList<FileData>?, name: String
    ) {
        this.repliedId = repliedId
        this.replyMsg = replyMessage
        this.replyImage = getReplyFiles(files)
        mReplyUserMessage.value = replyMessage
        mReplyUserName.value = name
    }

    private fun getReplyFiles(files: MutableList<FileData>?): String {
        return if (files != null) if (files.isNotEmpty()) files.first().url.toString() else "" else ""
    }

    fun onReplyCloseClickListener() {
        replyVisibility.value = false
        this.repliedId = ""
        this.replyMsg = ""
        this.replyImage = ""
    }

    fun onReplyVisibility() {
        replyVisibility.value = true
    }

    fun onCameraClickListener(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (checkCameraPermission(view.context)) {
                requestPermission(view.context)
            } else {
                cameraPicker()
            }
        } else
        // Marshmallow+
            if (checkPermissionForReadExternalStorage(view.context) || checkWriteExternalStoragePermission(
                    view.context
                ) || checkCameraPermission(view.context)
            ) {
                requestPermission(view.context)
            } else {
                cameraPicker()
            }
    }

    private fun cameraPicker() {
        try {
            // Create a file for the image
            photoFile = createImageFile(mActivity)

            // Get the Uri of the file using FileProvider
            val photoUri = FileProvider.getUriForFile(
                mActivity, mActivity.packageName + ".provider", photoFile!!
            )

            // Create an Intent to launch the camera app
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Set the Uri of the file as the output for the camera app
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            // Launch the camera app
            (mActivity as ChatActivity).takePicture.launch(takePictureIntent)
        } catch (ignored: Exception) {
        }
    }

    var onLaunchGallery: ((Boolean) -> Unit)? = null

    fun onGalleryClickListener(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (checkPermissionForAccessMediaLocation(view.context)) {
                requestPermission(view.context)
            } else {
                onLaunchGallery?.invoke(true)
            }
        } else
        // Marshmallow+
            if (checkPermissionForReadExternalStorage(view.context) || checkWriteExternalStoragePermission(
                    view.context
                )
            ) {
                requestPermission(view.context)
            } else {
                onLaunchGallery?.invoke(true)
            }
    }

    fun onSendClickListener(view: View) {
        if (!isNetworkConnection(view.context)) {

            val message = view.context.resources.getString(R.string.no_internet_connection)
            message.toast(view.context).show()

        } else if (chatText.value!!.trim().isNotEmpty() || mediaFiles.isNotEmpty()) {

            if (!SocketHandler.getSocket().connected()) {

                onSendMessageApiListener?.invoke(true)

            } else {

                ChatRepository.emitSendMessage(sendMessage(receiverId, userId))

                messageEventListener()
            }
        }
    }

    fun messageEventListener(id: String = "") {
        val data = MessageDataResponse(
            _id = id,
            unique_id = this.uniqueId,
            userId = userId,
            receiverId = receiverId,
            read_status = messageReadStatus.value ?: 1,
            message = getMessageText(),
            status = 1,
            link = getFistLink(getMessageText()),
            msgType = msgType,
            repliedId = repliedId,
            replyImage = replyImage,
            replymsg = replyMsg,
            files = mediaFiles,
            createdAt = getCurrentDateTime(),
            updatedAt = getCurrentDateTime(),
            chat_type = getChatType(),
            timezone = getTimeZone(),
            originId = receiverId,
        )

        msgType = MessageType.Normal.name.lowercase(Locale.getDefault())
        repliedId = ""
        replyMsg = ""
        replyImage = ""
        mediaFiles = arrayListOf()
        this.uniqueId = ""

        onItemClickListListener?.invoke(data)

        clearChatText()
    }

    fun emitDeleteMessage(selectedIds: List<String>) {
        ChatRepository.emitDeleteMessage(sendDeleteMessage(selectedIds))
    }

    private fun sendDeleteMessage(selectedIds: List<String>): JSONObject {
        val dJSONObject = JSONObject()
        dJSONObject.put("ids", JSONArray(Gson().toJson(selectedIds)))
        dJSONObject.put("receiver_id", receiverId)
        dJSONObject.put("user_id", userId)
        Log.e("TAG", "delete_message...: $dJSONObject")
        return dJSONObject
    }

    private fun getMessageText(): String {
        if (mediaFiles.isNotEmpty()) {
            val list = mediaFiles.filter { it.type == "image" }.map { it.type }
            if (list.isNotEmpty()) {
                return "Image"
            }
        }
        return mChatText.value ?: ""
    }

    private fun clearChatText() {
        chatText.value = ""
        onReplyCloseClickListener()
    }

    private fun sendTyping(receiverId: String, userId: String): JSONObject {
        val tJSONObject = JSONObject()
        tJSONObject.put("receiver_id", receiverId)
        tJSONObject.put("user_id", userId)
        Log.e("TAG", "typing...: $tJSONObject")
        return tJSONObject
    }

    private fun sendMessage(receiverId: String, userId: String): JSONObject {
        uniqueId = getUniqueId()

        val message = getMessageText()
        val sJSONObject = JSONObject()
        sJSONObject.put("receiver_id", receiverId)
        sJSONObject.put("user_id", userId)
        sJSONObject.put("read_status", 1)
        sJSONObject.put("unique_id", uniqueId)
        sJSONObject.put("message", message)
        sJSONObject.put("status", 1)
        sJSONObject.put("link", getFistLink(getMessageText()))
        sJSONObject.put("files", JSONArray(Gson().toJson(mediaFiles)))
        sJSONObject.put("msgType", msgType)
        sJSONObject.put("repliedId", repliedId)
        sJSONObject.put("replyImage", replyImage)
        sJSONObject.put("replymsg", replyMsg)
        sJSONObject.put("chat_type", getChatType())
        sJSONObject.put("timezone", getTimeZone())
        sJSONObject.put("createdAt", getCurrentDateTime())
        sJSONObject.put("updatedAt", getCurrentDateTime())

        Log.e("TAG", "messages...: $sJSONObject")
        return sJSONObject
    }

    private fun sendChatConnection(receiverId: String, userId: String): JSONObject {
        val cJSONObject = JSONObject()
        cJSONObject.put("receiver_id", receiverId)
        cJSONObject.put("user_id", userId)
        Log.e("TAG", "activeTime...: $cJSONObject")
        return cJSONObject
    }

    private fun sendReaction(message: String, messageId: String): JSONObject {
        val rJSONObject = JSONObject()
        rJSONObject.put("receiver_id", receiverId)
        rJSONObject.put("user_id", userId)
        rJSONObject.put("message", message)
        rJSONObject.put("messageId", messageId)
        Log.e("TAG", "reaction...: $rJSONObject")
        return rJSONObject
    }

    suspend fun addDateTime(list: List<MessageDataResponse>): List<MessageDataResponse> =
        suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.Default).launch {
                val groupedByDate = list.groupBy { data -> data.createdAt.substring(0, 10) }
                val processedList = groupedByDate.flatMap { (_, group) ->
                    group.mapIndexed { index, data ->
                        if (index == 0) {
                            if (msgDateTimeConvert(data.createdAt) == getCurrentDate()) {
                                data.copy(dateTime = "Today")
                            } else {
                                data.copy(dateTime = msgDateTimeConvert(data.createdAt))
                            }
                        } else {
                            data
                        }
                    }
                }
                continuation.resume(processedList)
            }
        }

    suspend fun addDateTime(data: MessageDataResponse, prevDate: String?): MessageDataResponse =
        suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.Default).launch {
                if (prevDate != null) {
                    if (getCurrentDate() != msgDateTimeConvert(prevDate)) {
                        if (data.dateTime == getCurrentDate()) {
                            data.dateTime = "Today"
                        } else {
                            data.dateTime = msgDateTimeConvert(data.createdAt)
                        }
                    }
                } else {
                    if (data.dateTime == getCurrentDate()) {
                        data.dateTime = "Today"
                    } else {
                        data.dateTime = msgDateTimeConvert(data.createdAt)
                    }
                }
                continuation.resume(data)
            }
        }

    private fun getCurrentDate(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    var onEmojiUpdateListener: ((String, MessageDataResponse) -> Unit)? = null

    var onMessageMenuListener: ((MessageMenuData, MessageDataResponse) -> Unit)? = null

    @SuppressLint("SetTextI18n", "InflateParams")
    fun showPopup(data: MessageDataResponse, anchorView: View) {
        val isLeft = data.userId != ChatActivity.userId
        val direction = if (isLeft) Gravity.END else Gravity.START
        val popupMenu =
            SimpleTooltip.Builder(mActivity).anchorView(anchorView).dismissOnInsideTouch(false)
                .dismissOnOutsideTouch(true).text("Emoji Popup")
                .contentView(R.layout.emoji_popup_layout)
                .animationPadding(SimpleTooltipUtils.pxFromDp(0F)).showArrow(false)
                .ignoreOverlay(false).gravity(direction).animated(false).overlayOffset(13F)
                .transparentOverlay(false)
                .highlightShape(OverlayView.HIGHLIGHT_SHAPE_RECTANGULAR_ROUNDED).build()

        val rvEmojiList = popupMenu.findViewById<RecyclerView>(R.id.rvEmojiList)
        val rvMenuData = popupMenu.findViewById<RecyclerView>(R.id.rvMenuList)
        val cvEmoji = popupMenu.findViewById<ConstraintLayout>(R.id.cvEmoji)

        val mAdapter = EmojiItemsAdapter()
        rvEmojiList.setHasFixedSize(true)
        rvEmojiList.adapter = mAdapter

        if (isLeft) cvEmoji.visible() else cvEmoji.gone()

        mAdapter.onClickListener = {
            onEmojiUpdateListener?.invoke(it, data)
            popupMenu.dismiss()
        }

        val mMenuAdapter = MenuItemsAdapter()
        rvMenuData.setHasFixedSize(true)
        rvMenuData.adapter = mMenuAdapter

        mMenuAdapter.onClickListener = {
            onMessageMenuListener?.invoke(it, data)
            popupMenu.dismiss()
        }

        mAdapter.setData(emojiList)
        mAdapter.notifyDataSetChanged()

        mMenuAdapter.setData(messageMenuList)
        mMenuAdapter.notifyDataSetChanged()

        popupMenu.show()
    }

    private fun requestPermission(context: Context) {
        val activity = context as ChatActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ActivityCompat.requestPermissions(
            activity, qAbovePermission, requestImageCode
        )
        else ActivityCompat.requestPermissions(activity, qBelowPermission, requestImageCode)
    }

    private fun checkCameraPermission(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(context, permission.CAMERA)
        return result != PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionForAccessMediaLocation(context: Context): Boolean {
        var result = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result = ContextCompat.checkSelfPermission(context, permission.ACCESS_MEDIA_LOCATION)
        }
        return result != PackageManager.PERMISSION_GRANTED
    }

    private fun checkWriteExternalStoragePermission(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(context, permission.WRITE_EXTERNAL_STORAGE)
        return result != PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionForReadExternalStorage(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(context, permission.READ_EXTERNAL_STORAGE)
        return result != PackageManager.PERMISSION_GRANTED
    }

    @Throws(IOException::class)
    private fun createImageFile(activity: Activity): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)

        currentPhotoPath = image.absolutePath
        return image
    }

    fun getItemIndex(data: ArrayList<MessageDataResponse>, id: String): Int {
        return data.indexOfFirst { it.unique_id == id }
    }

    fun updateReadStatus(list: ArrayList<MessageDataResponse>?) {
        if (list != null) {
            if (list.isNotEmpty()) {
                var unreadStatus =
                    list.filter { /*it.status != 0 &&*/ it.read_status != 3 && it.unique_id != null }
                        .map { it.unique_id }
                unreadStatus = unreadStatus.filter { it.toString().isNotEmpty() }.map { it }
                if (unreadStatus.isNotEmpty()) {
                    ChatRepository.emitReadStatusListener(sendUnreadData(unreadStatus))
                }
            }
        }
    }

    private fun sendUnreadData(unreadStatus: List<String?>?): JSONObject {
        if (unreadStatus == null) return JSONObject()
        val uJSONObject = JSONObject()
        val jsonArray = JSONArray(unreadStatus)
        uJSONObject.put("ids", jsonArray)
        uJSONObject.put("read_status", 3)
        uJSONObject.put("receiver_id", receiverId)
        uJSONObject.put("userId", userId)
        Log.e("TAG", "unreadStatus...: $uJSONObject")
        return uJSONObject
    }

    fun updateLists(
        list: List<MessageDataResponse>,
        ids: MutableList<String>,
        isClear: Boolean,
        activity: Activity,
        adapter: MessageItemListAdapter,
        onResult: (List<MessageDataResponse>) -> Unit
    ) {
        ChatActivityRepository.updateLists(list, ids, isClear, adapter, activity, onResult)
    }

    fun updateChildEntityInParent(chatDao: ChatDao, childId: String, ifData: Int) =
        CoroutineScope(Dispatchers.IO).launch {

            val parentWithChildren = chatDao.getParentWithChildren(receiverId)

            if (parentWithChildren != null) {
                if (parentWithChildren.parent?.response != null) {

                    val childToUpdate: MutableList<DatabaseMessageModel> =
                        parentWithChildren.parent.response.map {
                            if (ifData == 3) it.readStatus = 3
                            chatDao.updateChildren(it)
                            it
                        }.toMutableList()

                    if (childToUpdate != null) {
                        val index =
                            parentWithChildren.parent.response.indexOfFirst { it.uniqueId == childId }
                        val childrenList: MutableList<DatabaseMessageModel>
                        if (index > -1) {
                            val parentToUpdate = parentWithChildren.parent
                            childrenList = childToUpdate
                            parentToUpdate.let {
                                it.receiverId = receiverId
                                it.response = childrenList
                                chatDao.updateParent(it)
                            }
                        }
                    }
                }
            }
        }

    fun addNewItemToList(uniqueId: String, newItem: MessageDataResponse, chatDao: ChatDao) {
        CoroutineScope(Dispatchers.IO).launch {

            val parentWithChildren = chatDao.getParentWithChildren(receiverId)

            if (parentWithChildren != null) {
                if (parentWithChildren.parent != null) {
                    val childToUpdate =
                        parentWithChildren.parent.response.find { it.uniqueId == uniqueId }

                    if (childToUpdate != null) {
                        chatDao.updateChildren(arrayListOf(newItem).asDatabaseModel().first())

                        val index =
                            parentWithChildren.parent.response.indexOfFirst { it.uniqueId == uniqueId }
                        val childrenList = parentWithChildren.parent.response.toMutableList()
                        if (index > -1) {
                            val parentToUpdate = parentWithChildren.parent
                            childrenList[index] = arrayListOf(newItem).asDatabaseModel().first()
                            parentToUpdate.let {
                                it.receiverId = receiverId
                                it.response = childrenList
                                chatDao.updateParentWithChildren(it, childrenList)
                            }
                        }
                    } else {
                        chatDao.updateChildren(arrayListOf(newItem).asDatabaseModel().first())
                        val childrenList = parentWithChildren.parent.response.toMutableList()
                        val parentToUpdate = parentWithChildren.parent
                        childrenList.add(arrayListOf(newItem).asDatabaseModel().first())
                        parentToUpdate.let {
                            it.receiverId = receiverId
                            it.response = childrenList
                            chatDao.updateParent(it)
                            val index =
                                parentWithChildren.parent.response.indexOfFirst { i -> i.uniqueId == uniqueId }
                            if (index > -1) Log.e(
                                "addNewItemToList",
                                "addNewItemToList--------------$uniqueId"
                            )
                        }
                    }
                }
            }

        }
    }
}