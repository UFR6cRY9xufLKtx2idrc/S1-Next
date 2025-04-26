package me.ykrank.s1next.data.api

import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import okhttp3.HttpUrl

object Api {
    val baseApi = BaseApi.DEFAULT
    val BASE_URL = baseApi.baseUrl
    private val BASE_AVATAR_URL = baseApi.avatarUrl
    private val BASE_STATIC_URL = baseApi.staticUrl
    private val BASE_STATIC_URL_HTTP = baseApi.staticUrlHttp
    val HOST_LIST = BaseApi.HOST_LIST
    var SUPPORT_HTTPS = true
    private val RANDOM_IMAGE_URL = BaseApi.RANDOM_IMAGE_URL
    const val BASE_API_PREFIX = "api/mobile/index.php?module="
    const val API_VERSION_DEFAULT = "1"
    const val THREADS_PER_PAGE = 50
    const val POSTS_PER_PAGE = 40

    /**
     * emoticon init rect size
     */
    const val EMOTICON_INIT_DP = 32
    const val REPLY_NOTIFICATION_MAX_LENGTH = 100
    private const val URL_EMOTICON_IMAGE_PREFIX = "static/image/smiley/"
    private const val URL_EMOTICON_IMAGE_PREFIX_STATIC = "image/smiley/"

    /**
     * Opens the browser via [android.content.Intent].
     */
    val URL_BROWSER_REGISTER = prepend("member.php?mod=register")
    private val URL_USER_AVATAR_BIG = "${BASE_AVATAR_URL}%s_avatar_big.jpg"
    private val URL_BROWSER_FAVOURITES = prepend("home.php?mod=space&do=favorite")
    private val URL_BROWSER_THREAD_LIST = prepend("forum-%s-%d.html")
    private val URL_BROWSER_POST_LIST = prepend("thread-%s-%d-1.html")
    val URL_VIEW_VOTE = prepend("forum.php?mod=misc&action=viewvote")
    const val URL_DARK_ROOM = "forum.php?mod=misc&action=showdarkroom&ajaxdata=json"
    const val URL_WEB_BLACK_LIST = "home.php?mod=space&do=friend&view=blacklist"
    val URL_VIEW_NOTE = prepend("home.php?mod=space&do=notice&view=system")
    private fun prepend(suffix: String): String {
        return BASE_URL + suffix
    }

    private fun parseUserId(userId: String?): List<String>? {
        val userIdNum = userId?.toLongOrNull() ?: return null
        val userIdString = String.format("%09d", userIdNum)
        return listOf(
            userIdString.substring(0, 3),
            userIdString.substring(3, 5),
            userIdString.substring(5, 7),
            userIdString.substring(7, 9),
        )
    }

    private fun getUserAvatarPath(userId: String?): String? {
        return parseUserId(userId)?.reduce { acc, s -> "$acc/$s" }
    }

    fun getAvatarUrls(userId: String?): List<String> {
        return listOfNotNull(getAvatarBigUrl(userId))
    }

    fun getAvatarBigUrl(userId: String?): String? {
        return getUserAvatarPath(userId)?.let {
            return@let String.format(URL_USER_AVATAR_BIG, it)
        }
    }

    @JvmStatic
    fun isAvatarUrl(url: String?): Boolean {
        return url != null && url.startsWith(BASE_AVATAR_URL)
    }

    fun getFavouritesListUrlForBrowser(pageNum: Int): String {
        return Uri.parse(URL_BROWSER_FAVOURITES).buildUpon()
            .appendQueryParameter("page", pageNum.toString())
            .toString()
    }

    fun getThreadListUrlForBrowser(forumId: String?, pageNum: Int): String {
        return String.format(URL_BROWSER_THREAD_LIST, forumId, pageNum)
    }

    fun getPostListUrlForBrowser(threadId: String?, pageNum: Int): String {
        return String.format(URL_BROWSER_POST_LIST, threadId, pageNum)
    }

    fun randomImage(): String {
        return RANDOM_IMAGE_URL + "?" + System.currentTimeMillis()
    }

    /**
     * get emoticon from url
     *
     * @return emoticon name if exist. null if not
     */
    fun parseEmoticonName(url: String?): String? {
        if (TextUtils.isEmpty(url)) {
            return null
        }
        // url has no domain if it comes from BASE_URL server.
        if (!URLUtil.isNetworkUrl(url)) {
            if (url!!.startsWith(URL_EMOTICON_IMAGE_PREFIX)) {
                return url.substring(URL_EMOTICON_IMAGE_PREFIX.length)
            }
        } else if (url!!.startsWith(BASE_URL + URL_EMOTICON_IMAGE_PREFIX)) {
            return url.substring((BASE_URL + URL_EMOTICON_IMAGE_PREFIX).length)
        } else if (url.startsWith(BASE_STATIC_URL + URL_EMOTICON_IMAGE_PREFIX_STATIC)) {
            return url.substring((BASE_STATIC_URL + URL_EMOTICON_IMAGE_PREFIX_STATIC).length)
        } else if (url.startsWith(BASE_STATIC_URL_HTTP + URL_EMOTICON_IMAGE_PREFIX_STATIC)) {
            return url.substring((BASE_STATIC_URL_HTTP + URL_EMOTICON_IMAGE_PREFIX_STATIC).length)
        }
        return null
    }

    fun isEmoticonName(url: String): Boolean {
        return if (URLUtil.isNetworkUrl(url)) {
            url.startsWith(BASE_URL + URL_EMOTICON_IMAGE_PREFIX) ||
                    url.startsWith(BASE_STATIC_URL + URL_EMOTICON_IMAGE_PREFIX_STATIC) ||
                    url.startsWith(BASE_STATIC_URL_HTTP + URL_EMOTICON_IMAGE_PREFIX_STATIC)
        } else {
            url.startsWith(URL_EMOTICON_IMAGE_PREFIX)
        }
    }

    fun parseBaseUrl(httpUrl: HttpUrl): String {
        val pathSegments = httpUrl.pathSegments
        //if like [host]/2b/
        return if (pathSegments.isNotEmpty() && "2b" == pathSegments[0]) {
            httpUrl.scheme + "://" + httpUrl.host + "/2b/"
        } else {
            httpUrl.scheme + "://" + httpUrl.host + "/"
        }
    }
}
