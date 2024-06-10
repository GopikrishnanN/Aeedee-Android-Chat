package com.prng.aeedee_android_chat.util

import androidx.recyclerview.widget.DiffUtil
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse

class UserListDiffCallback(
    private val oldList: List<UserDataResponse>, private val newList: List<UserDataResponse>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]._id == newList[newItemPosition]._id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}