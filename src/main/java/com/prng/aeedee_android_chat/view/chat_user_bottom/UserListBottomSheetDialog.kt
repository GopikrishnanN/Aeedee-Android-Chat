package com.prng.aeedee_android_chat.view.chat_user_bottom

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.UserListBottomSheetLayoutBinding
import com.prng.aeedee_android_chat.isNetworkConnection
import com.prng.aeedee_android_chat.toast
import com.prng.aeedee_android_chat.view.chat_user_bottom.adapter.UserFollowersListAdapter
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersDataResponse
import java.util.Locale

class UserListBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var binding: UserListBottomSheetLayoutBinding
    private val mViewModel: UsersListBSViewModel by viewModels()
    private lateinit var mAdapter: UserFollowersListAdapter

    var onItemClickListener: ((UsersDataResponse) -> Unit)? = null

    private var mList: List<UsersDataResponse>? = arrayListOf()
    private val mFilteredList: List<UsersDataResponse> = arrayListOf()
    private var totalCount: Int? = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = UserListBottomSheetLayoutBinding.inflate(inflater, container, false)
        binding.viewModel = mViewModel
        initData()

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }

    private fun initData() {
        mAdapter = UserFollowersListAdapter()
        mAdapter.onItemClick = {
            dialog?.dismiss()
            onItemClickListener?.invoke(it)
        }

        binding.rvUserList.adapter = mAdapter

        binding.aivClose.setOnClickListener {
            dialog?.dismiss()
        }

        noChatGifImage()
        initList()
        onListener()
    }

    private fun initList() {

        if (!isNetworkConnection(requireActivity())) {
            val message = resources.getString(R.string.no_internet_connection)
            message.toast(requireActivity()).show()
        }

        mViewModel.getUsersList()?.observe(this) {
            if (it != null) {
                totalCount = it.followersCount
                val text = requireActivity().resources.getString(R.string.followers_s_list)
                binding.atvFollowsTitle.text = String.format(text, totalCount)
                mList = it.response
                loadUserList(mList!!)
            }
        }
    }

    private fun onListener() {
        mViewModel.onSearchListener = {
            filter(it)
        }
    }

    private fun loadUserList(response: List<UsersDataResponse>) {
        if (response.isNotEmpty()) {
            binding.clEmpty.visibility = View.GONE
            val sortedList = response.sortedBy { it.name }
            mAdapter.setData(sortedList)
            mAdapter.notifyDataSetChanged()
        } else {
            binding.clEmpty.visibility = View.VISIBLE
        }
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet!!)
        val layoutParams = bottomSheet.layoutParams
        val windowHeight = getWindowHeight()
        if (layoutParams != null) {
            layoutParams.height = windowHeight
        }
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    @Suppress("DEPRECATION")
    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity?)!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun noChatGifImage() {
        Glide.with(requireActivity()).load(R.drawable.animation_chat_image)
            .into(binding.aivChatIcon)
    }

    private fun filter(query: String) {
        (mFilteredList as ArrayList).clear()
        if (query.isEmpty()) {
            val list = mList!!.toMutableList().sortedBy { it.name }
            mFilteredList.addAll(list)
        } else {
            val filtered = mList!!.filter {
                it.name.toString().lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))
            }
            val list = filtered.toMutableList().sortedBy { it.name }
            mFilteredList.addAll(list)
        }
        mAdapter.setData(mFilteredList)
        mAdapter.notifyDataSetChanged()
    }

    companion object {
        const val TAG = "UserListBottomSheetDialog"
    }

}