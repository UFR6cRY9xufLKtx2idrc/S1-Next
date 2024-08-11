package me.ykrank.s1next.view.page.setting.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.bumptech.glide.Glide
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.FileUtil
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.RxJavaUtil
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.util.AppFileUtil
import javax.inject.Inject

/**
 * An Activity includes download settings that allow users
 * to modify download features and behaviors such as cache
 * size and avatars/images download strategy.
 */
class DownloadPreferenceFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener {

    @Inject
    internal lateinit var mDownloadPreferencesManager: DownloadPreferencesManager

    @Inject
    internal lateinit var mCacheBiz: CacheBiz

    private var disposable: Disposable? = null

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        App.appComponent.inject(this)
        addPreferencesFromResource(R.xml.preference_download)

        findPreference<Preference>(getString(R.string.pref_key_data_download_path))?.onPreferenceClickListener =
            this

        findPreference<Preference>(getString(R.string.pref_key_clear_image_cache))?.onPreferenceClickListener =
            this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshDataCacheSize()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_key_data_total_cache_size)) {
            refreshDataCacheSize()
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key ?: return false
        if (key == getString(R.string.pref_key_clear_image_cache)) {
            lifecycleScope.launch(L.report) {
                Glide.get(App.get()).clearMemory()
                withContext(Dispatchers.IO) {
                    Glide.get(App.get()).clearDiskCache()
                }
                activity?.toast(R.string.clear_image_cache_success)
            }

            return true
        }
        if (key == getString(R.string.pref_key_data_download_path)) {
            AppFileUtil.getDownloadPath(parentFragmentManager, null, true)
            return true
        }
        return false
    }

    override fun onDestroy() {
        RxJavaUtil.disposeIfNotNull(disposable)
        super.onDestroy()
    }

    private fun refreshDataCacheSize() {
        val prefDataCacheSize =
            findPreference<Preference>(getString(R.string.pref_key_data_total_cache_size))
        if (prefDataCacheSize != null) {
            lifecycleScope.launch(L.report) {
                val maxSize = mDownloadPreferencesManager.totalDataCacheSize
                val message = withContext(Dispatchers.IO) {
                    "${mCacheBiz.count}/$maxSize ${
                        FileUtil.getPrintSize(
                            mCacheBiz.size
                        )
                    }"
                }
                prefDataCacheSize.summary = message
            }
        }
    }

    companion object {
        val TAG: String = DownloadPreferenceFragment::class.java.simpleName
    }
}
