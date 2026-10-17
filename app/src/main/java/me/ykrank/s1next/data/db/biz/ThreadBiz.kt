package me.ykrank.s1next.data.db.biz

import androidx.annotation.WorkerThread
import me.ykrank.s1next.data.db.AppDatabase
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.data.db.dbmodel.DbThread

class ThreadBiz(private val manager: AppDatabaseManager) {

    private val threadDao
        get() = session.thread()

    private val session: AppDatabase
        get() = manager.getOrBuildDb()

    @WorkerThread
    fun getWithThreadId(threadId: Int): DbThread? {
        return threadDao.getByThreadId(threadId)
    }

    /**
     * 批量查询 DbThread，用于列表场景避免 N+1 查询。
     * 返回 threadId -> DbThread 的 Map，方便调用方 O(1) 命中。
     */
    @WorkerThread
    fun getWithThreadIds(threadIds: List<Int>): Map<Int, DbThread> {
        if (threadIds.isEmpty()) return emptyMap()
        return threadDao.getByThreadIds(threadIds).associateBy { it.threadId }
    }

    @WorkerThread
    fun saveThread(dbThread: DbThread) {
        val oDbThread = getWithThreadId(dbThread.threadId)
        if (oDbThread == null) {
            threadDao.insert(dbThread)
        } else {
            oDbThread.copyFrom(dbThread)
            threadDao.update(oDbThread)
        }
    }

    @WorkerThread
    fun delThread(threadId: Int) {
        val oDbThread = getWithThreadId(threadId)
        if (oDbThread != null) {
            threadDao.delete(oDbThread)
        }
    }

    companion object {

        val instance: ThreadBiz
            get() = bizDependencies().threadBiz
    }
}
