package me.ykrank.s1next.util

import android.content.Context

/**
 * Created by ykrank on 2017/8/9.
 */
object BuglyUtils {
    val isPlay: Boolean
        get() = false

    fun init(context: Context) {
        // 禁用 Bugly：不调用 CrashReport.initCrashReport / setIsDevelopmentDevice
    }
}
