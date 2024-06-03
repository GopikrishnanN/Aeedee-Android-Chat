package com.prng.aeedee_android_chat.roomdb.di

import android.content.Context
import androidx.room.Room
import com.prng.aeedee_android_chat.roomdb.deo.ChatDao
import com.prng.aeedee_android_chat.roomdb.deo.ChatDatabase

object DatabaseModule {

    fun provideAppDatabase(appContext: Context): ChatDatabase {
        return Room.databaseBuilder(
            appContext,
            ChatDatabase::class.java,
            "Chats"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
    }

    fun provideChannelDao(usersDatabase: ChatDatabase): ChatDao {
        return usersDatabase.chatDao
    }

}