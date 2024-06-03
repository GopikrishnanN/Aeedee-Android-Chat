package com.prng.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.widget.AppCompatButton
import com.prng.aeedee_android_chat.view.chat.ChatUserListActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(Intent(this, ChatUserListActivity::class.java))
            finish()
        }, 3000)

    }
}