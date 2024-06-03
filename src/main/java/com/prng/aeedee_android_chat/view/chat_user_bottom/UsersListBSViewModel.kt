package com.prng.aeedee_android_chat.view.chat_user_bottom

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prng.aeedee_android_chat.repository.ChatActivityRepository
import com.prng.aeedee_android_chat.userID
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersListResponse

class UsersListBSViewModel : ViewModel() {

    private var usersLiveData: MutableLiveData<UsersListResponse>? = null

    private val auth =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImdhbmVzaC5pbmZvLmtAZ21haWwuY29tIiwiaWQiOiI2NWIxZjMzN2M2NWQ3ODc0NWRmMTVjYTEiLCJpYXQiOjE3MDgwMDAwMDl9.w7igawY4BRsbQpXrU6t3HuWv1e2lOzNmogZ345SYi9M"

    var onSearchListener: ((String) -> Unit)? = null

    fun getUsersList(): LiveData<UsersListResponse>? {
        usersLiveData = ChatActivityRepository.getFollowersListApiCall(userId = userID, auth = auth)
        return usersLiveData
    }

    fun afterTextChanged(s: Editable) {
        onSearchListener?.invoke(s.trim().toString())
    }

}