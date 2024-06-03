package com.prng.aeedee_android_chat.view.chat_message.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.EmojiItemLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.visible
import kotlinx.android.extensions.LayoutContainer

class EmojiItemsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mList: List<String>? = listOf()

    var onClickListener: ((String) -> Unit)? = null

    fun setData(list: List<String>) {
        mList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingLeft: EmojiItemLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.emoji_item_layout, parent,
            false
        )
        return EmojiViewHolder(bindingLeft)
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is EmojiViewHolder) {
            holder.itemBinding.emojiText = mList!![position]
            holder.itemBinding.atvEmojiText.setOnClickListener {
                onClickListener?.invoke(mList!![position])
            }
            holder.itemBinding.aivAddEmoji.setOnClickListener {
                onClickListener?.invoke(mList!![position])
            }
            if (mList!![position] == "+") {
                holder.itemBinding.aivAddEmoji.visible()
                holder.itemBinding.atvEmojiText.gone()
            } else {
                holder.itemBinding.atvEmojiText.visible()
                holder.itemBinding.aivAddEmoji.gone()
            }
        }
    }

    class EmojiViewHolder(var itemBinding: EmojiItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}