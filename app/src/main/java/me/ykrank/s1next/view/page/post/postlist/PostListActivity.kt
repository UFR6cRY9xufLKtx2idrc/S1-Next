package me.ykrank.s1next.view.page.post.postlist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Browser
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.util.AudioManagerUtils
import com.github.ykrank.androidtools.util.OnceClickUtil
import com.github.ykrank.androidtools.widget.net.WifiBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.link.ThreadLink
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.dbmodel.ReadProgress
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.view.activity.BaseActivity
import me.ykrank.s1next.view.activity.ForumActivity
import javax.inject.Inject

/**
 * An Activity which includes [android.support.v4.view.ViewPager]
 * to represent each page of post lists.
 */
class PostListActivity : BaseActivity(), WifiBroadcastReceiver.NeedMonitorWifi {

    @Inject
    lateinit var mReadPreferences: ReadPreferencesManager


    var fragment: PostListFragment? = null

    private var threadId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.appComponent.inject(this)
        setContentView(R.layout.activity_base_long_title)

        disableDrawerIndicator()

        if (savedInstanceState == null) {
            val intent = intent
            val thread = intent.getParcelableExtra<Thread>(ARG_THREAD)
            val progress = intent.getParcelableExtra<ReadProgress>(ARG_READ_PROGRESS)
            val authorId = intent.getStringExtra(ARG_AUTHOR_ID)
            threadId = thread?.id
            if (thread == null) {//通过链接打开
                val threadLink: ThreadLink = intent.getParcelableExtra(ARG_THREAD_LINK)!!
                threadId = threadLink.threadId
                fragment =
                    PostListFragment.newInstance(threadLink)
            } else if (!authorId.isNullOrEmpty()) { //指定用户
                fragment = PostListFragment.newInstance(thread, authorId)
            } else if (progress != null) {//有进度信息
                fragment = PostListFragment.newInstance(thread, progress)
            } else {//没有进度信息
                fragment = PostListFragment.newInstance(
                    thread, intent.getBooleanExtra(
                        ARG_SHOULD_GO_TO_LAST_PAGE, false
                    )
                )
            }
            supportFragmentManager.beginTransaction().add(
                R.id.frame_layout, fragment!!,
                PostListFragment.TAG
            ).commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (mReadPreferences.volumeKeyPaging && !AudioManagerUtils.isMusicOrVideoPlay(this)) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    fragment?.moveToNext(-1)
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    fragment?.moveToNext(1)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (intent.getBooleanExtra(ARG_COME_FROM_OTHER_APP, false)) {
                    ForumActivity.start(this)
                    return true
                }
                return super.onOptionsItemSelected(item)
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val ARG_THREAD = "thread"
        private const val ARG_SHOULD_GO_TO_LAST_PAGE = "should_go_to_last_page"
        private const val ARG_AUTHOR_ID = "author_id"

        private const val ARG_THREAD_LINK = "thread_link"
        private const val ARG_COME_FROM_OTHER_APP = "come_from_other_app"

        private const val ARG_READ_PROGRESS = "read_progress"

        fun start(context: Context, thread: Thread, shouldGoToLastPage: Boolean) {
            val intent = Intent(context, PostListActivity::class.java)
            intent.putExtra(ARG_THREAD, thread)
            intent.putExtra(ARG_SHOULD_GO_TO_LAST_PAGE, shouldGoToLastPage)

            context.startActivity(intent)
        }

        fun start(context: Context, thread: Thread, authorId: String) {
            val intent = Intent(context, PostListActivity::class.java)
            intent.putExtra(ARG_THREAD, thread)
            intent.putExtra(ARG_AUTHOR_ID, authorId)

            context.startActivity(intent)
        }

        fun start(activity: Activity, threadLink: ThreadLink) {
            // see android.text.style.URLSpan#onClick(View)
            val appId = activity.intent.getStringExtra(Browser.EXTRA_APPLICATION_ID)

            start(activity, threadLink, appId != null && activity.packageName != appId)
        }

        fun start(context: Context, threadLink: ThreadLink, comeFromOtherApp: Boolean) {
            val intent = Intent(context, PostListActivity::class.java)
            intent.putExtra(ARG_THREAD_LINK, threadLink)
            intent.putExtra(ARG_COME_FROM_OTHER_APP, comeFromOtherApp)

            context.startActivity(intent)
        }

        fun start(context: Context, readProgress: ReadProgress) {
            val intent = Intent(context, PostListActivity::class.java)
            val thread = Thread()
            thread.id = readProgress.threadId.toString()
            intent.putExtra(ARG_THREAD, thread)
            intent.putExtra(ARG_READ_PROGRESS, readProgress)

            context.startActivity(intent)
        }

        /**
         * 为View绑定“点击打开有读取进度的帖子”的事件
         *
         * @param view   点击焦点
         * @param thread 帖子信息 的 提供者
         * @return
         */
        fun bindClickStartForView(
            view: View,
            lifecycleOwner: LifecycleOwner,
            threadProvider: () -> Thread?
        ) {
            OnceClickUtil.setClickLister(view, {
                val preferencesManager = App.preAppComponent.readProgressPreferencesManager
                if (preferencesManager.isLoadAuto) {
                    lifecycleOwner.lifecycleScope.launch {
                        val thread = threadProvider()
                        val readProgress = withContext(Dispatchers.IO) {
                            ReadProgressBiz.instance.getWithThreadId(
                                thread?.id?.toInt() ?: 0
                            )
                        }
                        val context = view.context
                        val intent = Intent(context, PostListActivity::class.java)
                        intent.putExtra(ARG_THREAD, thread)
                        intent.putExtra(ARG_READ_PROGRESS, readProgress)
                        context.startActivity(intent)
                    }
                } else {
                    threadProvider()?.apply {
                        start(it.context, this, false)
                    }
                }
            }, 1000)
        }
    }

}
