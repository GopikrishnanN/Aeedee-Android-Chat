package com.prng.aeedee_android_chat.view.forward_chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse

class ForwardUsersViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<ChatUserResponse>? = MutableLiveData(null)

    var mSearchText: String = ""

    var onSearchListener: ((ChatUserRequest) -> Unit)? = null

    fun getChatUserList(request: ChatUserRequest): LiveData<ChatUserResponse>? {
        usersLiveData = ChatActivityRepository.getChatUserListApiCall(userId = userID, request)
        return usersLiveData
    }

    fun afterSearchTextChanged(char: CharSequence, start: Int, end: Int, count: Int) {
        mSearchText = char.toString()
        val request = ChatUserRequest(limit = 50, search = mSearchText.trim())
        onSearchListener?.invoke(request)
    }
}