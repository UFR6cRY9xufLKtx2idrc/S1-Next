package me.ykrank.s1next.view.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidautodispose.AndroidRxDispose
import com.github.ykrank.androidlifecycle.event.ActivityEvent
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewAdapter
import com.github.ykrank.androidtools.util.AnimUtils
import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.RxJavaUtil
import com.github.ykrank.androidtools.widget.AppBarOffsetChangedListener
import com.github.ykrank.androidtools.widget.glide.model.ImageInfo
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.databinding.ActivityHomeBinding
import me.ykrank.s1next.view.dialog.LoginPromptDialogFragment
import me.ykrank.s1next.view.event.BlackListChangeEvent
import me.ykrank.s1next.view.internal.BlacklistMenuAction
import me.ykrank.s1next.widget.image.ImageBiz
import me.ykrank.s1next.widget.track.event.ViewHomeTrackEvent
import javax.inject.Inject

/**
 * Created by ykrank on 2017/1/8.
 */

class UserHomeActivity : BaseActivity() {

    @Inject
    internal lateinit var s1Service: S1Service

    private lateinit var binding: ActivityHomeBinding
    private var uid: String? = null
    private var name: String? = null
    private var isInBlacklist: Boolean = false
    private var blacklistMenu: MenuItem? = null
    private lateinit var adapter: SimpleRecycleViewAdapter

    private val imageBiz by lazy {
        ImageBiz(mDownloadPreferencesManager)
    }

    override val isTranslucent: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.appComponent.inject(this)

        uid = intent.getStringExtra(ARG_UID)
        name = intent.getStringExtra(ARG_USERNAME)
        val thumbImageInfo = intent.getParcelableExtra<ImageInfo>(ARG_IMAGE_INFO)
        trackAgent.post(ViewHomeTrackEvent(uid, name))
        leavePageMsg("UserHomeActivity##uid:$uid,name:$name")

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.downloadPreferencesManager = mDownloadPreferencesManager
        binding.thumb = thumbImageInfo?.url
        binding.lifecycleOwner = this
        val profile = Profile()
        profile.homeUid = uid
        profile.homeUsername = name
        binding.data = profile

        initTransition()
        initListener()
        setupImage()
        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_home, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        blacklistMenu = menu.findItem(R.id.menu_blacklist)
        refreshBlacklistMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.menu_blacklist -> {
                if (isInBlacklist) {
                    BlacklistMenuAction.removeBlacklist(this, mEventBus, uid?.toInt() ?: 0, name)
                } else {
                    BlacklistMenuAction.addBlacklist(this, uid?.toInt() ?: 0, name)
                }
                return true
            }

            R.id.menu_refresh_avatar -> {

                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        mEventBus.get()
            .ofType(BlackListChangeEvent::class.java)
            .to(AndroidRxDispose.withObservable(this, ActivityEvent.PAUSE))
            .subscribe { blackListEvent ->
                val dbWrapper = BlackListBiz.getInstance()
                lifecycleScope.launch(Dispatchers.IO) {
                    if (blackListEvent.isAdd) {
                        dbWrapper.saveDefaultBlackList(
                            blackListEvent.authorPostId, blackListEvent.authorPostName,
                            blackListEvent.remark
                        )
                    } else {
                        dbWrapper.delDefaultBlackList(
                            blackListEvent.authorPostId,
                            blackListEvent.authorPostName
                        )
                    }
                }
            }
    }

    private fun afterBlackListChange(isAdd: Boolean) {
        showShortToast(if (isAdd) R.string.blacklist_add_success else R.string.blacklist_remove_success)
        refreshBlacklistMenu()
    }

    private fun initTransition() {
        if (!intent.getBooleanExtra(ARG_TRANSITION, false)) {
            window.setSharedElementReturnTransition(null)
            window.setSharedElementReenterTransition(null)
            binding.avatar.transitionName = null

            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                com.github.ykrank.androidtools.R.anim.slide_in_right_quick,
                0
            )
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                0,
                com.github.ykrank.androidtools.R.anim.slide_out_right_quick
            )
        }
    }

    private fun initListener() {

        binding.appBar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(
                appBarLayout: AppBarLayout,
                oldVerticalOffset: Int,
                verticalOffset: Int
            ) {
                val maxScroll = appBarLayout.totalScrollRange
                val oldPercentage = Math.abs(oldVerticalOffset).toFloat() / maxScroll.toFloat()
                val percentage = Math.abs(verticalOffset).toFloat() / maxScroll.toFloat()
                if (oldPercentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move up
                    AnimUtils.startAlphaAnimation(
                        binding.toolbarTitle,
                        TITLE_ANIMATIONS_DURATION.toLong(),
                        View.VISIBLE
                    )
                } else if (oldPercentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move down
                    AnimUtils.startAlphaAnimation(
                        binding.toolbarTitle,
                        TITLE_ANIMATIONS_DURATION.toLong(),
                        View.INVISIBLE
                    )
                }
            }
        })

        binding.avatar.setOnClickListener { v ->
            val bigAvatarUrl = Api.getAvatarBigUrl(uid)
            GalleryActivity.start(v.context, bigAvatarUrl)
        }

        binding.ivNewPm.setOnClickListener { v ->
            binding.data?.let {
                NewPmActivity.startNewPmActivityForResultMessage(
                    this,
                    it.homeUid, it.homeUsername
                )
            }
        }

        binding.tvFriends.setOnClickListener { v -> FriendListActivity.start(this, uid, name) }

        binding.tvThreads.setOnClickListener { v -> UserThreadActivity.start(this, uid, name) }

        binding.tvReplies.setOnClickListener { v -> UserReplyActivity.start(this, uid, name) }

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerView.isNestedScrollingEnabled = false
        adapter = SimpleRecycleViewAdapter(this, R.layout.item_home_stat, false)
        binding.recyclerView.adapter = adapter
    }

    private fun setupImage() {

    }

    private fun loadData() {
        binding.data?.let { profile ->

            s1Service.getProfileWeb(
                "${Api.BASE_URL}space-uid-${profile.homeUid}.html",
                profile.homeUid
            )
                .map { Profile.fromHtml(it) }
                .compose(RxJavaUtil.iOSingleTransformer())
                .to(AndroidRxDispose.withSingle(this, ActivityEvent.DESTROY))
                .subscribe({
                    binding.data = it
                    adapter.swapDataSet(it.stats)
                }, L::e)
        }
    }

    @MainThread
    private fun refreshBlacklistMenu() {
        if (blacklistMenu == null) {
            return
        }
        val wrapper = BlackListBiz.getInstance()

        lifecycleScope.launch(L.report) {
            val blackList = withContext(Dispatchers.IO) {
                wrapper.getMergedBlackList(uid?.toInt() ?: 0, name)
            }
            if (blackList != null) {
                isInBlacklist = true
                blacklistMenu?.setTitle(R.string.menu_blacklist_remove)
            } else {
                isInBlacklist = false
                blacklistMenu?.setTitle(R.string.menu_blacklist_add)
            }
        }
    }

    companion object {

        private const val PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.71f
        private const val TITLE_ANIMATIONS_DURATION = 300

        private const val ARG_UID = "uid"
        private const val ARG_USERNAME = "username"
        private const val ARG_IMAGE_INFO = "image_info"
        private const val ARG_TRANSITION = "transition"

        fun start(
            activity: FragmentActivity,
            uid: String,
            userName: String?
        ) {
            if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(
                    activity.supportFragmentManager,
                    App.appComponent.user
                )
            ) {
                return
            }

            val intent = Intent(activity, UserHomeActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            activity.startActivity(intent)
        }

        fun start(
            activity: FragmentActivity,
            uid: String,
            userName: String?,
            avatarView: View
        ) {
            if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(
                    activity.supportFragmentManager,
                    App.appComponent.user
                )
            ) {
                return
            }

            val baseContext = ContextUtils.getBaseContext(activity)
            if (baseContext !is Activity) {
                L.leaveMsg("uid:$uid")
                L.leaveMsg("userName:$userName")
                L.report(IllegalStateException("UserHomeActivity start error: context not instance of activity"))
                return
            }
            val imageInfo =
                avatarView.getTag(com.github.ykrank.androidtools.R.id.tag_drawable_info) as ImageInfo?
            val intent = Intent(baseContext, UserHomeActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            intent.putExtra(ARG_TRANSITION, true)
            if (imageInfo != null) {
                intent.putExtra(ARG_IMAGE_INFO, imageInfo)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                baseContext, avatarView, baseContext.getString(R.string.transition_avatar)
            )
            ActivityCompat.startActivity(baseContext, intent, options.toBundle())
        }
    }
}
