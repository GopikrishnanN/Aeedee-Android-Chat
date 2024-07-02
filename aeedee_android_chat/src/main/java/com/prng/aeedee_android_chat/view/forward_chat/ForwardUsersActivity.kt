package com.prng.aeedee_android_chat.view.forward_chat

import android.annotation.SuppressLint
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
import com.prng.aeedee_android_chat.view.chat_message.ChatActivity
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersDataResponse
import com.prng.aeedee_android_chat.view.forward_chat.adapter.ForwardUsersItemAdapter
import com.prng.aeedee_android_chat.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class ForwardUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForwardUsersBinding
    private val mViewModel: ForwardUsersViewModel by viewModels()
    private lateinit var mAdapter: ForwardUsersItemAdapter

    private var sFiles: JSONArray? = null
    private var sMessage: String? = null

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

        fetchUserList()

        onClickListener()
    }

    private fun getIntentData() {
        if (intent.hasExtra("files"))
            sFiles = JSONArray(intent.getStringExtra("files").toString())
        else JSONArray()
        if (intent.hasExtra("message"))
            sMessage = intent.getStringExtra("message").toString()

        mViewModel.initData(sFiles, sMessage)
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
                    mAdapter.getSelectedItems().filter { it.isSelected!! }.map { it.name }
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

    private fun sendForwardData(sItems: List<UsersDataResponse>) {
        CoroutineScope(Dispatchers.IO).launch {
            sItems.forEachIndexed { index, user ->
                mViewModel.emitSendMessage(user.friendId.toString(), ((sItems.size - 1) == index))
            }
        }
    }

    private fun fetchUserList() {
        if (!isNetworkConnection(applicationContext)) {
            val message = resources.getString(R.string.no_internet_connection)
            message.toast(applicationContext).show()
        } else {
            mViewModel.getChatUserList()?.observe(this) {
                if (it != null) {
                    setUiData(it.response)
                }
            }
        }
    }

    private fun setUiData(response: List<UsersDataResponse>) {
        if (response.isNotEmpty()) {
            mAdapter.setData(response)
            mAdapter.notifyDataSetChanged()
        }
    }
}