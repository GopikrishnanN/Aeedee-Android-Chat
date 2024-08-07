package com.prng.aeedee_android_chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prng.aeedee_android_chat.retrofit.RetrofitClient
import com.prng.aeedee_android_chat.util.URIPathHelper
import com.prng.aeedee_android_chat.view.chat_message.model.MessageMenuData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern

// Socket Event Name's
const val START_TYPING = "start_typing"
const val STOP_TYPING = "stop_typing"
const val NEW_MESSAGE = "newmsg"
const val CHAT_CONNECT = "chat_connect"
const val CHAT_DISCONNECT = "chat_disconnect"
const val ACTIVE_TIME = "active_time"
const val REACTION = "reaction"
const val READ_STATUS = "read_status"
const val DELETE_MESSAGE = "delete-msg"

// Global User Id
var userID = "65f2d9b84c342fb51e72343f"
//var userID = "65f29bd9c4f2640a7a24d99c"
//var userID = "6651bf9e6508b954311c0afb"
//var userID = "664ca37f1cabc43f39b51ba4"
//var userID = "65ddbed3f98eadc6bee76361"

var userName = "Nitheesh Kumar"

// Layout Size
const val wrapContent = WindowManager.LayoutParams.WRAP_CONTENT
const val matchParent = WindowManager.LayoutParams.MATCH_PARENT

// Message Type
enum class MessageType {
    Forward,
    Normal,
    Reply
}

// Payload Content
enum class Payload {
    Update
}

// Local Emoji List
//val emojiList = arrayListOf("😂", "😄", "😍", "😨", "😔", "😡"/*, "+"*/)
//val emojiList = arrayListOf("&#x1F604;", "&#x1FAE3;", "&#x1F614;", "&#x1F62E;", "&#x1F621;)
val emojiList = arrayListOf(
    "\uD83D\uDE04",  // Smiley 😄
    "\uD83E\uDEE3",  // One eye open 🫣
    "\uD83D\uDE14",  // Sad 😔
    "\uD83D\uDE2E",  // Shock 😮
    "\uD83D\uDE21",  // Angry 😡
    "+"              // Plus ➕
)

// Local Message Menu List
val messageMenuList = arrayListOf(
    MessageMenuData(name = "Reply", icon = R.drawable.ic_reply_icon, id = 0),
    MessageMenuData(name = "Copy", icon = R.drawable.ic_copy_icon, id = 1),
    MessageMenuData(name = "Forward", icon = R.drawable.ic_forward_icon, id = 2),
    MessageMenuData(name = "Delete", icon = R.drawable.ic_recycle_bin_icon, id = 3),
)

inline fun <reified T> JSONObject.toDataClass(): T? {
    return try {
        Gson().fromJson(this.toString(), T::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun msgDateTimeConvert(date: String): String = getDate(date)

fun getDate(dateTime: String): String {
    val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    originalFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date: Date? = originalFormat.parse(dateTime)

    // Format the date to the desired format
    val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return if (date != null) {
        targetFormat.format(date).uppercase(Locale.getDefault())
    } else {
        ""
    }
}

fun getCurrentDateTime(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date())
}

fun getCurrentDate(): String {
    val currentDate = Date()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(currentDate)
}

fun isNetworkConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkInfo = connectivityManager.activeNetworkInfo

    return networkInfo != null && networkInfo.isConnectedOrConnecting
}

fun Any.toast(context: Context): Toast {
    return Toast.makeText(context, this.toString(), Toast.LENGTH_SHORT)
}

inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson(json, object : TypeToken<T>() {}.type)

fun <T> JSONObject.getOrDefault(key: String, default: T): T {
    return try {
        when (default) {
            is String -> getString(key) as T
            is Int -> getInt(key) as T
            is Boolean -> getBoolean(key) as T
            is Double -> getDouble(key) as T
            is Long -> getLong(key) as T
            is JSONArray -> getJSONArray(key) as T
            is JSONObject -> getJSONObject(key) as T
            else -> throw IllegalArgumentException("Unsupported default type")
        }
    } catch (e: JSONException) {
        default
    }
}

fun View.visible(): Int {
    this.visibility = View.VISIBLE
    return this.visibility
}

fun View.gone(): Int {
    this.visibility = View.GONE
    return this.visibility
}

fun View.invisible(): Int {
    this.visibility = View.INVISIBLE
    return this.visibility
}

fun String.replaceNextLineToEmpty(): String {
    return replace("\n", "")
}

fun String.getLastPathSegmentOrUrl(): Pair<String, Boolean> {
    if (this.contains(RetrofitClient.baseUrl)) {
        val cleanUrl = if (this.endsWith("/")) this.dropLast(1) else this
        val lastSegment = cleanUrl.substringAfterLast("/")
        return Pair(lastSegment, true)
    }
    return Pair(this, false)
}

fun getTimeZone(): String {
    val timeZone = TimeZone.getDefault()
    return timeZone.id
}

fun getUniqueId(): String {
    val secureRandom = SecureRandom()
    val randomBytes = ByteArray(8)
    secureRandom.nextBytes(randomBytes)
    return BigInteger(1, randomBytes).toString(14)
}

fun copyToClipboard(text: String, context: Context) {
    val clipboard: ClipboardManager? =
        context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
}

// Function to get screen height
fun getScreenHeight(context: Context): Int {
    val displayMetrics = DisplayMetrics()
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}

// Function to get screen width
fun getScreenWidth(context: Context): Int {
    val displayMetrics = DisplayMetrics()
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(
        displayMetrics
    )
    return displayMetrics.widthPixels
}

fun setConstraintLayoutWidthToPercent(context: Context, constraintLayout: ConstraintLayout) {
    val screenWidth = getScreenWidth(context)
    val targetWidth = (screenWidth * 0.7).toInt()

    val layoutParams = constraintLayout.layoutParams as ViewGroup.LayoutParams
    layoutParams.width = targetWidth
    constraintLayout.layoutParams = layoutParams
}

fun getMimeType(context: Context, filePath: String): String? {
    val file = File(filePath)
    val uri = Uri.fromFile(file)
    return getMimeType(context, uri)
}

fun getMimeType(context: Context, uri: Uri): String? {
    val contentResolver = context.contentResolver
    return contentResolver.getType(uri) ?: run {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
    }
}

fun hideKeyboardFrom(context: Context, view: View) {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun extractFirstUrl(text: String): String? {
    val urlPattern = "(https?://\\S+)"
    val pattern: Pattern = Pattern.compile(urlPattern)
    val matcher: Matcher = pattern.matcher(text)

    return if (matcher.find()) matcher.group(1) else null
}

fun fetchUriPath(context: Context, uri: Uri?): String? {
    uri?.let {
        val uriPathHelper = URIPathHelper()
        return uriPathHelper.getPath(context, it)
    }
    return null
}

fun gifCircularProgress(
    context: Context, strokeWidth: Float = 5f, centerRadius: Float = 30f
): CircularProgressDrawable {
    return CircularProgressDrawable(context).apply {
        this.strokeWidth = strokeWidth
        this.centerRadius = centerRadius
        setColorSchemeColors(
            ContextCompat.getColor(context, R.color.blue),
            ContextCompat.getColor(context, R.color.black),
            ContextCompat.getColor(context, R.color.white)
        )
        start()
    }
}

fun displayHtml(html: String, atv: AppCompatTextView) {

    val htmlForm = html.replace("\n", "<br>")

    val styledText = HtmlCompat.fromHtml(htmlForm, HtmlCompat.FROM_HTML_MODE_LEGACY)

    atv.movementMethod = LinkMovementMethod.getInstance()
    atv.text = styledText
}

fun textToDrawable(context: Context, text: String): Drawable {

    val textView = TextView(context).apply {
        this.text = text.uppercase(Locale.ROOT)
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        this.setTextColor(Color.WHITE)
        this.setBackgroundColor(Color.parseColor("#F39D11"))
        this.setPadding(25, 20, 25, 25)
    }

    textView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    val width = textView.measuredWidth
    val height = textView.measuredHeight

    textView.layout(0, 0, width, height)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    textView.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
}