package me.ykrank.s1next.view.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.databinding.FragmentBaseBinding
import me.ykrank.s1next.view.activity.HistoryActivity
import me.ykrank.s1next.view.adapter.HistoryCursorRecyclerViewAdapter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Fragment show post view history list
 */
@AndroidEntryPoint
class HistoryListFragment : BaseFragment() {
    private var mRecyclerAdapter: HistoryCursorRecyclerViewAdapter? = null

    @Inject
    internal lateinit var historyBiz: HistoryBiz

    @Inject
    internal lateinit var cacheBiz: CacheBiz

    @Inject
    internal lateinit var readPreferencesManager: ReadPreferencesManager

    @Inject
    internal lateinit var readProgressBiz: ReadProgressBiz

    private lateinit var binding: FragmentBaseBinding
    private var mode: String = HistoryActivity.MODE_HISTORY
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getString(ARG_MODE, HistoryActivity.MODE_HISTORY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leavePageMsg("HistoryListFragment")
        val activity: Activity = requireActivity()
        binding.recyclerView.setLayoutManager(LinearLayoutManager(activity))
        mRecyclerAdapter = HistoryCursorRecyclerViewAdapter(
            activity,
            viewLifecycleOwner,
            readPreferencesManager,
            readProgressBiz,
            historyBiz
        )
        binding.recyclerView.setAdapter(mRecyclerAdapter)
    }

    override fun onPause() {
        mRecyclerAdapter?.changeCursor(null)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val currentQuery = query.trim()
        lifecycleScope.launch(L.report) {
            try {
                val cursor = withContext(Dispatchers.IO) {
                    if (mode == HistoryActivity.MODE_POST_BACKUP) {
                        cacheBiz.getPostBackupThreadsCursor(currentQuery)
                    } else {
                        historyBiz.getHistoryListCursor(currentQuery)
                    }
                }
                mRecyclerAdapter?.changeCursor(cursor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                L.report(e)
                showSnackbar(getString(R.string.message_load_error))
            }
        }
    }

    fun updateQuery(newQuery: String) {
        val normalizedQuery = newQuery.trim()
        if (query == normalizedQuery) {
            return
        }
        query = normalizedQuery
        load()
    }

    companion object {
        val TAG = HistoryListFragment::class.java.getName()
        private const val ARG_MODE = "mode"

        @JvmStatic
        fun newInstance(mode: String = HistoryActivity.MODE_HISTORY): HistoryListFragment {
            return HistoryListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                }
            }
        }
    }
}
