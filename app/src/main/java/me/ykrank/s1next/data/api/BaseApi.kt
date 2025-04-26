package me.ykrank.s1next.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Created by ykrank on 4/16/25
 */
class BaseApi(val baseUrl: String) {
    private val httpUrl = baseUrl.toHttpUrl()
    val host = httpUrl.host
    private val mainDomain = host.split(".").takeLast(2).joinToString(".")
    val avatarUrl = "https://avatar.$mainDomain/"
    val staticUrl = "https://static.$mainDomain/"
    val staticUrlHttp = "http://static.$mainDomain/"
    val appApiUrl = httpUrl.newBuilder().addPathSegments("api/app/").build()

    companion object {
        val DEFAULT = BaseApi("https://stage1st.com/2b/")
        val HOST_LIST = arrayOf(
            "bbs.saraba1st.com", "www.saraba1st.com", "stage1st.com", "www.stage1st.com"
        )
        const val RANDOM_IMAGE_URL = "https://ac.stage3rd.com/S1_ACG_randpic.asp"
    }
}