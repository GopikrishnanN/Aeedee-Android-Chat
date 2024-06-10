package com.prng.aeedee_android_chat.view.forward_chat

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ActivityForwardUsersBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.isNetworkConnection
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.view.chat_message.ChatActivity
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.forward_chat.adapter.ForwardUsersItemAdapter
import com.prng.aeedee_android_chat.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForwardUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForwardUsersBinding
    private val mViewModel: ForwardUsersViewModel by viewModels()
    private lateinit var mAdapter: ForwardUsersItemAdapter

    private var mMessageData: MessageDataResponse? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForwardUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewModel = mViewModel
        binding.lifecycleOwner = this

        getIntentData()

        mAdapter = ForwardUsersItemAdapter()
        binding.rvChatUserList.adapter = mAdapter

        binding.atvSelectedItems.movementMethod = ScrollingMovementMethod()

        val request = ChatUserRequest(limit = 50)
        fetchUserList(request)

        onClickListener()
    }

    private fun getIntentData() {
        if (intent.hasExtra("data"))
            mMessageData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("data", MessageDataResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("data")
            }

        mViewModel.initData(mMessageData)
    }

    @SuppressLint("SetTextI18n")
    private fun onClickListener() {
        binding.aivBack.setOnClickListener {
            finish()
        }

        binding.aivSend.setOnClickListener {
            sendForwardData(mAdapter.getSelectedItems())
        }

        mViewModel.onSearchListener = {

        }

        mViewModel.onCloseActivity = {
            ChatActivity.mActivity?.finish()
            finish()
        }

        mAdapter.onItemClick = { isSelected ->
            if (isSelected) {
                val data =
                    mAdapter.getSelectedItems().filter { it.isSelected!! }.map { it.userName }
                if (data.isNotEmpty()) {
                    binding.clBottomSend.visible()
                    binding.atvSelectedItems.text = data.joinToString(", ")
                    binding.atvName.text = "${data.size} selected"
                } else {
                    binding.clBottomSend.gone()
                    binding.atvSelectedItems.text = ""
                    binding.atvName.text =
                        ContextCompat.getString(applicationContext, R.string.forward_to_title)
                }
            }
        }
    }

    private fun sendForwardData(sItems: List<UserDataResponse>) {
        CoroutineScope(Dispatchers.IO).launch {
            sItems.forEachIndexed { index, user ->
                mViewModel.emitSendMessage(user.userId.toString(), ((sItems.size - 1) == index))
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
                    setUiData(it.response)
                }
            }
        }
    }

    private fun setUiData(response: List<UserDataResponse>) {
        if (response.isNotEmpty()) {
            mAdapter.setData(response)
            mAdapter.notifyDataSetChanged()
        }
    }
}