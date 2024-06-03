package com.prng.aeedee_android_chat.view.chat_message.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.MessageMenuItemLayoutBinding
import com.prng.aeedee_android_chat.view.chat_message.model.MessageMenuData
import kotlinx.android.extensions.LayoutContainer

class MenuItemsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mList: List<MessageMenuData>? = listOf()

    var onClickListener: ((MessageMenuData) -> Unit)? = null

    fun setData(list: List<MessageMenuData>) {
        mList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingLeft: MessageMenuItemLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.message_menu_item_layout, parent,
            false
        )
        return ViewHolder(bindingLeft)
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.itemBinding.menu = mList!![position]
            holder.itemBinding.atvMenuItemText.setOnClickListener {
                onClickListener?.invoke(mList!![position])
            }
            if (mList!!.size - 1 == position)
                holder.itemBinding.vDivider.visibility = View.GONE
            else
                holder.itemBinding.vDivider.visibility = View.VISIBLE

            val selectedColor = ContextCompat.getColor(
                holder.itemBinding.root.context, android.R.color.holo_red_light
            )

            if (mList!![position].id == 3) {
                holder.itemBinding.atvMenuItemText.setTextColor(selectedColor)
                holder.itemBinding.aivMenuIcon.setColorFilter(ContextCompat.getColor(holder.itemBinding.root.context, android.R.color.holo_red_light))
            }
        }
    }

    class ViewHolder(var itemBinding: MessageMenuItemLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}