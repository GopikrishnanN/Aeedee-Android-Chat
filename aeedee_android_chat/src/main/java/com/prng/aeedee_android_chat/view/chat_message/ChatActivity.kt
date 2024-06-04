package com.prng.aeedee_android_chat.view.chat_message

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
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
import com.prng.aeedee_android_chat.roomdb.entity_model.asDatabaseModel
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.util.CustomDialog
import com.prng.aeedee_android_chat.util.FunctionScheduler
import com.prng.aeedee_android_chat.util.UCropContract
import com.prng.aeedee_android_chat.util.UCropInput
import com.prng.aeedee_android_chat.util.UCropResult
import com.prng.aeedee_android_chat.view.chat_message.adapter.MessageItemListAdapter
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadRequest
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataUsers
import com.prng.aeedee_android_chat.view.chat_message.model.MessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.asDatabaseModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.visible
import com.prng.aeedee_android_chat.wrapContent
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class ChatActivity : AppCompatActivity() {
    private lateinit var mActivityBinding: ActivityChatBinding
    private val mViewModel: ChatViewModel by viewModels()
    private lateinit var mAdapter: MessageItemListAdapter
    private lateinit var layoutManager: LinearLayoutManager

    companion object {
        // User Id
        var userId: String = ""
        private var receiverId: String = ""
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
    private var isFirstLocalDb = true

    private var isSocket = false
    private var isRecent = false
    private var isLocalData = false
    private var isEmoji = false

    private lateinit var scheduler: FunctionScheduler

    private lateinit var lDialog: CustomDialog<UploadLoaderLayoutBinding>

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

    private val cropImage = registerForActivityResult(UCropContract()) { result: UCropResult ->
        if (result.resultCode == RESULT_OK) {
            result.uri?.let { uri ->
                uri.path?.let { path ->
                    uploadImageApi(path)

                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            result.error?.let { throwable ->
                // Handle the crop error
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

        val messages: LiveData<List<MessageDataUsers>?> =
            chatDao.getMessageAll().map { it?.asDatabaseModel() }.distinctUntilChanged()

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
//                            val lastMessageId =
//                                if (mResponse != null && mResponse!!.isNotEmpty()) mResponse!!.last()._id.ifEmpty { mResponse!!.last().unique_id!! } else ""
                            val lastMessageId =
                                if (mResponse != null && mResponse!!.isNotEmpty()) mResponse!!.last()._id else ""
                            if (lastMessageId.isNotEmpty()) {
                                fetchMessageApi(lastMessageId, 1)
                            }
                        }
                    } else {
                        setUiData(emptyList())
                        isFirstLocalDb = false
                        fetchMessageApi(lastId, 0)
                    }
                } else {
                    isFirstLocalDb = false
                    fetchMessageApi(lastId, 0)
                }
        }

        mViewModel.initData(receiverId, userId)

        mViewModel.initSocket(this@ChatActivity)

        emitActiveTimeLoop()

        layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = false
        }
        mActivityBinding.rvChatMessageList.setHasFixedSize(true)
        mActivityBinding.rvChatMessageList.layoutManager = layoutManager

        typingLoading(false)

        val drawableResId = R.drawable.ic_delete_icon
        setDrawableStartWithSize(mActivityBinding.atvDeleteText, drawableResId)

        mAdapter = MessageItemListAdapter()
        mActivityBinding.rvChatMessageList.adapter = mAdapter

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
            mViewModel.emitReaction(reaction, data)
        }

        mViewModel.onMessageMenuListener = { menu, data ->
            when (menu.id) {
                0 -> {
                    // - - Reply - -
                    mViewModel.messageType(data._id, data.message, name)
                    mViewModel.messageType(MessageType.Reply.name)
                    mViewModel.onReplyVisibility()
                }

                1 -> {
                    // - - Copy - -
                    copyToClipboard(data.message, applicationContext)
                }

                2 -> {
                    // - - Forward - -
                    Log.e("TAG", "Forward: ")
                }

                3 -> {
                    // - - Delete - -
                    mAdapter.isSelection = true
                    deleteSelection(data)
                }
            }
        }

        mViewModel.onItemClickListListener = {
            mResponse?.let { _ ->
                runBlocking {
                    isSocket = true
                    val processedData: MessageDataResponse = if (mResponse!!.isNotEmpty()) {
                        mViewModel.addDateTime(it, mResponse!!.last().createdAt)
                    } else {
                        mViewModel.addDateTime(it, null)
                    }
                    mAdapter.addData(processedData)
                    setDbResponse(arrayListOf(processedData), false)

                    val recyclerViewState = onSaveInstanceRV()
                    mAdapter.notifyItemChanged((mResponse!!.size - 1), Payload.Update.name)
                    onRestoreInstance(recyclerViewState)

                    scrollList()
                }
            }
        }

        mViewModel.onSendMessageApiListener = {
            sendChatMessage()
        }

        mViewModel.onUserIdListener = {
            if (it.isNotEmpty()) {
                mViewModel.messageEventListener(it)
            }
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
            onlineStatus(it)
        }

        mActivityBinding.aivBack.setOnClickListener {
            finish()
        }

        mViewModel.onActiveTimeListener = {
            val chatStatus = it.chatStatus.toString().replace("_", "")
            mActivityBinding.atvMessage.text = chatStatus
            if (chatStatus == "offline") {
                val color = ContextCompat.getColor(applicationContext, R.color.black)
                mActivityBinding.atvMessage.setTextColor(color)
            } else {
                val color = ContextCompat.getColor(applicationContext, R.color.blue)
                mActivityBinding.atvMessage.setTextColor(color)
            }
        }

        mViewModel.onReadStatusListener = {
            if (it.ids != null) {
                if (it.ids.isNotEmpty()) {
                    for (id in it.ids) {
                        mResponse?.let { list ->
                            val position = mViewModel.getItemIndex(list, id)
                            if (position > -1) {
                                mResponse!![position].read_status = it.readStatus.toString().toInt()
                                mAdapter.updateData(position, list[position])
                                var recyclerViewState = onSaveInstanceRV()
                                mAdapter.notifyItemChanged(
                                    (mResponse!!.size - 1), Payload.Update.name
                                )
                                onRestoreInstance(recyclerViewState)

                                for (i in mResponse!!.size - 1 downTo 0) {
                                    if (mResponse!![i].read_status != 3) {
                                        mResponse!![position].read_status = 3
                                        mAdapter.updateData(position, mResponse!![position])
                                    }
                                    recyclerViewState = onSaveInstanceRV()
                                    mAdapter.notifyItemChanged(
                                        (mResponse!!.size - 1), Payload.Update.name
                                    )
                                    onRestoreInstance(recyclerViewState)
                                }
                            }
                        }
                    }
                    setDbResponse(mResponse!!, false)
                }
            }
        }

        mViewModel.onEmojiUpdatesListener = {
            isEmoji = true
            emojiSelection(data = it)
        }

        mViewModel.onReactionDataListener = {
            emojiListener(data = it)
        }

        mActivityBinding.clDeleteChat.setOnClickListener {
            val selectedIds = mAdapter.getSelectedIds()
            val request = DeleteMessageRequest(ids = selectedIds)
            deleteMessage(request)
        }

        mActivityBinding.atvCancelSelection.setOnClickListener {
            val list = mAdapter.getSelectedIds().toMutableList()
            mViewModel.updateLists(mResponse!!, list, true, mAdapter) {
                deleteMessageSelection(false)
            }
        }
    }

    private fun emojiListener(data: DatabaseReactionData) {
        CoroutineScope(Dispatchers.Default).launch {
            for (i in mResponse!!.indices) {
                if (mResponse!![i].unique_id == data.messageId) {
                    mResponse!![i].reaction = arrayListOf(data)
                    mAdapter.selectMessage(mResponse!![i], i)
                    mAdapter.notifyItemChanged(i, Payload.Update.name)
                }
            }
            Log.e("TAG", "emojiListener: ${mResponse!!.size}")
            setDbResponse(mResponse!!)
        }
    }

    private fun deleteMessageSelection(isSelected: Boolean) {
        runOnUiThread {
            if (isSelected) {
                mActivityBinding.clMessageLayout.gone()
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
        CoroutineScope(Dispatchers.Default).launch {
            for (i in mResponse!!.indices) {
                if (mResponse!![i].unique_id == data.unique_id) {
                    mResponse!![i].isSelectEnable = !mResponse!![i].isSelectEnable
                    mAdapter.selectMessage(mResponse!![i], i)
                    mAdapter.notifyItemChanged(i, Payload.Update.name)
                    deleteMessageSelection(true)
                }
            }
        }
    }

    private fun emojiSelection(data: MessageDataResponse) {
        CoroutineScope(Dispatchers.Default).launch {
            for (i in mResponse!!.indices) {
                if (mResponse!![i].unique_id == data.unique_id) {
                    for (j in data.reaction!!.indices) {
                        for (k in mResponse!![i].reaction!!.indices) {
                            if (mResponse!![i].reaction!![k].userId == data.reaction!![j].userId) {
                                mResponse!![i].reaction!![k].message = data.reaction!![j].message
                                mResponse!![i].reaction!![k].receiverId =
                                    data.reaction!![j].receiverId
                                mResponse!![i].reaction!![k].messageId =
                                    data.reaction!![j].messageId
                                mResponse!![i].reaction!![k].userId = data.reaction!![j].userId
                            } else {
                                mResponse!![i].reaction!!.addAll(data.reaction!!)
                            }
                        }
                    }
                    mAdapter.selectMessage(mResponse!![i], i)
                    mAdapter.notifyItemChanged(i, Payload.Update.name)
                }
            }
            setDbResponse(mResponse!!, false)
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
        mActivityBinding.rvChatMessageList.layoutManager?.onRestoreInstanceState(p)
    }

    private fun emitActiveTimeLoop() {
        if ((SocketHandler.getSocket().connected()))
            scheduler = FunctionScheduler {
                mViewModel.emitActiveTime()
            }
    }

    private fun initIntent() {
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
                isSocket = false
                if (it.lastId != null)
                    if (it.lastId.isNotEmpty()) {
                        lastId = it.lastId
                        totalCount = it.count
                    } else {
                        lastId = ""
                        totalCount = 0
                    }
                setDbResponse(it.response, true)
            }
        }
    }

    private fun setDbResponse(response: List<MessageDataResponse>, isApi: Boolean) {
        lifecycleScope.launch {
            if (response.isNotEmpty()) {
                if (mResponse!!.isEmpty()) {
                    val dbUserMessage = DatabaseMessageData(
                        receiverId = receiverId,
                        response = response.asDatabaseModel(),
                        _id = receiverId
                    )
                    chatDao.insertMessageDataUsers(dbUserMessage)
                } else {
                    if (!isFirstTime && isApi) {
                        mResponse?.addAll(0, response)
                        val dbUserMessage = DatabaseMessageData(
                            receiverId = receiverId,
                            response = mResponse!!.asDatabaseModel(),
                            _id = receiverId
                        )
                        chatDao.insertMessageDataUsers(dbUserMessage)
                    } else if (isRecent && isApi) {
                        mResponse?.addAll(response)
                        val dbUserMessage = DatabaseMessageData(
                            receiverId = receiverId,
                            response = mResponse!!.asDatabaseModel(),
                            _id = receiverId
                        )
                        chatDao.insertMessageDataUsers(dbUserMessage)
                    } else {
                        val dbUserMessage = DatabaseMessageData(
                            receiverId = receiverId,
                            response = mResponse!!.asDatabaseModel(),
                            _id = receiverId
                        )
                        chatDao.insertMessageDataUsers(dbUserMessage)
                    }
                }
            }
        }
    }

    private fun setDbResponse(response: List<MessageDataResponse>) {
        lifecycleScope.launch {
            if (response.isNotEmpty()) {
                val dbUserMessage = DatabaseMessageData(
                    receiverId = receiverId,
                    response = response.asDatabaseModel(),
                    _id = receiverId
                )
                chatDao.insertMessageDataUsers(dbUserMessage)
            }
        }
    }


    private fun setUiData(responses: List<MessageDataResponse>) {
        if (responses.isNotEmpty()) {
            Log.e("TAG", "setUiDataResponses: ${responses.size}")
            mActivityBinding.aivNoMessageIcon.gone()
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
                        mResponse?.addAll(processedList)
                        mAdapter.setData(mResponse!!)
                        mAdapter.notifyItemRangeChanged(
                            mResponse!!.size - responses.size, mResponse!!.size
                        )
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
            if (mResponse?.isNotEmpty() == true) {
                mViewModel.updateReadStatus(mResponse)
            }
        } else {
            if (mResponse!!.isEmpty()) mActivityBinding.aivNoMessageIcon.visible()
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

    private fun onlineStatus(isOnline: Boolean) {
        if (isOnline) {
            mActivityBinding.aivProfileImage.borderWidth = 5
            mActivityBinding.aivProfileImage.borderColor =
                ContextCompat.getColor(this, R.color.blue)
        } else {
            mActivityBinding.aivProfileImage.borderWidth = 0
            mActivityBinding.aivProfileImage.borderColor =
                ContextCompat.getColor(this, android.R.color.transparent)
        }
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickMedia.launch(intent)
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
                mViewModel.messageEventListener(uniqueId = mViewModel.uniqueId)
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
                    mViewModel.updateLists(this, list, false, mAdapter) {
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
        if (!lDialog.isShowing)
            lDialog.show()
    }

    private fun dismiss() {
        if (this::lDialog.isInitialized)
            if (lDialog.isShowing)
                lDialog.dismiss()
    }

    override fun onResume() {
        super.onResume()
        if ((SocketHandler.getSocket().connected()))
            if (this::scheduler.isInitialized)
                scheduler.start()
    }

    override fun onPause() {
        super.onPause()
        if ((SocketHandler.getSocket().connected()))
            if (this::scheduler.isInitialized)
                scheduler.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if ((SocketHandler.getSocket().connected())) {
            ChatRepository.offEvents()
            mViewModel.clearTyping()
            if (this::scheduler.isInitialized)
                scheduler.stop()
        }
    }
}