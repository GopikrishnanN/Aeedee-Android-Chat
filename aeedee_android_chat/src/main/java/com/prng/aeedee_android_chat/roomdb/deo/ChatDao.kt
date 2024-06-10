package com.prng.aeedee_android_chat.roomdb.deo

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import com.prng.aeedee_android_chat.roomdb.db_model.ParentWithChildren
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageData
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseMessageModel
import com.prng.aeedee_android_chat.roomdb.entity_model.DatabaseUsersModel
import com.prng.aeedee_android_chat.util.ListConverter

@Dao
interface ChatDao {

    @Query("select * from DatabaseUsersModel")
    fun getUserAll(): LiveData<List<DatabaseUsersModel>?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsersAll(users: List<DatabaseUsersModel>)

    @Query("DELETE FROM DatabaseUsersModel")
    fun deleteAllUsers()

    @Transaction
    fun replaceUsers(users: List<DatabaseUsersModel>) {
        deleteAllUsers()
        insertUsersAll(users)
    }

    //.........................................................................

    @Query("select * from DatabaseMessageData")
    fun getMessageAll(): LiveData<List<DatabaseMessageData>?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageDataUsers(databaseMessageData: DatabaseMessageData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildren(children: List<DatabaseMessageModel>)

    @Transaction
    @Update
    suspend fun updateParentWithChildren(
        parent: DatabaseMessageData, children: List<DatabaseMessageModel>
    ) {
        insertMessageDataUsers(parent)
        insertChildren(children)
    }

    @Query("SELECT * FROM DatabaseMessageData WHERE originId = :parentId")
    suspend fun getParentWithChildren(parentId: String): ParentWithChildren

    @Update
    suspend fun updateParent(children: DatabaseMessageData)

    @Update
    suspend fun updateChildren(children: DatabaseMessageModel)
}

@Database(
    entities = [DatabaseUsersModel::class, DatabaseMessageData::class, DatabaseMessageModel::class],
    version = 1, exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract val chatDao: ChatDao
}