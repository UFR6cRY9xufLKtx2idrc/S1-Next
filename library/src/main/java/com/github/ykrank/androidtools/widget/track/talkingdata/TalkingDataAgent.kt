package com.github.ykrank.androidtools.widget.track.talkingdata

import android.app.Activity
import android.content.Context

import com.github.ykrank.androidtools.data.TrackUser
import com.github.ykrank.androidtools.widget.track.TrackAgent

/**
 * Created by ykrank on 2016/12/28.
 * Agent for talking data proxy
 *
 * 已禁用：所有方法体清空，不再调用 TalkingDataSDK。
 * 保留类名与 TrackAgent 实现，避免改动 DataTrackAgent 的依赖关系。
 */
class TalkingDataAgent : TrackAgent {

    override fun init(context: Context) {
        // 禁用 TalkingData：不调用 TalkingDataSDK.initSDK / startA
    }

    override fun setUser(user: TrackUser) {
        // 禁用
    }

    override fun onResume(activity: Activity) {
        // 禁用
    }

    override fun onPause(activity: Activity) {
        // 禁用
    }

    override fun onPageStart(context: Context, string: String) {
        // 禁用
    }

    override fun onPageEnd(context: Context, string: String) {
        // 禁用
    }

    override fun onEvent(context: Context, name: String, label: String, data: Map<String, String?>) {
        // 禁用
    }
}
