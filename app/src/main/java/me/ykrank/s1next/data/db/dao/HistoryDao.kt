package me.ykrank.s1next.data.db.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.dbmodel.History


@Dao
interface HistoryDao {
    @Query("SELECT * FROM History LIMIT :limit OFFSET :offset")
    fun loadLimit(limit: Int, offset: Int): List<History>

    @Query("SELECT * FROM History ORDER BY Timestamp DESC LIMIT :limit")
    fun loadCursor(limit: Int): Cursor

    @Query("SELECT * FROM History WHERE ThreadId == :threadId LIMIT 1")
    fun getByThreadId(threadId: Int): History?

    @Query("DELETE FROM History WHERE _id NOT IN ( SELECT _id FROM History ORDER BY Timestamp DESC LIMIT :maxSize)")
    fun deleteNotTopRecords(
        maxSize: Int = HistoryBiz.MAX_SIZE
    ): Int

    @Insert
    fun insert(history: History)

    @Delete
    fun delete(history: List<History>)

    @Update
    fun update(history: History)

    @Query("SELECT COUNT(*) FROM History")
    fun getCount(): Int
}