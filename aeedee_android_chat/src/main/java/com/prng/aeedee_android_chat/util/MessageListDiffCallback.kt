package com.prng.aeedee_android_chat.util

import androidx.recyclerview.widget.DiffUtil
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse

class MessageListDiffCallback(
    private val oldList: List<MessageDataResponse>, private val newList: List<MessageDataResponse>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].unique_id == newList[newItemPosition].unique_id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}