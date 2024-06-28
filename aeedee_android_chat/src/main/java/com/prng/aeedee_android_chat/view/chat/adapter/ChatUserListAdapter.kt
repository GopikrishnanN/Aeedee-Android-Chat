package com.prng.aeedee_android_chat.view.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.prng.aeedee_android_chat.databinding.ChatUserListItemLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.textToDrawable
import com.prng.aeedee_android_chat.util.UserListDiffCallback
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.visible
import kotlinx.android.extensions.LayoutContainer

class ChatUserListAdapter : RecyclerView.Adapter<ChatUserListAdapter.ViewHolder>() {

    private var mList: List<UserDataResponse>? = listOf()

    var onItemClick: ((UserDataResponse, Boolean) -> Unit)? = null

    var isSelection: Boolean = false

    fun updateList(newList: List<UserDataResponse>, newSelectedList: List<UserDataResponse>) {
        val newSelectedMap = (newSelectedList.map { it._id }).associateWith { true }

        val diffCallback = UserListDiffCallback(mList!!, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        if (mList!!.isNotEmpty()) {
            (mList as ArrayList).clear()
            (mList as ArrayList).addAll(newList.map {
                it.copy(isSelected = newSelectedMap[it._id] ?: false)
            })
        } else {
            mList = newList.map {
                it.copy(isSelected = newSelectedMap[it._id] ?: false)
            }.toMutableList()
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

        if (mList!![position].isSelected!!) binding.aivSelectionItem.visible()
        else binding.aivSelectionItem.gone()

        binding.clUserChat.setOnClickListener {
            if (isSelection) {
                mList!![holder.absoluteAdapterPosition].isSelected =
                    !mList!![holder.absoluteAdapterPosition].isSelected!!
                if (mList!![holder.absoluteAdapterPosition].isSelected!!) binding.aivSelectionItem.visible()
                else binding.aivSelectionItem.gone()
            }
            onItemClick?.invoke(mList!![holder.absoluteAdapterPosition], isSelection)
        }

        binding.clUserChat.setOnLongClickListener {
            if (!isSelection) {
                isSelection = true
                mList!![holder.absoluteAdapterPosition].isSelected =
                    !mList!![holder.absoluteAdapterPosition].isSelected!!
                if (mList!![holder.absoluteAdapterPosition].isSelected!!) binding.aivSelectionItem.visible()
                else binding.aivSelectionItem.gone()
                onItemClick?.invoke(mList!![holder.absoluteAdapterPosition], isSelection)
            }
            return@setOnLongClickListener true
        }

        holder.itemView.animate().alpha(1f).translationY(0f).setDuration(300).start()

        val placeholder =
            textToDrawable(binding.root.context, getCharName(mList!![position].userName))

        Glide.with(binding.root.context).load(mList!![position].avatar)
            .apply(
                RequestOptions()
                    .placeholder(placeholder)
                    .error(placeholder)
                    .fallback(placeholder)
                    .centerCrop()
            )
            .into(binding.aivProfileImage)
    }

    private fun getCharName(userName: String?): String {
        return if(userName!!.length > 1) userName.slice(0..1) else userName
    }

    class ViewHolder(var itemBinding: ChatUserListItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}