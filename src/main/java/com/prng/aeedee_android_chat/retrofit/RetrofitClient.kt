package com.prng.aeedee_android_chat.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val baseUrl = "https://dev.aeedee.com"

    val retrofitClient: Retrofit.Builder by lazy {

//        val levelType: Level = if (BuildConfig.BUILD_TYPE.contentEquals("debug"))
//            Level.BODY else Level.NONE

        val levelType: Level = Level.BODY

        val logging = HttpLoggingInterceptor()
        logging.setLevel(levelType)

        val okhttpClient = OkHttpClient.Builder()
        okhttpClient.addInterceptor(logging)

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okhttpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
    }

    val apiInterface: ApiInterface by lazy {
        retrofitClient.build().create(ApiInterface::class.java)
    }
}
