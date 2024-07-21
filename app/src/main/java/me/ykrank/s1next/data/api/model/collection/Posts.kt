package me.ykrank.s1next.data.api.model.collection

import androidx.annotation.WorkerThread
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.ykrank.androidtools.util.StringUtils
import me.ykrank.s1next.data.api.model.Account
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.biz.BlackWordBiz
import me.ykrank.s1next.data.db.dbmodel.BlackList
import me.ykrank.s1next.data.db.dbmodel.BlackWord
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@JsonIgnoreProperties(ignoreUnknown = true)
class Posts @JsonCreator constructor(
    @JsonProperty("special_trade") trade: Map<Int, Any>?,
    @JsonProperty("postlist") rawPostList: List<Post>?
) : Account() {

    @JsonProperty("thread")
    var postListInfo: Thread? = null
        set(p) {
            this.postList.forEach {
                if (p?.author == it.authorName) {
                    it.isOpPost = true
                }
            }
            field = p
        }

    @JsonProperty("threadsortshow")
    var threadAttachment: ThreadAttachment? = null

    @JsonProperty("_postList")
    val postList: List<Post> = filterPostList(rawPostList)

    @JsonProperty("special_poll")
    val vote: Vote? = null

    init {
        if (trade != null && !rawPostList.isNullOrEmpty()) {
            val post = rawPostList[0]
            if (trade.containsKey(post.id + 1)) {
                post.isTrade = true
            }
        }
    }

    fun initCommentCount(commentCountMap: Map<Int, Int?>) {
        this.postList.forEach {
            if (commentCountMap.containsKey(it.id)) {
                it.rates = listOf()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Posts

        if (postListInfo != other.postListInfo) return false
        if (threadAttachment != other.threadAttachment) return false
        if (postList != other.postList) return false
        if (vote != other.vote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (postListInfo?.hashCode() ?: 0)
        result = 31 * result + (threadAttachment?.hashCode() ?: 0)
        result = 31 * result + postList.hashCode()
        result = 31 * result + (vote?.hashCode() ?: 0)
        return result
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    class ThreadAttachment {

        @JsonProperty("threadsortname")
        var title: String? = null

        @JsonProperty("optionlist")
        var infoList: ArrayList<Info>? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ThreadAttachment

            if (title != other.title) return false
            if (infoList != other.infoList) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title?.hashCode() ?: 0
            result = 31 * result + (infoList?.hashCode() ?: 0)
            return result
        }

        @PaperParcel
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Info : PaperParcelable {

            @JsonIgnore
            @get:JsonGetter
            var label: String? = null

            @JsonIgnore
            @get:JsonGetter
            var value: String? = null

            constructor()

            @JsonCreator
            constructor(@JsonProperty("title") label: String?,
                        @JsonProperty("value") value: String?,
                        @JsonProperty("unit") unit: String?) {
                this.label = label
                this.value = StringUtils.unescapeNonBreakingSpace(value) + (unit ?: "")
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Info

                if (label != other.label) return false
                if (value != other.value) return false

                return true
            }

            override fun hashCode(): Int {
                var result = label?.hashCode() ?: 0
                result = 31 * result + (value?.hashCode() ?: 0)
                return result
            }

            companion object {
                @JvmField
                val CREATOR = PaperParcelPosts_ThreadAttachment_Info.CREATOR
            }
        }
    }

    companion object {

        /**
         * @see .filterPost
         */
        @WorkerThread
        fun filterPostList(oPosts: List<Post>?): List<Post> {
            val posts = ArrayList<Post>()
            if (oPosts != null) {
                val blackWords = BlackWordBiz.instance.getAllNotNormalBlackWord()
                for (post in oPosts) {
                    val fPost = filterPost(post, false, blackWords)
                    if (fPost != null) {
                        posts.add(fPost)
                    }
                }
            }
            return posts
        }

        /**
         * 对数据源进行处理
         *
         *  * 标记黑名单用户
         *  * 回复引用新版替换回老版样式
         *  * 过滤屏蔽词（已屏蔽的对象不会在修改屏蔽词后自动更新，而必须是重新的原始对象）
         *
         * 如果修改了过滤状态，则会返回不同的对象
         */
        @WorkerThread
        fun filterPost(post: Post, clone: Boolean = false, blackWords: List<BlackWord>? = null): Post? {
            var newPost: Post = post
            val blackListWrapper = BlackListBiz.getInstance()
            val blackList = blackListWrapper.getMergedBlackList(post.authorId?.toIntOrNull()
                    ?: -1, post.authorName, enableCache = true)
            if (blackList == null || blackList.post == BlackList.NORMAL) {
                // 不在黑名单中
                if (post.hide == Post.HIDE_USER) {
                    if (clone) {
                        newPost = post.clone()
                    }
                    newPost.hide = Post.HIDE_NO
                }
            } else if (blackList.post == BlackList.DEL_POST) {
                return null
            } else if (blackList.post == BlackList.HIDE_POST) {
                if (post.hide != Post.HIDE_USER) {
                    if (clone) {
                        newPost = post.clone()
                    }
                    newPost.hide = Post.HIDE_USER
                }
                newPost.remark = blackList.remark
            }

            val reply = newPost.reply
            if (reply != null && newPost.hide == Post.HIDE_NO) {
                val mBlackWords = blackWords
                        ?: BlackWordBiz.instance.getAllNotNormalBlackWord()
                mBlackWords.forEach {
                    val word = it.word
                    if (!word.isNullOrEmpty() && it.stat != BlackWord.NORMAL) {
                        if (reply.contains(word, false)) {
                            if (it.stat == BlackWord.DEL) {
                                return null
                            } else if (it.stat == BlackWord.HIDE) {
                                //Only clone if not cloned before
                                if (clone && newPost === post) {
                                    newPost = post.clone()
                                }
                                newPost.hide = Post.HIDE_WORD
                                return@forEach
                            }
                        }
                    }
                }
            }

            return newPost
        }
    }


}
