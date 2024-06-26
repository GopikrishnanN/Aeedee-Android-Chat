package com.prng.aeedee_android_chat.view.chat

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ActivityChatUserListBinding
import com.prng.aeedee_android_chat.databinding.UploadLoaderLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.isNetworkConnection
import com.prng.aeedee_android_chat.repository.ChatRepository
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.deo.ChatDatabase
import com.prng.aeedee_android_chat.roomdb.di.DatabaseModule
import com.prng.aeedee_android_chat.roomdb.entity_model.asDatabaseModel
import com.prng.aeedee_android_chat.socket.SocketHandler
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.util.CustomDialog
import com.prng.aeedee_android_chat.view.chat.adapter.ChatUserListAdapter
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.DeleteUserRequest
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.view.chat.model.asDatabaseModel
import com.prng.aeedee_android_chat.view.chat_message.ChatActivity
import com.prng.aeedee_android_chat.view.chat_user_bottom.UserListBottomSheetDialog
import com.prng.aeedee_android_chat.visible
import com.prng.aeedee_android_chat.wrapContent
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChatUserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatUserListBinding
    private lateinit var mAdapter: ChatUserListAdapter
    private val mViewModel: ChatUserListViewModel by viewModels()
    private lateinit var database: ChatDatabase
    private lateinit var chatDao: ChatDao

    private var isPause: Boolean = false

    private lateinit var lDialog: CustomDialog<UploadLoaderLayoutBinding>
    private lateinit var dList: ArrayList<UserDataResponse>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EmojiManager.install(GoogleEmojiProvider())

        setUserId()

        database = DatabaseModule.provideAppDatabase(this)
        chatDao = DatabaseModule.provideChannelDao(database)

        dList = ArrayList()

        binding.viewModel = mViewModel
        binding.lifecycleOwner = this

        binding.fabAddUser.hide()

        mAdapter = ChatUserListAdapter()
        binding.rvChatUserList.adapter = mAdapter
        binding.rvChatUserList.itemAnimator = DefaultItemAnimator()

        binding.rvChatUserList.layoutManager = LinearLayoutManager(applicationContext).apply {
            isSmoothScrollbarEnabled = true
        }

        binding.rvChatUserList.addItemDecoration(CustomItemDecoration())

        mViewModel.initSocket(this)

        refreshApi()

        onClickOperation()

        dbUpdateData()
    }

    class CustomItemDecoration : RecyclerView.ItemDecoration() {

        private val decorationValue = 10

        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.set(decorationValue, decorationValue, decorationValue, decorationValue)
        }
    }

    private fun setUserId() {
        Log.e("TAG", "setUserId: ${Build.MODEL}")
        userID = when (Build.MODEL) {
            "vivo 1804" -> {
                "65f2d9b84c342fb51e72343f"
            }

            "vivo 1820" -> {
                "65ddbed3f98eadc6bee76361"
//                "65f29bd9c4f2640a7a24d99c"
            }

            else -> {
                "65f2d9b84c342fb51e72343f"
//                "65ddbed3f98eadc6bee76361"
            }
        }
    }

    private fun refreshApi() {
        binding.srlRefreshData.setOnRefreshListener {
            binding.srlRefreshData.isRefreshing = true

            val request = ChatUserRequest(limit = 50)
            fetchUserList(request)

            Handler(Looper.myLooper()!!).postDelayed({
                binding.srlRefreshData.isRefreshing = false
            }, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.emitChatConnection()
        Handler(Looper.myLooper()!!).postDelayed({
            isPause = false
        }, 200)
        val request = ChatUserRequest(limit = 50)
        fetchUserList(request)
    }

    override fun onPause() {
        super.onPause()
        isPause = true
        mViewModel.emitChatDisconnection()
        mViewModel.mSearchText = ""
        binding.aetSearchUser.setText("")
    }

    private fun dbUpdateData() {
        val users: LiveData<List<UserDataResponse>?> =
            chatDao.getUserAll().map { it?.asDatabaseModel() }//.distinctUntilChanged()

        users.observe(this) {
            if (it != null) {
                setUiData(it)
            }
        }
    }

    private fun fetchUserList(request: ChatUserRequest) {
        if (!isNetworkConnection(applicationContext)) {
            val message = resources.getString(R.string.no_internet_connection)
            message.toast(applicationContext).show()
        } else {
            mViewModel.getChatUserList(request)?.observe(this) {
                if (it != null) {
                    binding.srlRefreshData.isRefreshing = false
                    lifecycleScope.launch {
                        chatDao.replaceUsers(it.response.asDatabaseModel())
                    }
                }
            }
        }
    }

    private fun setUiData(response: List<UserDataResponse>) {
        if (response.isNotEmpty()) {
            binding.clEmpty.gone()
            binding.fabAddUser.show()
            mAdapter.updateList(response, dList)
        } else {
            noChatGifImage()
            binding.clEmpty.visible()
            mAdapter.updateList(arrayListOf(), dList)
            binding.fabAddUser.hide()
        }
    }

    private fun noChatGifImage() {
        Glide.with(applicationContext).load(R.drawable.animation_chat_image)
            .into(binding.aivChatIcon)
    }

    private fun onClickOperation() {
        binding.atbStartAChat.setOnClickListener {
            openBottomSheet()
        }

        binding.fabAddUser.setOnClickListener {
            openBottomSheet()
        }

        mAdapter.onItemClick = { data, isSelection ->
            if (isSelection) {

                val dSetData = dList.map { it._id }.toSet()

                if (dSetData.contains(data._id)) dList.remove(data) else dList.add(data)

                if (dList.isEmpty()) {
                    mAdapter.isSelection = false
                    binding.clUserMenu.gone()
                } else {
                    binding.clUserMenu.visible()
                }

            } else {

                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("userId", userID)
                intent.putExtra("receiverId", data.userId)
                intent.putExtra("name", data.userName)
                intent.putExtra("avatar", data.avatar)
                startActivity(intent)

            }
        }

        binding.aivCancel.setOnClickListener {
            dList.clear()
            binding.clUserMenu.gone()
            mAdapter.isSelection = false
            fetchUserList(ChatUserRequest(limit = 50))
        }

        binding.aivDelete.setOnClickListener {
            val ids = dList.map { it.userId.toString() }
            loader()
            val request = DeleteUserRequest(receiverId = ids)
            mViewModel.deleteChatUserList(request).observe(this) {
                if (it != null) {
                    dList.clear()
                    mAdapter.isSelection = false
                    binding.clUserMenu.gone()
                    runBlocking {
                        chatDao.deleteMessageDataByIds(ids)
                    }
                    dismiss()
                    fetchUserList(ChatUserRequest(limit = 50))
                }
            }
        }

        mViewModel.onSearchListener = {
            if (!isPause)
                fetchUserList(it)
        }

        mViewModel.onDeleteMessageListener = { deletedIds ->
            mViewModel.updateChildEntityInParent(
                chatDao, deletedIds.ids!!, deletedIds.user_id.toString(), null, ifData = 1
            )
        }

        mViewModel.onReactionMessageListener = { data ->
            mViewModel.updateChildEntityInParent(
                chatDao, arrayListOf(data.messageId!!), data.userId.toString(), data, ifData = 2
            )
        }

        mViewModel.onReadStatusListener = { data ->
            mViewModel.updateChildEntityInParent(
                chatDao, data.ids!!, data.readStatus.toString(), null, readStatus = data, ifData = 3
            )
        }
    }

    private fun openBottomSheet() {
        val modal = UserListBottomSheetDialog()
        modal.onItemClickListener = {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("userId", it.userId)
            intent.putExtra("receiverId", it.friendId)
            intent.putExtra("name", it.name)
            intent.putExtra("avatar", it.avatar)
            intent.putExtra("code", it.phone!!.code)
            intent.putExtra("number", it.phone.number)
            startActivity(intent)
        }
        supportFragmentManager.let { modal.show(it, UserListBottomSheetDialog.TAG) }
    }

    private fun loader() {
        lDialog = CustomDialog(this, UploadLoaderLayoutBinding::inflate).apply {
            configureDialog = { dialogBinding ->
                window?.setLayout(wrapContent, wrapContent)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCanceledOnTouchOutside(false)
                setCancelable(false)

                dialogBinding.cpiLoader.visible()
                dialogBinding.aivLoaderImage.gone()
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

    override fun onDestroy() {
        super.onDestroy()
        dismiss()
        mViewModel.emitChatDisconnection()
        Handler(Looper.myLooper()!!).postDelayed({
            SocketHandler.offEvents()
            ChatRepository.offEvents()
            ChatRepository.offEventsConnection()
            mViewModel.disconnectSocket()
        }, 1000)
    }

}