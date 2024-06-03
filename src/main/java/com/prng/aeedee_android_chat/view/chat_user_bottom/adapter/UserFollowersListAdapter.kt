package com.prng.aeedee_android_chat.view.chat_user_bottom.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.HeaderFollowerItemLayoutBinding
import com.prng.aeedee_android_chat.databinding.UserFollowerItemLayoutBinding
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersDataResponse
import kotlinx.android.extensions.LayoutContainer

class UserFollowersListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_TYPE_HEADER = 0
        private const val ITEM_TYPE_ITEM = 1
    }

    private var mList: List<UsersDataResponse>? = listOf()

    var onItemClick: ((UsersDataResponse) -> Unit)? = null

    fun setData(list: List<UsersDataResponse>) {
        mList = list
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHeader(position)) ITEM_TYPE_HEADER else ITEM_TYPE_ITEM
    }

    private fun isHeader(position: Int): Boolean {
        return position == 0 || mList!![position].getFirstCharUppercase() != mList!![position - 1].getFirstCharUppercase()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_ITEM) {
            val itemBinding = UserFollowerItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ItemViewHolder(itemBinding)
        } else {
            val headerBinding = HeaderFollowerItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(headerBinding)
        }
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.itemBinding.data = mList!![position]
            holder.itemBinding.clItemClick.setOnClickListener {
                onItemClick?.invoke(mList!![position])
            }

            Glide.with(holder.itemBinding.root.context).load(mList!![position].avatar)
                .placeholder(R.drawable.ic_placeholder_icon).error(R.drawable.ic_placeholder_icon)
                .into(holder.itemBinding.aivProfileImage)
        } else if (holder is HeaderViewHolder) {
            holder.itemBinding.data = mList!![position]

            holder.itemBinding.clItemClick.setOnClickListener {
                onItemClick?.invoke(mList!![position])
            }

            Glide.with(holder.itemBinding.root.context).load(mList!![position].avatar)
                .placeholder(R.drawable.ic_placeholder_icon).error(R.drawable.ic_placeholder_icon)
                .into(holder.itemBinding.aivProfileImage)
        }
    }

    class ItemViewHolder(var itemBinding: UserFollowerItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    class HeaderViewHolder(var itemBinding: HeaderFollowerItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}