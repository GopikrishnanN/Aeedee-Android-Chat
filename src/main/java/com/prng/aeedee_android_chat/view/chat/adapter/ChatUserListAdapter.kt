package com.prng.aeedee_android_chat.view.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ChatUserListItemLayoutBinding
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import kotlinx.android.extensions.LayoutContainer

class ChatUserListAdapter : RecyclerView.Adapter<ChatUserListAdapter.ViewHolder>() {

    private var mList: List<UserDataResponse>? = listOf()

    var onItemClick: ((UserDataResponse) -> Unit)? = null

    fun setData(list: List<UserDataResponse>) {
        mList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ChatUserListItemLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.chat_user_list_item_layout, parent,
            false
        )
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
        Glide.with(binding.root.context).load(mList!![position].avatar)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_placeholder_icon)
                    .error(R.drawable.ic_placeholder_icon)
                    .fallback(R.drawable.ic_placeholder_icon)
                    .centerCrop()
            )
            .into(binding.aivProfileImage)
    }

    class ViewHolder(var itemBinding: ChatUserListItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}