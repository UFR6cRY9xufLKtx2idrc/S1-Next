package me.ykrank.s1next.binding

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.glide.transformations.BlurTransformation
import com.github.ykrank.androidtools.widget.glide.viewtarget.DrawableViewBackgroundTarget
import com.github.ykrank.androidtools.widget.glide.viewtarget.ViewBackgroundTarget
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api.getAvatarBigUrl
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.widget.image.ImageBiz
import me.ykrank.s1next.widget.image.image

/**
 * Created by AdminYkrank on 2016/4/17.
 */
object ViewBindingAdapter {
    @JvmStatic
    @BindingAdapter("marginEnd")
    fun setMarginEnd(view: View, margin: Float) {
        val layoutParams = view.layoutParams as MarginLayoutParams
        layoutParams.setMarginEnd(margin.toInt())
        view.setLayoutParams(layoutParams)
    }

    /**
     * action when view init
     *
     * @param view       view
     * @param onViewBind action when view init
     */
    @JvmStatic
    @BindingAdapter("bindEvent")
    fun setOnViewBind(view: View, onViewBind: Function1<View, Any>) {
        try {
            onViewBind(view)
        } catch (e: Exception) {
            L.report(e)
        }
    }

    @JvmStatic
    @BindingAdapter("downloadPreferencesManager", "blurUid")
    fun setUserBlurBackground(
        view: View, oldManager: DownloadPreferencesManager?, oldBlurUid: String?,
        newManager: DownloadPreferencesManager, newBlurUid: String?
    ) {
        if (TextUtils.isEmpty(newBlurUid)) {
            setBlurBackground(view, oldManager, null, newManager, null)
            return
        }
        if (!TextUtils.equals(oldBlurUid, newBlurUid)) {
            val oldAvatarUrl: String? = getAvatarBigUrl(oldBlurUid)
            val newAvatarUrl: String? = getAvatarBigUrl(newBlurUid)

            setBlurBackground(view, oldManager, oldAvatarUrl, newManager, newAvatarUrl)
        }
    }

    @BindingAdapter("downloadPreferencesManager", "blurUrl")
    fun setBlurBackground(
        view: View, oldManager: DownloadPreferencesManager?, oldBlurUrl: String?,
        newManager: DownloadPreferencesManager, newBlurUrl: String?
    ) {
        val context = view.context
        val blurTransformation = BlurTransformation(context, 20)
        blurTransformation.targetSize = 50
        if (TextUtils.isEmpty(newBlurUrl)) {
            Glide.with(view)
                .load(R.drawable.ic_avatar_placeholder)
                .apply(
                    RequestOptions()
                        .centerCrop()
                        .transform(blurTransformation)
                )
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(DrawableViewBackgroundTarget(view))
            return
        }
        if (!TextUtils.equals(oldBlurUrl, newBlurUrl)) {
            Glide.with(view)
                .asBitmap()
                .image(ImageBiz(newManager), newBlurUrl)
                .centerCrop()
                .transform(blurTransformation)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        view.post {
                            setBlurBackground(
                                view,
                                oldManager,
                                null,
                                newManager,
                                null
                            )
                        }
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(object : ViewBackgroundTarget<Bitmap?>(view) {
                    override fun setResource(resource: Bitmap?) {
                        setDrawable(BitmapDrawable(getView().resources, resource))
                    }
                })
        }
    }
}
