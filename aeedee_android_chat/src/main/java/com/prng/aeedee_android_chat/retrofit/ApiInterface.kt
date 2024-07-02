package com.prng.aeedee_android_chat.retrofit

import com.prng.aeedee_android_chat.view.chat.model.ChatUserRequest
import com.prng.aeedee_android_chat.view.chat.model.ChatUserResponse
import com.prng.aeedee_android_chat.view.chat.model.DeleteUserRequest
import com.prng.aeedee_android_chat.view.chat_message.model.ChatReadStatusRequest
import com.prng.aeedee_android_chat.view.chat_message.model.ImageUploadResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageListResponse
import com.prng.aeedee_android_chat.view.chat_message.model.MessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.MessageResponse
import com.prng.aeedee_android_chat.view.chat_message.model.SendMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageRequest
import com.prng.aeedee_android_chat.view.chat_message.model.message.DeleteMessageResponse
import com.prng.aeedee_android_chat.view.chat_user_bottom.model.UsersListResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {

    @POST("/user/mobile/msg-userlist")
    fun getChatUserList(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: ChatUserRequest
    ): Call<ChatUserResponse>

    @POST("/user/mobile/msg-list")
    fun getMessageList(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: MessageRequest,
    ): Call<MessageListResponse>

    @POST("/user/mobile/msg-add")
    fun sendMessageApiCall(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: SendMessageRequest,
    ): Call<MessageResponse>

    @POST("/user/mobile/friend-lists")
    fun getFollowersList(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
    ): Call<UsersListResponse>

    @Multipart
    @POST("/user/mobile/msg-image")
    fun uploadImage(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Part file: MultipartBody.Part,
        @Part("fileType") fileType: String,
    ): Call<ImageUploadResponse>

    @POST("/user/mobile/msg-delete")
    fun deleteMessageData(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: DeleteMessageRequest,
    ): Call<DeleteMessageResponse>

    @POST("/user/mobile/delete-chathistory")
    fun deleteUserItem(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: DeleteUserRequest,
    ): Call<DeleteMessageResponse>

    @POST("/user/mobile/message-read")
    fun updateChatReadStatusApiCall(
        @Header("Authorization") authorization: String,
        @Header("user-id") userId: String,
        @Body request: ChatReadStatusRequest,
    ): Call<DeleteMessageResponse>

}