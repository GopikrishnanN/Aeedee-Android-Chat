package com.prng.aeedee_android_chat

import android.animation.ObjectAnimator
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@BindingAdapter("app:srcCompat")
fun setSrcCompat(imageView: ImageView, resource: Any) {
    val requestOptions = RequestOptions().placeholder(android.R.color.transparent)
    when (resource) {
        is String -> { // Assume it's a URL
            Glide.with(imageView.context)
                .load(resource)
                .apply(requestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }

        is Int -> { // Assume it's a drawable resource ID
            Glide.with(imageView.context)
                .load(resource)
                .apply(requestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }

        else -> {
            Glide.with(imageView.context)
                .load(resource)
                .apply(requestOptions)
                .into(imageView)
        }
    }
}

@BindingAdapter("app:imageCompat")
fun setImageCompat(imageView: AppCompatImageView, resource: Any) {
    val requestOptions = RequestOptions().placeholder(android.R.color.transparent)
    Glide.with(imageView.context).load(resource).apply(requestOptions).into(imageView)
}

@BindingAdapter("android:dateTime")
fun setDateTime(textView: AppCompatTextView, dateTime: String?) {
    if (dateTime != null) {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (dateTime.isNotEmpty()) {
            val date = inputFormat.parse(dateTime)
            textView.text = date?.let { outputFormat.format(it) }
        } else {
            textView.text = "..."
        }
    } else {
        textView.text = "..."
    }
}

@BindingAdapter("android:messageTime")
fun setMessageTime(textView: AppCompatTextView, dateTime: String?) {
    if (dateTime != null) {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (dateTime.isNotEmpty()) {
            val date = inputFormat.parse(dateTime)
            textView.text = date?.let { outputFormat.format(it) }.toString().lowercase()
        } else {
            textView.text = "..."
        }
    } else {
        textView.text = "..."
    }
}

@BindingAdapter("android:messageDateTime")
fun setMessageDateTime(textView: AppCompatTextView, dateTime: String?) {
    val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    originalFormat.timeZone = TimeZone.getTimeZone("UTC")
    textView.text = if (dateTime != null) {
        val date: Date? = originalFormat.parse(dateTime)

        val targetFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        if (date != null) {
            if (msgDateTimeConvert(dateTime) == getCurrentDate()) {
                "Today"
            } else {
                targetFormat.format(date).uppercase(Locale.getDefault())
            }
        } else ""
    } else ""
}

@BindingAdapter("android:visibility")
fun setVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("android:visibilityReaction")
fun setVisibilityReaction(view: AppCompatTextView, data: MutableList<DatabaseReactionData>?) {
    if (data != null) {
        if (data.isNotEmpty()) {
            view.text = data.first().message
//            animateEmoji(view)
        } else view.gone()
//        val sb = StringBuilder()
//        if (data.isNotEmpty()) {
//            val size = data.size
//            for (i in data.indices) {
//                sb.append(data[i].message)
//                if ((size - 1) != i) sb.append(" ")
//                if (size > 1 && (size - 1) == i) sb.append(size.toString())
//                view.visible()
//                animateEmoji(view)
//            }
//            view.text = sb.toString()
//        } else view.gone()
    } else view.gone()
}

private fun animateEmoji(view: AppCompatTextView) {
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.0f, 1.0f)
    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.0f, 1.0f)
    scaleX.duration = 500
    scaleY.duration = 500

    val bounceInterpolator = android.view.animation.BounceInterpolator()
    scaleX.interpolator = bounceInterpolator
    scaleY.interpolator = bounceInterpolator

    scaleX.start()
    scaleY.start()
}

@BindingAdapter("app:visibilityGone")
fun setVisibilityGone(view: View, isVisible: Boolean?) {
    view.visibility = if (isVisible == true) View.VISIBLE else View.GONE
}

@BindingAdapter("app:typefaceStyle")
fun setTypefaceStyle(textView: AppCompatTextView, count: Int) {
    textView.setTypeface(null, if (count > 0) Typeface.BOLD else Typeface.NORMAL)
}
