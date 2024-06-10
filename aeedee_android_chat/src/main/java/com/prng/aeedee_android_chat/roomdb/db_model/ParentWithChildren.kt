package com.prng.aeedee_android_chat.roomdb.db_model

import androidx.room.Embedded
import androidx.room.Relation
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageData
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
data class ParentWithChildren(
    @Embedded val parent: DatabaseMessageData?,
    @Relation(parentColumn = "originId", entityColumn = "originId")
    val children: List<DatabaseMessageModel>?
)
