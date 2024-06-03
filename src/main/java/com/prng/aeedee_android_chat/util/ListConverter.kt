package com.prng.aeedee_android_chat.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prng.aeedee_android_chat.fromJson
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseFileData
import com.prng.aeedee_android_chat.view.chat_message.model.message.DatabaseReactionData

class ListConverter {

    @TypeConverter
    fun fromList(list: List<DatabaseMessageModel>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toList(json: String): List<DatabaseMessageModel> {
        return Gson().fromJson(json)
    }

    @TypeConverter
    fun fromFileData(json: String): MutableList<DatabaseFileData> {
        val type = object : TypeToken<MutableList<DatabaseFileData>>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun toFileData(torrent: MutableList<DatabaseFileData>): String {
        val type = object : TypeToken<MutableList<DatabaseFileData>>() {}.type
        return Gson().toJson(torrent, type)
    }

    @TypeConverter
    fun fromReactionData(json: String): MutableList<DatabaseReactionData> {
        val type = object : TypeToken<MutableList<DatabaseReactionData>>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun toReactionData(torrent: MutableList<DatabaseReactionData>): String {
        val type = object : TypeToken<MutableList<DatabaseReactionData>>() {}.type
        return Gson().toJson(torrent, type)
    }
}