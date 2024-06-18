package com.prng.aeedee_android_chat.view.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ChatUserListItemLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.util.UserListDiffCallback
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.visible
import kotlinx.android.extensions.LayoutContainer

class ChatUserListAdapter : RecyclerView.Adapter<ChatUserListAdapter.ViewHolder>() {

    private var mList: List<UserDataResponse>? = listOf()

    var onItemClick: ((UserDataResponse) -> Unit)? = null

    fun setData(list: List<UserDataResponse>) {
        mList = list
    }

    fun updateList(newList: List<UserDataResponse>) {
        val diffCallback = UserListDiffCallback(mList!!, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        if (mList!!.isNotEmpty()) {
            (mList as ArrayList).clear()
            (mList as ArrayList).addAll(newList)
        } else {
            mList = newList
        }
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val li = LayoutInflater.from(parent.context)
        val binding = ChatUserListItemLayoutBinding.inflate(li, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.itemBinding
        binding.data = mList!![position]
        binding.clUserChat.setOnClickListener {
            onItemClick?.invoke(mList!![position])
        }

        holder.itemView.animate().alpha(1f).translationY(0f).setDuration(300).start()

        Glide.with(binding.root.context).load(mList!![position].avatar)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_placeholder_icon)
                    .error(R.drawable.ic_placeholder_icon)
                    .fallback(R.drawable.ic_placeholder_icon)
                    .centerCrop()
            )
            .into(binding.aivProfileImage)

        if ((mList!!.size - 1) == position) {
            binding.viewDivider.gone()
            binding.viewBottom.visible()
        } else {
            binding.viewDivider.visible()
            binding.viewBottom.gone()
        }
    }

    class ViewHolder(var itemBinding: ChatUserListItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}