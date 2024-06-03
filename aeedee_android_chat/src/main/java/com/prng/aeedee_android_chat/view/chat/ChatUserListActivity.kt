package com.prng.aeedee_android_chat.view.chat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.bumptech.glide.Glide
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ActivityChatUserListBinding
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
import com.prng.aeedee_android_chat.view.chat.adapter.ChatUserListAdapter
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.view.chat.model.asDatabaseModel
import com.prng.aeedee_android_chat.view.chat_message.ChatActivity
import com.prng.aeedee_android_chat.view.chat_user_bottom.UserListBottomSheetDialog
import com.prng.aeedee_android_chat.visible
import kotlinx.coroutines.launch

class ChatUserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatUserListBinding
    private lateinit var mAdapter: ChatUserListAdapter
    private val mViewModel: ChatUserListViewModel by viewModels()
    private lateinit var database: ChatDatabase
    private lateinit var chatDao: ChatDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = DatabaseModule.provideAppDatabase(this)
        chatDao = DatabaseModule.provideChannelDao(database)

        binding.viewModel = mViewModel
        binding.lifecycleOwner = this

        binding.fabAddUser.hide()

        mAdapter = ChatUserListAdapter()
        binding.rvChatUserList.adapter = mAdapter

        mViewModel.initSocket(this)

        refreshApi()

        onClickOperation()

        dbUpdateData()
    }

    private fun refreshApi() {
//        binding.srlRefreshData.setOnRefreshListener {
//            binding.srlRefreshData.isRefreshing = true
//
//            val request = ChatUserRequest(limit = 50)
//            fetchUserList(request)
//
//            Handler(Looper.myLooper()!!).postDelayed({
//                binding.srlRefreshData.isRefreshing = false
//            }, 2000)
//        }
    }

    override fun onResume() {
        super.onResume()
        val request = ChatUserRequest(limit = 50)
        fetchUserList(request)
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
        }

        mViewModel.getChatUserList(request)?.observe(this) {
            if (it != null) {
                lifecycleScope.launch {
                    chatDao.replaceUsers(it.response.asDatabaseModel())
                }
            }
        }
    }

    private fun setUiData(response: List<UserDataResponse>) {
        if (response.isNotEmpty()) {
            binding.clEmpty.gone()
            binding.fabAddUser.show()
            mAdapter.setData(response)
            mAdapter.notifyDataSetChanged()
        } else {
            noChatGifImage()
            binding.clEmpty.visible()
            mAdapter.setData(arrayListOf())
            mAdapter.notifyDataSetChanged()
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

        mAdapter.onItemClick = { data ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("userId", userID)
            intent.putExtra("receiverId", data.userId)
            intent.putExtra("name", data.userName)
            intent.putExtra("avatar", data.avatar)
            startActivity(intent)
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

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.emitChatDisconnection()
        Handler(Looper.myLooper()!!).postDelayed({
            SocketHandler.offEvents()
            ChatRepository.offEvents()
            ChatRepository.offEventsConnection()
            mViewModel.disconnectSocket()
        }, 1000)
    }

}