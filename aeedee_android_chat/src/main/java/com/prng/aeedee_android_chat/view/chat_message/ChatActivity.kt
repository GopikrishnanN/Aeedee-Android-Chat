package com.prng.aeedee_android_chat.view.chat_message

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.prng.aeedee_android_chat.MessageType
import com.prng.aeedee_android_chat.Payload
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.copyToClipboard
import com.prng.aeedee_android_chat.databinding.ActivityChatBinding
import com.prng.aeedee_android_chat.databinding.UpdateEmojiReactionLayoutBinding
import com.prng.aeedee_android_chat.databinding.UploadLoaderLayoutBinding
import com.prng.aeedee_android_chat.getMimeType
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.hideKeyboardFrom
import com.prng.aeedee_android_chat.invisible
import com.prng.aeedee_android_chat.isNetworkConnection
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.deo.ChatDatabase
import com.prng.aeedee_android_chat.roomdb.di.DatabaseModule
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageData
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.roomdb.entity_model.asDatabaseModel
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.userName
import com.prng.aeedee_android_chat.util.CustomDialog
import com.prng.aeedee_android_chat.util.FunctionScheduler
import com.prng.aeedee_android_chat.util.UCropContract
import com.prng.aeedee_android_chat.util.UCropInput
import com.prng.aeedee_android_chat.util.UCropResult
import com.prng.aeedee_android_chat.util.UserIdData
import com.prng.aeedee_android_chat.view.chat_message.adapter.MessageItemListAdapter
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadRequest
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataUsers
import com.prng.aeedee_android_chat.view.chat_message.model.MessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.asDatabaseModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.forward_chat.ForwardUsersActivity
import com.prng.aeedee_android_chat.visible
import com.prng.aeedee_android_chat.wrapContent
import com.vanniktech.emoji.EmojiPopup
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatActivity : AppCompatActivity() {
    private lateinit var mActivityBinding: ActivityChatBinding
    private val mViewModel: ChatViewModel by viewModels()
    private lateinit var mAdapter: MessageItemListAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var mKeyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    companion object {
        // User Id
        var userId: String = ""
        private var receiverId: String = ""

        var isFirstLocalDb = true
        var isActivity = true

        @SuppressLint("StaticFieldLeak")
        var mActivity: Activity? = null
    }

    private var name: String = ""
    private var avatar: String = ""
    private var code: String = ""
    private var number: String = ""

    private var mResponse: ArrayList<MessageDataResponse>? = arrayListOf()
    private var lastId: String = ""
    private var totalCount: Int = 0
    private var lastVisiblePosition = RecyclerView.NO_POSITION

    private lateinit var database: ChatDatabase
    private lateinit var chatDao: ChatDao

    private var isPagination = false
    private var isFirstTime = true

    private var isSocket = false
    private var isRecent = false
    private var isLocalData = false
    private var isEmoji = false

    private lateinit var scheduler: FunctionScheduler

    private lateinit var lDialog: CustomDialog<UploadLoaderLayoutBinding>

    private lateinit var eDialog: CustomDialog<UpdateEmojiReactionLayoutBinding>

    private lateinit var popup: EmojiPopup

    private var recentCount = 0
    private var readStatusEmitCount = 0

    val takePicture: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (mViewModel.photoFile != null) {
                mViewModel.currentPhotoPath = mViewModel.photoFile!!.absolutePath
                setPicture(true)
            }
        }
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    Log.d("PhotoPicker", "Media selected: $uri")
                    mViewModel.currentPhotoPath = uri.toString()
                    setPicture(false)
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            } else {
                Log.d("PhotoPicker", "Activity result not OK")
            }
        }

    private val pickMediaLatest =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Media selected: $uri")
                mViewModel.currentPhotoPath = uri.toString()
                setPicture(false)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    private val cropImage = registerForActivityResult(UCropContract()) { result: UCropResult ->
        if (result.resultCode == RESULT_OK) {
            result.uri?.let { uri ->
                uri.path?.let { path ->
                    uploadImageApi(path)
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            result.error?.let { throwable ->
                Log.e("CropImage", "Error: ${throwable.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityBinding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(mActivityBinding.root)

        initIntent()

        database = DatabaseModule.provideAppDatabase(this)
        chatDao = DatabaseModule.provideChannelDao(database)

        mActivityBinding.lifecycleOwner = this
        mActivityBinding.viewModel = mViewModel

        mAdapter = MessageItemListAdapter()
        mActivityBinding.rvChatMessageList.adapter = mAdapter

        mAdapter.setUserData(
            UserIdData(userId = receiverId, userName = name, oppositeUserName = userName)
        )

        mActivity = this
        isFirstLocalDb = true
        readStatusEmitCount = 0

        val messages: LiveData<List<MessageDataUsers>?> =
            chatDao.getMessageAll().map { it?.asDatabaseModel() }//.distinctUntilChanged()

        messages.observe(this) {
            if (!isSocket)
                if (!it.isNullOrEmpty()) {
                    val list = it.filter { data -> data.receiverId == receiverId }
                        .map { data -> data.response.asDatabaseModel() }
                    if (list.isNotEmpty()) {
                        isLocalData = true
                        setUiData(list.first())

                        if (isFirstLocalDb) {
                            isFirstLocalDb = false
                            isRecent = true
                            val lastMessageId =
                                if (mResponse != null && mResponse!!.isNotEmpty()) mResponse!!.last().unique_id.toString() else ""
                            if (lastMessageId.isNotEmpty()) {
                                fetchMessageApi(lastMessageId, 1)
                            }
                        }
                    } else {
                        setUiData(emptyList())
                        mActivityBinding.pbProgress.visible()
                        mActivityBinding.svNoMessageIcon.gone()
                        isFirstLocalDb = false
                        fetchMessageApi(lastId, 0)
                    }
                } else {
                    mActivityBinding.pbProgress.visible()
                    mActivityBinding.svNoMessageIcon.gone()
                    isFirstLocalDb = false
                    fetchMessageApi(lastId, 0)
                }
        }

        mViewModel.initData(receiverId, userId)

        mViewModel.initSocket(this@ChatActivity)

        emitActiveTimeLoop()

        emojiKeyboard()

        layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = false
        }
        mActivityBinding.rvChatMessageList.setHasFixedSize(true)
        mActivityBinding.rvChatMessageList.layoutManager = layoutManager

        typingLoading(false)

        val drawableResId = R.drawable.ic_delete_icon
        setDrawableStartWithSize(mActivityBinding.atvDeleteText, drawableResId)

        Handler(Looper.myLooper()!!).postDelayed({
            mActivityBinding.rvChatMessageList.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isPagination) {
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        if (firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition != lastVisiblePosition) {
                            lastVisiblePosition = firstVisibleItemPosition
                            if (firstVisibleItemPosition == 0 && totalCount >= 50) {
                                isPagination = false
                                isFirstTime = false
                                fetchMessageApi(lastId, 0)
                            }
                        }
                    }
                }
            })
        }, 500)

        mAdapter.onLongClickListener = { data, view ->
            mViewModel.showPopup(data, view)
        }

        mAdapter.onSelectionClickListener = {
            deleteMessageSelection(it)
        }

        mViewModel.onEmojiUpdateListener = { reaction, data ->
            if (reaction != "+") mViewModel.emitReaction(reaction, data)
        }

        mViewModel.onMessageMenuListener = { menu, data ->
            when (menu.id) {
                0 -> {
                    // - - Reply - -
                    mViewModel.messageType(
                        data.unique_id.toString(), data.message, data.files, name
                    )
                    mViewModel.messageType(MessageType.Reply.name)
                    mViewModel.onReplyVisibility()
                }

                1 -> {
                    // - - Copy - -
                    copyToClipboard(data.message, applicationContext)
                }

                2 -> {
                    // - - Forward - -
                    val forwardIntent = Intent(applicationContext, ForwardUsersActivity::class.java)
                    forwardIntent.putExtra("data", data)
                    startActivity(forwardIntent)
                }

                3 -> {
                    // - - Delete - -
                    mAdapter.isSelection = true
                    deleteSelection(data)
                }
            }
        }

        mViewModel.onItemClickListListener = {
            CoroutineScope(Dispatchers.IO).launch {
                isSocket = true

                try {
                    val idAlready =
                        mAdapter.getAllItems().filter { d -> d.unique_id == it.unique_id }
                            .map { it.unique_id }
                    if (idAlready.isNotEmpty()) {
                        return@launch
                    }

                    mViewModel.addNewItemToList(it.unique_id.toString(), it, chatDao)

                    val processedData: MessageDataResponse =
                        if (mAdapter.getAllItems().isNotEmpty()) {
                            mViewModel.addDateTime(it, mAdapter.getAllItems().last().createdAt)
                        } else {
                            mViewModel.addDateTime(it, null)
                        }

                    mAdapter.addData(processedData)
                    mActivityBinding.rvChatMessageList.visible()
                    mActivityBinding.svNoMessageIcon.gone()

                    setDbNewMessage()

                    val recyclerViewState = onSaveInstanceRV()
                    runOnUiThread {
                        if (mAdapter.getAllItems().size == 1) {
                            mAdapter.notifyDataSetChanged()
                        } else {
                            if ((mAdapter.getAllItems().size - 1) > -1)
                                mAdapter.notifyItemChanged(
                                    (mAdapter.getAllItems().size - 1), Payload.Update.name
                                )
                        }
                    }
                    onRestoreInstance(recyclerViewState)

                    scrollList()
                } catch (_: Exception) {
                }
            }
        }

        mViewModel.onSendMessageApiListener = {
            sendChatMessage()
        }

        mViewModel.onLaunchGallery = {
            if (it) {
                imagePicker()
            }
        }

        mViewModel.onTypingListener = {
            typingLoading(it)
        }

        mViewModel.onUserOnlineListener = {
            mViewModel.updateReadStatus(mAdapter, chatDao)
        }

        mViewModel.onFocusableListener = {
            if (it) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mActivityBinding.clChatBox.isFocusable = true
                    mActivityBinding.clChatBox.isFocusableInTouchMode = true
                }
            }
        }

        mActivityBinding.aivBack.setOnClickListener {
            finish()
        }

        mViewModel.onActiveTimeListener = {
            val chatStatus = it.chatStatus.toString().replace("_", "")
            Log.e("onActiveTimeListener", "onActiveTimeListener: $chatStatus")
            mActivityBinding.atvMessage.text = chatStatus
            mActivityBinding.atvMessage.invalidate()
            if (chatStatus == "offline") {
                val color = ContextCompat.getColor(applicationContext, R.color.black)
                mActivityBinding.atvMessage.setTextColor(color)
            } else {
                val color = ContextCompat.getColor(applicationContext, R.color.blue)
                mActivityBinding.atvMessage.setTextColor(color)
            }
        }

        mKeyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            mActivityBinding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight = mActivityBinding.root.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                mActivityBinding.aivCamera.gone()
                mActivityBinding.aivGallery.gone()
            } else {
                mActivityBinding.aivCamera.visible()
                mActivityBinding.aivGallery.visible()
                if (mActivityBinding.aetEditMessage.text!!.isEmpty()) {
                    mActivityBinding.aetEditMessage.clearFocus()
                }
            }
        }
        mActivityBinding.root.viewTreeObserver.addOnGlobalLayoutListener(mKeyboardListener)

        mViewModel.onReadStatusListener = {
            CoroutineScope(Dispatchers.IO).launch {
                if (it.ids != null) {
                    if (it.ids.isNotEmpty()) {
                        val list = mAdapter.getAllItems()
                        it.ids.forEach { id ->
                            if (list.isNotEmpty()) {
                                val position = mViewModel.getItemIndex(list as ArrayList, id)
                                if (position > -1) {

                                    mResponse =
                                        list.map { rs -> rs.copy(read_status = 3) } as ArrayList<MessageDataResponse>

                                    mAdapter.setData(mResponse!!)

                                    updateReadStatus(position)

                                    mViewModel.updateChildEntityInParent(chatDao, id, 3)
                                }
                            }
                        }
                    }
                }
            }
        }

        mViewModel.onEmojiUpdatesListener = {
            isEmoji = true
            emojiSelection(data = it)
        }

        mViewModel.onReactionDataListener = {
            isEmoji = true
            emojiListener(data = it)
        }

        mViewModel.onDeleteMessageListener = { data ->
            mAdapter.getAllItems().let {
                loader(2)
                mViewModel.updateLists(
                    it, data.ids!!.toMutableList(), this, mAdapter, chatDao, true
                ) {
                    dismiss()
                    deleteMessageSelection(false)
//                    setDbResponse(list, false)
                }
            }
        }

        mViewModel.onMessageTextListener = {
            if (it.trim().isNotEmpty()) {
                mActivityBinding.aivSend.setColorFilter(
                    ContextCompat.getColor(applicationContext, R.color.blue),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                mActivityBinding.aivSend.setColorFilter(
                    ContextCompat.getColor(applicationContext, R.color.black_gray),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        mActivityBinding.clDeleteChat.setOnClickListener {
            val selectedIds = mAdapter.getSelectedIds().toMutableList()
            mViewModel.emitDeleteMessage(selectedIds)
            loader(2)
            mViewModel.updateLists(
                mAdapter.getAllItems(), selectedIds, this, mAdapter, chatDao, true
            ) {
                dismiss()
                deleteMessageSelection(false)
//                setDbResponse(it, false)
            }
        }

        mActivityBinding.atvCancelSelection.setOnClickListener {
            val list = mAdapter.getSelectedIds().toMutableList()
            mViewModel.updateLists(mAdapter.getAllItems(), list, this, mAdapter, chatDao, false) {
                deleteMessageSelection(false)
            }
        }
    }

    private fun setDbNewMessage() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = mAdapter.getAllItems() as ArrayList
            val (parentData, childData) = getParentData(list)
            chatDao.updateParentWithChildren(parentData, childData)
        }
    }

    private fun updateReadStatus(position: Int) {
        if (mAdapter.getAllItems() != null)
            if (mAdapter.getAllItems().size > position) {
                mAdapter.updateData(position, mAdapter.getAllItems()[position])
                runOnUiThread { mAdapter.notifyItemChanged(position, Payload.Update.name) }
            }
    }

    private fun emojiKeyboard() {
        popup = EmojiPopup.Builder.fromRootView(mActivityBinding.aetEditMessage)
            .build(mActivityBinding.aetEditMessage)
        mActivityBinding.aivEmojiKB.setImageResource(R.drawable.ic_emoji_smile_icon)

        mActivityBinding.aivEmojiKB.setOnClickListener {
            popup.toggle()
            mActivityBinding.aivEmojiKB.postDelayed({
                if (popup.isShowing) mActivityBinding.aivEmojiKB.setImageResource(R.drawable.ic_keyboard_emoji_icon)
                else mActivityBinding.aivEmojiKB.setImageResource(R.drawable.ic_emoji_smile_icon)
            }, 100)
        }
    }

    private fun emojiListener(data: DatabaseReactionData) {
        CoroutineScope(Dispatchers.IO).launch {
            val list = mAdapter.getAllItems()
            val updateData = list.filter { it.unique_id == data.messageId }.map { it }
            if (updateData.isNotEmpty()) {
                val index = list.indexOfFirst { it.unique_id == data.messageId }
                list[index].reaction = arrayListOf(data)
                mAdapter.selectMessage(list[index], index)
                runOnUiThread {
                    mAdapter.notifyItemChanged(index, Payload.Update.name)
                }
            }
            Log.e("TAG", "emojiListener: ${list.size}")
//            setDbResponse(mResponse!!)
        }
    }

    private fun deleteMessageSelection(isSelected: Boolean) {
        runOnUiThread {
            if (isSelected) {
                mActivityBinding.clMessageLayout.gone()
                if (mActivityBinding.aetEditMessage.text!!.isNotEmpty())
                    mActivityBinding.aetEditMessage.setText("")
                hideKeyboardFrom(applicationContext, mActivityBinding.aetEditMessage)
                mActivityBinding.clDeleteChat.visible()
                mActivityBinding.atvCancelSelection.visible()
            } else {
                mActivityBinding.clMessageLayout.visible()
                mActivityBinding.clDeleteChat.gone()
                mActivityBinding.atvCancelSelection.gone()
            }
        }
    }

    private fun setDrawableStartWithSize(tv: AppCompatTextView, drawableResId: Int) {
        val drawable = AppCompatResources.getDrawable(tv.context, drawableResId)
        drawable?.setBounds(0, 0, 80, 80)
        tv.setCompoundDrawablesRelative(drawable, null, null, null)
    }

    private fun deleteSelection(data: MessageDataResponse) {
        CoroutineScope(Dispatchers.IO).launch {
            val list = mAdapter.getAllItems()
            for (i in list.indices) {
                if (list[i].unique_id == data.unique_id) {
                    list[i].isSelectEnable = true
                    mAdapter.selectMessage(list[i], i)
                    runOnUiThread {
                        mAdapter.notifyItemChanged(i, Payload.Update.name)
                    }
                    deleteMessageSelection(true)
                }
            }
        }
    }

    private fun emojiSelection(data: MessageDataResponse) {
        CoroutineScope(Dispatchers.IO).launch {
            val list = mAdapter.getAllItems()
            val updateData =
                mAdapter.getAllItems().filter { it.unique_id == data.unique_id }.map { it }
            if (updateData.isNotEmpty()) {
                val index = list.indexOfFirst { it.unique_id == data.unique_id }
                list[index].reaction = data.reaction
                mAdapter.selectMessage(list[index], index)
                runOnUiThread {
                    mAdapter.notifyItemChanged(index, Payload.Update.name)
                }
            }
            setDbResponse(list, false)
        }
    }

    private fun fetchMessageApi(lastId: String, recent: Int) {
        val request =
            MessageRequest(receiverId = receiverId, lastId = lastId, limit = 50, recent = recent)
        fetchMessageApiList(request)
    }

    private fun onSaveInstanceRV(): Parcelable? {
        return mActivityBinding.rvChatMessageList.layoutManager?.onSaveInstanceState()
    }

    private fun onRestoreInstance(p: Parcelable?) {
        mActivityBinding.rvChatMessageList.post {
            mActivityBinding.rvChatMessageList.layoutManager?.onRestoreInstanceState(p)
        }
    }

    private fun emitActiveTimeLoop() {
        if ((SocketHandler.getSocket().connected()))
            scheduler = FunctionScheduler {
                mViewModel.emitActiveTime()
            }
    }

    private fun initIntent() {
        isActivity = true
        intent?.let {
            if (it.hasExtra("userId")) userId = it.getStringExtra("userId")!!
            if (it.hasExtra("receiverId")) receiverId = it.getStringExtra("receiverId")!!
            if (it.hasExtra("avatar")) avatar = it.getStringExtra("avatar")!!
            if (it.hasExtra("name")) name = it.getStringExtra("name")!!
            if (it.hasExtra("code")) code = it.getStringExtra("code")!!
            if (it.hasExtra("number")) number = it.getStringExtra("number")!!

            appBarData()
        }
    }

    private fun appBarData() {

        mActivityBinding.atvName.text = name

        Glide.with(mActivityBinding.root.context).load(avatar).apply(
            RequestOptions().placeholder(R.drawable.ic_placeholder_icon)
                .error(R.drawable.ic_placeholder_icon).fallback(R.drawable.ic_placeholder_icon)
                .centerCrop()
        ).into(mActivityBinding.aivProfileImage)
    }

    private fun fetchMessageApiList(request: MessageRequest) {

        if (!isNetworkConnection(applicationContext)) {
            val message = resources.getString(R.string.no_internet_connection)
            message.toast(applicationContext).show()
        }

        mViewModel.getChatUserList(request).observe(this) {
            if (it != null) {
                mActivityBinding.pbProgress.gone()
                isSocket = false
                if (it.lastId != null)
                    if (it.lastId.isNotEmpty()) {
                        lastId = it.lastId
                        totalCount = it.count
                    } else {
                        lastId = ""
                        totalCount = 0
                    }

                if (isFirstLocalDb) {
                    setDbResponse(it.response)
                } else {
//                    if (isRecent) {
                    if (it.response.size > 49)
                        isFirstLocalDb = true
//                    }
                    setDbResponse(it.response, true)
                }
            }
        }
    }

    //  chatDao.insertMessageDataUsers(dbUserMessage)
    private fun setDbResponse(response: List<MessageDataResponse>, isApi: Boolean) {
        lifecycleScope.launch {
            if (response != null)
                if (response.isNotEmpty()) {
                    if (mResponse!!.isEmpty()) {
                        val (parentData, childData) = getParentData(response)
                        chatDao.updateParentWithChildren(parentData, childData)
                    } else {
                        if (!isFirstTime && isApi) {
                            mResponse?.addAll(0, response)
                            val (parentData, childData) = getParentData(mResponse!!)
                            chatDao.updateParentWithChildren(parentData, childData)
                        } else if (isRecent && isApi) {
                            recentCount = response.size
                            mResponse?.addAll(response)
                            val (parentData, childData) = getParentData(mResponse!!)
                            chatDao.updateParentWithChildren(parentData, childData)
                        } else {
                            val (parentData, childData) = getParentData(mResponse!!)
                            chatDao.updateParentWithChildren(parentData, childData)
                        }
                    }
                } else if (mResponse!!.isEmpty()) {
                    mActivityBinding.svNoMessageIcon.visible()
                }
        }
    }

    private suspend fun getParentData(response: List<MessageDataResponse>): Pair<DatabaseMessageData, List<DatabaseMessageModel>> =
        suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                val updatedItems = updateOriginId(response, newOriginId = receiverId)
                val parentData = DatabaseMessageData(
                    receiverId = receiverId,
                    response = updatedItems.asDatabaseModel(),
                    _id = receiverId
                )
                continuation.resume(Pair(parentData, updatedItems.asDatabaseModel()))
            }
        }

    private suspend fun updateOriginId(
        items: List<MessageDataResponse>, newOriginId: String
    ): List<MessageDataResponse> = suspendCoroutine { continuation ->
        CoroutineScope(Dispatchers.IO).launch {

            val list = items.map { item ->
                item.copy(originId = newOriginId)
            }

            continuation.resume(removeDuplicate(list))
        }
    }

    private suspend fun removeDuplicate(list: List<MessageDataResponse>): List<MessageDataResponse> =
        suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                val idSet: HashSet<String> = HashSet()
                val uniqueDataList: ArrayList<MessageDataResponse> = arrayListOf()

                for (data in list) {
                    if (idSet.add(data.unique_id.toString())) {
                        uniqueDataList.add(data)
                    }
                }

                continuation.resume(uniqueDataList)
            }
        }

    private fun setDbResponse(response: List<MessageDataResponse>) {
        lifecycleScope.launch {
            val (parentData, childData) = getParentData(response)
            chatDao.updateParentWithChildren(parentData, childData)
        }
    }

    private fun setUiData(responses: List<MessageDataResponse>) {
        mActivityBinding.svNoMessageIcon.gone()
        if (responses.isNotEmpty()) {
            Log.e("TAG", "setUiData: ${mResponse!!.size}")
            if (mResponse!!.isEmpty()) {
                mResponse?.clear()
                runBlocking {
                    mActivityBinding.rvChatMessageList.invisible()
                    val processedList = mViewModel.addDateTime(responses)
                    mResponse?.addAll(processedList)
                    mAdapter.setData(mResponse!!)
                    mAdapter.notifyDataSetChanged()
                    scrollList()
                    Handler(Looper.myLooper()!!).postDelayed({
                        isPagination = true
                    }, 1000)
                }
            } else {
                runBlocking {
                    mActivityBinding.rvChatMessageList.invisible()
                    if (isEmoji) {
                        isEmoji = false
                        Handler(Looper.myLooper()!!).postDelayed({
                            isPagination = true
                        }, 1000)
                    } else if (isRecent) {
                        val processedList = mViewModel.addDateTime(responses)
                        mResponse = processedList as ArrayList<MessageDataResponse>
                        mAdapter.setData(mResponse!!)
                        mAdapter.notifyItemRangeChanged(
                            ((mResponse!!.size - recentCount) - 1), mResponse!!.size
                        )
                        recentCount = 0
                        isRecent = false
                        scrollList()
                        Handler(Looper.myLooper()!!).postDelayed({
                            isPagination = true
                        }, 1000)
                    } else {
                        val processedList = mViewModel.addDateTime(responses)
                        mResponse = processedList as ArrayList<MessageDataResponse>
                        mAdapter.setData(mResponse!!)
                        mAdapter.notifyDataSetChanged()
                        Handler(Looper.myLooper()!!).postDelayed({
                            isPagination = true
                        }, 1000)
                    }
                }
                mActivityBinding.rvChatMessageList.visible()
            }
//            if (readStatusEmitCount < 2) {
//            if (mAdapter.getAllItems().isNotEmpty()) {
//                    readStatusEmitCount += 1
//                mViewModel.updateReadStatus(mAdapter.getAllItems() as ArrayList<MessageDataResponse>)
//                }
//            }
        } else {
            if (mResponse!!.isEmpty()) {
                mActivityBinding.svNoMessageIcon.visible()
            }
        }
    }

    private fun scrollList() {
        mActivityBinding.rvChatMessageList.visible()
        mActivityBinding.rvChatMessageList.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mActivityBinding.rvChatMessageList.viewTreeObserver.removeOnGlobalLayoutListener(
                    this
                )

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val lastPosition = mAdapter.itemCount - 1

                        val lastItemView = layoutManager.findViewByPosition(lastPosition)
                        if (lastItemView != null) {
                            val itemHeight = lastItemView.height
                            layoutManager.scrollToPositionWithOffset(lastPosition, -itemHeight)
                        } else {
                            layoutManager.scrollToPositionWithOffset(lastPosition, -100)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 100)
            }
        })
    }

    private fun typingLoading(isTyping: Boolean) {
        if (isTyping) {
            mActivityBinding.atvLeftTypingLoader.visible()
            Glide.with(applicationContext).load(R.drawable.dots_loading_image)
                .into(mActivityBinding.atvLeftTypingLoader)
        } else {
            mActivityBinding.atvLeftTypingLoader.gone()
            Glide.with(applicationContext).clear(mActivityBinding.atvLeftTypingLoader)
        }
    }

    private fun imagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pickMediaLatest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickMedia.launch(intent)
        }
    }

    private fun setPicture(isCamera: Boolean) {
        val picUri = if (isCamera) Uri.fromFile(File(mViewModel.currentPhotoPath!!))
        else Uri.parse(mViewModel.currentPhotoPath)

        startCrop(picUri)
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped.png"))

        val options = UCrop.Options().apply {
            val nightModeFlags: Int =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            setStatusBarColor(
                ContextCompat.getColor(
                    applicationContext,
                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) R.color.black else R.color.white
                )
            )
            setToolbarColor(ContextCompat.getColor(applicationContext, R.color.white))
        }

        val input = UCropInput(
            sourceUri = uri,
            destinationUri = destinationUri,
            aspectRatioX = 1f,
            aspectRatioY = 1f,
            maxSizeX = 5000,
            maxSizeY = 5000,
            options = options
        )

        cropImage.launch(input)
    }

    private fun uploadImageApi(path: String) {
        val file = File(path)
        val mimeType = getMimeType(applicationContext, path)

        val request = ImageUploadRequest(
            url = path, fileName = file.name, fileType = mimeType.toString()
        )
        loader(1)
        mViewModel.uploadImageApi(request).observe(this@ChatActivity) {
            if (it != null) {
                mViewModel.setMedias(it.response, it.type, true)
                mViewModel.onSendClickListener(mActivityBinding.aivSend)
                dismiss()
                Log.e("TAG", "Network url: ${it.response}")
            }
        }
    }

    private fun sendChatMessage() {
        mViewModel.sendChatMessage().observe(this) { data ->
            if (data != null) {
                mViewModel.messageEventListener()
            }
        }
    }

    private fun deleteMessage(request: DeleteMessageRequest) {

        if (!isNetworkConnection(applicationContext)) {
            val message = resources.getString(R.string.no_internet_connection)
            message.toast(applicationContext).show()
        }
        loader(2)
        mViewModel.deleteMessageItems(request).observe(this) { data ->
            if (data != null) {
                mResponse?.apply {
                    val list = request.ids!!.toMutableList()
                    mViewModel.updateLists(this, list, this@ChatActivity, mAdapter, chatDao, true) {
                        dismiss()
                        deleteMessageSelection(false)
                        setDbResponse(mResponse!!, false)
                    }
                }
            }
        }
    }

    private fun loader(status: Int) {
        lDialog = CustomDialog(this, UploadLoaderLayoutBinding::inflate).apply {
            configureDialog = { dialogBinding ->
                window?.setLayout(wrapContent, wrapContent)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCanceledOnTouchOutside(false)
                setCancelable(false)

                if (status == 1) {
                    Glide.with(applicationContext)
                        .load(R.drawable.animation_uploading_image)
                        .into(dialogBinding.aivLoaderImage)
                    dialogBinding.cpiLoader.gone()
                    dialogBinding.aivLoaderImage.visible()
                } else if (status == 2) {
                    dialogBinding.cpiLoader.visible()
                    dialogBinding.aivLoaderImage.gone()
                }
            }
        }
        if (isActivity)
            if (!lDialog.isShowing)
                lDialog.show()
    }

    private fun dismiss() {
        if (isActivity) {
            if (this::lDialog.isInitialized)
                if (lDialog.isShowing)
                    lDialog.dismiss()

            if (this::eDialog.isInitialized)
                if (eDialog.isShowing)
                    eDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        isActivity = true

        if ((SocketHandler.getSocket().connected()))
            if (this::scheduler.isInitialized)
                scheduler.start()

        Handler(Looper.myLooper()!!).postDelayed({
            if (isActivity)
                mViewModel.emitChatConnection()
        }, 200)
    }

    override fun onPause() {
        super.onPause()
        isActivity = false

        if ((SocketHandler.getSocket().connected()))
            if (this::scheduler.isInitialized)
                scheduler.pause()

        mViewModel.emitChatDisconnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivity = null
        isActivity = false
        if ((SocketHandler.getSocket().connected())) {
            ChatRepository.offEvents()
            mViewModel.clearTyping()
            ChatRepository.onNewMessageListener = null
            if (this::scheduler.isInitialized)
                scheduler.stop()
        }
    }
}