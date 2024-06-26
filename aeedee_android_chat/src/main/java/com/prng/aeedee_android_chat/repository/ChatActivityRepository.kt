package com.prng.aeedee_android_chat.repository

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.prng.aeedee_android_chat.retrofit.RetrofitClient
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import com.prng.aeedee_android_chat.view.chat.model.DeleteUserRequest
import com.prng.aeedee_android_chat.view.chat_message.adapter.MessageItemListAdapter
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadRequest
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageDataResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageListResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.MessageResponse
import com.prng.aeedee_android_chat.view.chat_message.model.SendMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageResponse
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersListResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

object ChatActivityRepository {

    fun getChatUserListApiCall(
        userId: String,
        request: ChatUserRequest
    ): MutableLiveData<ChatUserResponse> {

        val chatUserListSetterGetter = MutableLiveData<ChatUserResponse>()

        val auth =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImdhbmVzaC5pbmZvLmtAZ21haWwuY29tIiwiaWQiOiI2NWIxZjMzN2M2NWQ3ODc0NWRmMTVjYTEiLCJpYXQiOjE3MDgwMDAwMDl9.w7igawY4BRsbQpXrU6t3HuWv1e2lOzNmogZ345SYi9M"
        val call = RetrofitClient.apiInterface.getChatUserList(auth, userId, request)

        call.enqueue(object : Callback<ChatUserResponse> {
            override fun onFailure(call: Call<ChatUserResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<ChatUserResponse>,
                response: Response<ChatUserResponse>
            ) {
                Log.v("DEBUG : ", response.body().toString())

                val data = response.body()

                chatUserListSetterGetter.value = data
            }
        })

        return chatUserListSetterGetter
    }

    fun getChatListApiCall(
        auth: String, userId: String, request: MessageRequest
    ): MutableLiveData<MessageListResponse> {

        val chatSetterGetter = MutableLiveData<MessageListResponse>()

        val call = RetrofitClient.apiInterface.getMessageList(auth, userId, request)

        call.enqueue(object : Callback<MessageListResponse> {
            override fun onFailure(call: Call<MessageListResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<MessageListResponse>, response: Response<MessageListResponse>
            ) {
                Log.v("DEBUG : recent-${request.recent}--", response.body().toString())

                val data = response.body()

                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun sendMessageApiCall(
        auth: String, userId: String, request: SendMessageRequest
    ): MutableLiveData<MessageResponse> {

        val chatSetterGetter = MutableLiveData<MessageResponse>()

        val call = RetrofitClient.apiInterface.sendMessageApiCall(auth, userId, request)

        call.enqueue(object : Callback<MessageResponse> {
            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<MessageResponse>, response: Response<MessageResponse>
            ) {
                Log.v("DEBUG : ---------", response.body().toString())

                val data = response.body()

                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun getFollowersListApiCall(auth: String, userId: String): MutableLiveData<UsersListResponse> {

        val chatSetterGetter = MutableLiveData<UsersListResponse>()

        val call = RetrofitClient.apiInterface.getFollowersList(auth, userId)

        call.enqueue(object : Callback<UsersListResponse> {
            override fun onFailure(call: Call<UsersListResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<UsersListResponse>, response: Response<UsersListResponse>
            ) {
                val data = response.body()

                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun uploadImageApi(
        auth: String, userId: String, request: ImageUploadRequest
    ): MutableLiveData<ImageUploadResponse> {

        val chatSetterGetter = MutableLiveData<ImageUploadResponse>()

        val file = File(request.url)

        if (!file.exists()) {
            Log.e("DEBUG", "File not found: ${request.url}")
            return chatSetterGetter
        }

        val requestFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())

        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("file", file.name, requestFile)

        val call = RetrofitClient.apiInterface.uploadImage(auth, userId, body, "png")

        call.enqueue(object : Callback<ImageUploadResponse> {
            override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>
            ) {
                Log.v("DEBUG : ---------", response.body().toString())

                val data = response.body()
                data?.type = "image"
                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun deleteMessageApiData(
        auth: String, userId: String, request: DeleteMessageRequest
    ): MutableLiveData<DeleteMessageResponse> {

        val chatSetterGetter = MutableLiveData<DeleteMessageResponse>()

        val call = RetrofitClient.apiInterface.deleteMessageData(auth, userId, request)

        call.enqueue(object : Callback<DeleteMessageResponse> {
            override fun onFailure(call: Call<DeleteMessageResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<DeleteMessageResponse>, response: Response<DeleteMessageResponse>
            ) {
                val data = response.body()

                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun deleteUserChatApiCall(
        userId: String, request: DeleteUserRequest
    ): MutableLiveData<DeleteMessageResponse> {
        val auth =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImdhbmVzaC5pbmZvLmtAZ21haWwuY29tIiwiaWQiOiI2NWIxZjMzN2M2NWQ3ODc0NWRmMTVjYTEiLCJpYXQiOjE3MDgwMDAwMDl9.w7igawY4BRsbQpXrU6t3HuWv1e2lOzNmogZ345SYi9M"
        val chatSetterGetter = MutableLiveData<DeleteMessageResponse>()

        val call = RetrofitClient.apiInterface.deleteUserItem(auth, userId, request)

        call.enqueue(object : Callback<DeleteMessageResponse> {
            override fun onFailure(call: Call<DeleteMessageResponse>, t: Throwable) {
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<DeleteMessageResponse>, response: Response<DeleteMessageResponse>
            ) {
                Log.v("DEBUG : ---------", response.body().toString())

                val data = response.body()

                chatSetterGetter.value = data
            }
        })

        return chatSetterGetter
    }

    fun updateLists(
        list: List<MessageDataResponse>, ids: MutableList<String>,
        receiverId: String, chatDao: ChatDao, adapter: MessageItemListAdapter, activity: Activity,
        isDelete: Boolean, onResult: (List<MessageDataResponse>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isDelete) {
                if (list.isNotEmpty()) {
                    val updatedMessages = list.map { item ->
                        if (item.unique_id in ids) item.apply { this.status = 0 } else item
                    }
                    val idsSet = ids.toSet()
                    for (i in updatedMessages.indices) {
                        if (idsSet.contains(updatedMessages[i].unique_id)) {
                            adapter.updateData(i, list = updatedMessages[i])
                            activity.runOnUiThread { adapter.notifyItemChanged(i) }
                        }
                    }
                    updateChildEntityInParent(chatDao, ids, receiverId, 4)
                    adapter.isSelection = false
                    withContext(Dispatchers.IO) { onResult(updatedMessages) }
                }
            } else {
                val resetMessages = list.map { item ->
                    if (item.isSelectEnable) item.isSelectEnable = false
                    item
                }
                val idsSet = ids.toSet()
                for (i in resetMessages.indices) {
                    if (idsSet.contains(resetMessages[i].unique_id)) {
                        adapter.updateData(i, list = resetMessages[i])
                        activity.runOnUiThread { adapter.notifyItemChanged(i) }
                    }
                }
                adapter.isSelection = false
                withContext(Dispatchers.IO) { onResult(resetMessages) }
            }
        }
    }

    private fun updateChildEntityInParent(
        chatDao: ChatDao, childIds: List<String>, receiverId: String, ifData: Int
    ) = CoroutineScope(Dispatchers.IO).launch {
        val idsSet = childIds.toSet()
        val parentWithChildren = chatDao.getParentWithChildren(receiverId)

        if (parentWithChildren != null) {
            if (parentWithChildren.parent?.response != null) {

                val childToUpdate: MutableList<DatabaseMessageModel> =
                    parentWithChildren.parent.response.map {
                        if (ifData == 4 && idsSet.contains(it.uniqueId)) it.status = 0
                        chatDao.updateChildren(it)
                        it
                    }.toMutableList()

                if (childToUpdate != null) {
                    try {
                        val parentToUpdate = parentWithChildren.parent
                        parentToUpdate.let {
                            it.receiverId = receiverId
                            it.response = childToUpdate
                            chatDao.updateParent(it)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

}