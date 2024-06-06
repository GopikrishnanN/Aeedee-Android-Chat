package com.prng.aeedee_android_chat.view.forward_chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.ForwardUserItemLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.view.chat.model.UserDataResponse
import com.prng.aeedee_android_chat.visible
import kotlinx.android.extensions.LayoutContainer

class ForwardUsersItemAdapter : RecyclerView.Adapter<ForwardUsersItemAdapter.ItemViewHolder>() {

    private var mList: List<UserDataResponse>? = listOf()

    var onItemClick: ((Boolean) -> Unit)? = null

    fun setData(list: List<UserDataResponse>) {
        mList = list
    }

    fun getSelectedItems(): List<UserDataResponse> {
        return mList!!.filter { it.isSelected == true }.map { it }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemBinding = ForwardUserItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val binding = holder.itemBinding
        binding.data = mList!![position]
        binding.clUserChat.setOnClickListener {
            mList!![position].isSelected = isSelected(mList!![position].isSelected)
            if (binding.aivSelectionItem.visibility == View.VISIBLE)
                binding.aivSelectionItem.gone()
            else binding.aivSelectionItem.visible()
            onItemClick?.invoke(true)
        }

        Glide.with(binding.root.context).load(mList!![position].avatar)
            .placeholder(R.drawable.ic_placeholder_icon).error(R.drawable.ic_placeholder_icon)
            .into(binding.aivProfileImage)
    }

    private fun isSelected(selected: Boolean?): Boolean {
        return if (selected != null) {
            !selected
        } else {
            true
        }
    }

    class ItemViewHolder(var itemBinding: ForwardUserItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}