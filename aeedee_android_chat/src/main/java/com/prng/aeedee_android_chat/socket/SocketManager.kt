package com.prng.aeedee_android_chat.socket

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.prng.aeedee_android_chat.userID
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("StaticFieldLeak")
object SocketHandler {

    private lateinit var mSocket: Socket
    private lateinit var mContext: Context

    var onSocketStatus: ((Boolean) -> Unit)? = null

    @Synchronized
    fun setSocket(context: Context) {
        try {
// "http://10.0.2.2:3000" is the network your Android emulator must use to join the localhost network on your computer
// "http://localhost:3000/" will not work
// If you want to use your physical phone you could use your ip address plus :3000
// This will allow your Android Emulator and physical device at your home to connect to the server
//            mSocket = IO.socket("http://10.0.2.2:3000")
            mContext = context
            try {
                val socketUrl = "https://dev.aeedee.com/chat"
                val hostnameVerifier = HostnameVerifier { _, _ -> true }
                val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?,
                    ) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?,
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate?> {
                        return arrayOfNulls(0)
                    }
                })
                val trustManager = trustAllCerts[0] as X509TrustManager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, null)
                val sslSocketFactory = sslContext.socketFactory
                val okHttpClient: OkHttpClient =
                    OkHttpClient.Builder().hostnameVerifier(hostnameVerifier)
                        .sslSocketFactory(sslSocketFactory, trustManager).build()
                val opts = IO.Options()
                opts.query = "?id=$userID"
                Log.e("TAG", "sslSocket: " + opts.query)
                opts.callFactory = okHttpClient
                opts.webSocketFactory = okHttpClient
                mSocket = IO.socket(socketUrl, opts)

            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }

        } catch (_: URISyntaxException) {
        }
    }

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun onConnection() {
        mSocket.on(Socket.EVENT_CONNECT, onConnected)
    }

    private val onConnected = Emitter.Listener { _ ->
        if (mSocket.connected()) {
            onSocketStatus?.invoke(true)
            Log.e("TAG", "Socket Connected")
        }
    }

    @Synchronized
    fun onDisconnection() {
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected)
    }

    private val onDisconnected = Emitter.Listener { _ ->
        if (!mSocket.connected()) {
            Log.e("TAG", "Socket Disconnected")
            onSocketStatus?.invoke(false)
        }
    }

    @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }

    fun offEvents() {
        mSocket.off(Socket.EVENT_CONNECT, onConnected)
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected)
    }
}