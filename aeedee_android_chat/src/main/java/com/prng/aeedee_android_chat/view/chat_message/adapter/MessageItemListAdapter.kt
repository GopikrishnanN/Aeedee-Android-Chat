package com.prng.aeedee_android_chat.view.chat_message.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.prng.aeedee_android_chat.R
import com.prng.aeedee_android_chat.databinding.LeftDeletedMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.LeftForwardMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.LeftImageMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.LeftReplyMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.LeftTextMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.RightDeletedMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.RightForwardMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.RightImageMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.RightReplyMessageLayoutBinding
import com.prng.aeedee_android_chat.databinding.RightTextMessageLayoutBinding
import com.prng.aeedee_android_chat.gone
import com.prng.aeedee_android_chat.setConstraintLayoutWidthToPercent
import com.prng.aeedee_android_chat.util.UserIdData
import com.prng.aeedee_android_chat.view.chat_message.ChatActivity
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.message.FileData
import com.prng.aeedee_android_chat.visible
import kotlinx.android.extensions.LayoutContainer

class MessageItemListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mList: List<MessageDataResponse>? = listOf()

    var onLongClickListener: ((MessageDataResponse, View) -> Unit)? = null

    var onSelectionClickListener: ((Boolean) -> Unit)? = null

    var isSelection: Boolean = false

    private var mUserData: UserIdData? = null

    fun setData(list: List<MessageDataResponse>) {
        mList = list
    }

    fun addData(list: MessageDataResponse) {
        if (mList!!.isNotEmpty()) {
            (mList as ArrayList).add(list)
        } else {
            mList = arrayListOf(list)
        }
    }

    fun updateData(position: Int, list: MessageDataResponse) {
        (mList as ArrayList)[position] = list
    }

    fun getAllItems(): List<MessageDataResponse> {
        return mList ?: arrayListOf()
    }

    fun getSelectedIds(): List<String> {
        return mList!!.filter { it.isSelectEnable }.map { it.unique_id.toString() }
    }

    private fun selectionMessage(aivSelectMessage: View, position: Int) {
        aivSelectMessage.apply { if (this.visibility != visible()) visible() else gone() }
        mList!![position].isSelectEnable = !mList!![position].isSelectEnable
        val list = mList!!.filter { it.isSelectEnable }.map { it.isSelectEnable }
        isSelection = if (list.isNotEmpty()) list.first() else false
        onSelectionClickListener?.invoke(isSelection)
    }

    fun selectMessage(data: MessageDataResponse, index: Int) {
        (mList as ArrayList)[index] = data
    }

    fun setUserData(userData: UserIdData) {
        mUserData = userData
    }

    override fun getItemViewType(position: Int): Int {
        val data = mList!![position]
        val isLeftMessage = data.userId != ChatActivity.userId

        return if (!isLeftMessage && data.chat_type == "forward") 9 // Right Forward
        else if (isLeftMessage && data.chat_type == "forward") 8 // Left Forward
        else if (!isLeftMessage && data.status == 0) 7 // Right Deleted
        else if (isLeftMessage && data.status == 0) 6 // Left Deleted
        else if (!isLeftMessage && getFileStatus(data.files)) 5 // Right Images
        else if (isLeftMessage && getFileStatus(data.files)) 4 // Left Images
        else if (!isLeftMessage && getRepliedStatus(data)) 3 // Right Replied
        else if (isLeftMessage && getRepliedStatus(data)) 2 // Left Replied
        else if (!isLeftMessage) 1 // Right Text
        else 0 // Left Text
    }

    private fun getRepliedStatus(data: MessageDataResponse): Boolean {
        if (data.repliedId.isNullOrEmpty()) return false
        return true
    }

    private fun getFileStatus(files: List<FileData>?): Boolean {
        if (files.isNullOrEmpty()) return false
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val bindingLeft = LeftTextMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                LeftTextViewHolder(bindingLeft)
            }

            1 -> {
                val bindingRight = RightTextMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                RightTextViewHolder(bindingRight)
            }

            2 -> {
                val bindingLeft = LeftReplyMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                LeftReplyViewHolder(bindingLeft)
            }

            3 -> {
                val bindingRight = RightReplyMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                RightReplyViewHolder(bindingRight)
            }

            4 -> {
                val bindingLeft = LeftImageMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                LeftImageViewHolder(bindingLeft)
            }

            5 -> {
                val bindingRight = RightImageMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                RightImageViewHolder(bindingRight)
            }

            6 -> {
                val bindingRight = LeftDeletedMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                LeftDeleteViewHolder(bindingRight)
            }

            7 -> {
                val bindingRight = RightDeletedMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                RightDeleteViewHolder(bindingRight)
            }

            8 -> {
                val bindingRight = LeftForwardMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                LeftForwardViewHolder(bindingRight)
            }

            else -> {
                val bindingRight = RightForwardMessageLayoutBinding.inflate(
                    inflater, parent, false
                )
                RightForwardViewHolder(bindingRight)
            }
        }
    }

    override fun getItemCount(): Int {
        return mList!!.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (holder) {
                is LeftTextViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is RightTextViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is LeftReplyViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is RightReplyViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is LeftImageViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is RightImageViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is LeftDeleteViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is RightDeleteViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is LeftForwardViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }

                is RightForwardViewHolder -> {
                    holder.itemBinding.data = mList!![position]
                    holder.itemBinding.executePendingBindings()
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LeftTextViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clLeftTexLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clLeftTextMessage)
                    }
                    return@setOnLongClickListener true
                }

                binding.atvLeftTextMessage.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clLeftTextMessage)
                    }
                    return@setOnLongClickListener true
                }

            }

            is RightTextViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clMessageViewLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clViewLayout)
                    }
                    return@setOnLongClickListener true
                }

                binding.clSelectionLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }

                binding.clMessageViewLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }
            }

            is LeftReplyViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]
                binding.receiverId = mUserData

                setConstraintLayoutWidthToPercent(binding.root.context, binding.clOverlayLayout)

                if (mList!![position].replyImage!!.isNotEmpty())
                    Glide.with(binding.root.context).load(mList!![position].replyImage)
                        .into(binding.atvRightReplyImage)

                binding.clOverlayLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clOverlayLayout)
                    }
                    return@setOnLongClickListener true
                }
            }

            is RightReplyViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]
                binding.receiverId = mUserData

                setConstraintLayoutWidthToPercent(binding.root.context, binding.clOverlayLayout)

                if (mList!![position].replyImage?.isNotEmpty() == true)
                    Glide.with(binding.root.context).load(mList!![position].replyImage)
                        .into(binding.atvLeftReplyImage)

                binding.clOverlayLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clOverlayLayout)
                    }
                    return@setOnLongClickListener true
                }

                binding.clSelectionLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }

                binding.clOverlayLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }
            }

            is LeftImageViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clMessageLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clMessageLayout)
                    }
                    return@setOnLongClickListener true
                }

                val circularProgressDrawable =
                    CircularProgressDrawable(binding.root.context).apply {
                        strokeWidth = 5f
                        centerRadius = 30f
                        setColorSchemeColors(
                            ContextCompat.getColor(binding.root.context, R.color.blue),
                            ContextCompat.getColor(binding.root.context, R.color.black),
                            ContextCompat.getColor(binding.root.context, R.color.white),
                        )
                        start()
                    }

                Glide.with(binding.root.context)
                    .asBitmap()
                    .load(mList!![position].files!!.first().url)
                    .placeholder(circularProgressDrawable)
                    .into(binding.aivMessageImage)
            }

            is RightImageViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clMessageLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clMessageLayout)
                    }
                    return@setOnLongClickListener true
                }

                binding.clSelectionLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }

                binding.clMessageLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }

//                val radius = 50f // Adjust the radius as needed
//                val mask = SideCurvedDrawable(radius)

                val circularProgressDrawable =
                    CircularProgressDrawable(binding.root.context).apply {
                        strokeWidth = 5f
                        centerRadius = 30f
                        setColorSchemeColors(
                            ContextCompat.getColor(binding.root.context, R.color.blue),
                            ContextCompat.getColor(binding.root.context, R.color.black),
                            ContextCompat.getColor(binding.root.context, R.color.white),
                        )
                        start()
                    }

                Glide.with(binding.root.context)
                    .asBitmap()
                    .load(mList!![position].files!!.first().url)
                    .placeholder(circularProgressDrawable)
                    .into(binding.aivMessageImage)

//                    .into(object : CustomTarget<Bitmap>() {
//                        override fun onResourceReady(
//                            resource: Bitmap, transition: Transition<in Bitmap>?
//                        ) {
//                            val output = Bitmap.createBitmap(
//                                resource.width, resource.height, Bitmap.Config.ARGB_8888
//                            )
//                            val canvas = Canvas(output)
//                            mask.setBounds(0, 0, canvas.width, canvas.height)
//                            mask.draw(canvas)
//                            val paint = Paint()
//                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//                            canvas.drawBitmap(resource, 0f, 0f, paint)
//                            binding.aivMessageImage.setImageBitmap(output)
//                            binding.aivMessageImage.setImageBitmap(resource)
//                        }
//
//                        override fun onLoadCleared(placeholder: Drawable?) {
//                            // Handle cleanup if needed
//                        }
//                    })
            }

            is LeftDeleteViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]
                binding.executePendingBindings()
            }

            is RightDeleteViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]
                binding.executePendingBindings()
            }

            is LeftForwardViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clLeftTexLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clLeftTextMessage)
                    }
                    return@setOnLongClickListener true
                }
            }

            is RightForwardViewHolder -> {
                val binding = holder.itemBinding
                binding.data = mList!![position]

                binding.clMessageViewLayout.setOnLongClickListener {
                    if (!isSelection) {
                        onLongClickListener?.invoke(mList!![position], binding.clViewLayout)
                    }
                    return@setOnLongClickListener true
                }

                binding.clSelectionLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }

                binding.clMessageViewLayout.setOnClickListener {
                    if (isSelection) {
                        selectionMessage(binding.aivSelectMessage, position)
                    }
                }
            }
        }
    }

    // Left Text
    class LeftTextViewHolder(var itemBinding: LeftTextMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Right Text
    class RightTextViewHolder(var itemBinding: RightTextMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Left Reply
    class LeftReplyViewHolder(var itemBinding: LeftReplyMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Right Reply
    class RightReplyViewHolder(var itemBinding: RightReplyMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Left Images
    class LeftImageViewHolder(var itemBinding: LeftImageMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Right Images
    class RightImageViewHolder(var itemBinding: RightImageMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Left Delete
    class LeftDeleteViewHolder(var itemBinding: LeftDeletedMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Right Images
    class RightDeleteViewHolder(var itemBinding: RightDeletedMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Left Forward
    class LeftForwardViewHolder(var itemBinding: LeftForwardMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }

    // Right Forward
    class RightForwardViewHolder(var itemBinding: RightForwardMessageLayoutBinding) :
        RecyclerView.ViewHolder(itemBinding.root), LayoutContainer {
        override val containerView: View get() = itemBinding.root
    }
}