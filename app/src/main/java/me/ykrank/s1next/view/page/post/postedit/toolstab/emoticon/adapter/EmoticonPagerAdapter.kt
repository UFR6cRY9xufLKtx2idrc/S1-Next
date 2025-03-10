package me.ykrank.s1next.view.page.post.postedit.toolstab.emoticon.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.github.ykrank.androidtools.R
import com.github.ykrank.androidtools.widget.GridAutofitLayoutManager
import me.ykrank.s1next.view.page.post.postedit.toolstab.emoticon.adapter.EmoticonGridRecyclerAdapter.BindingViewHolder
import me.ykrank.s1next.widget.EmoticonFactory

class EmoticonPagerAdapter(
    private val mActivity: Activity,
    private val mEmoticonFactory: EmoticonFactory
) : PagerAdapter() {
    private val mEmoticonWidth: Float
    private val mEmoticonGridPadding: Int

    private val mEmoticonTypeTitles: List<String>

    init {
        val resources = mActivity.resources
        mEmoticonWidth = resources.getDimension(R.dimen.minimum_touch_target_size)
        mEmoticonGridPadding = resources.getDimensionPixelSize(R.dimen.emoticon_padding)
        mEmoticonTypeTitles = mEmoticonFactory.emotionTypeTitles
    }

    override fun getCount(): Int {
        return mEmoticonTypeTitles.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return mEmoticonTypeTitles[position]
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val recyclerView = RecyclerView(mActivity)
        recyclerView.setHasFixedSize(true)
        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, mEmoticonGridPadding, 0, mEmoticonGridPadding)
        val gridLayoutManager = GridAutofitLayoutManager(mActivity, mEmoticonWidth.toInt())
        gridLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.layoutManager = gridLayoutManager
        val recyclerAdapter: RecyclerView.Adapter<BindingViewHolder> =
            EmoticonGridRecyclerAdapter(
                mActivity,
                mEmoticonFactory.getEmoticonsByIndex(position)
            )
        recyclerView.adapter = recyclerAdapter

        container.addView(recyclerView)

        return recyclerView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}
