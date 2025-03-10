package me.ykrank.s1next.widget

import android.content.Context
import android.util.SparseArray
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Emoticon
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A factory provides emotions.
 */
class EmoticonFactory(context: Context) {
    private val init = AtomicBoolean()

    val emotionTypeTitles: List<String> =
        context.resources.getStringArray(R.array.emoticon_type).toList()

    private val mEmoticons: SparseArray<List<Emoticon>> = SparseArray()

    private val animalEmoticonList: List<Emoticon> by lazy {
        createEmotionList(31, "animal2017", "a")
    }

    private val bundamEmoticonList: List<Emoticon> by lazy {
        createEmotionList(37, "bundam2017", "b")
    }

    private val cartonEmoticonList: List<Emoticon> by lazy {
        createEmotionList(490, "carton2017", "c")
    }

    private val deviceEmoticonList: List<Emoticon> by lazy {
        createEmotionList(45, "device2017", "d")
    }

    private val faceEmoticonList: List<Emoticon> by lazy {
        createEmotionList(277, "face2017", "f")
    }

    private val gooseEmoticonList: List<Emoticon> by lazy {
        createEmotionList(74, "goose2017", "g")
    }

    fun getEmoticonsByIndex(index: Int): List<Emoticon> {
        var emoticons: List<Emoticon>? = mEmoticons.get(index)
        if (emoticons == null) {
            emoticons = when (index) {
                0 -> faceEmoticonList
                1 -> cartonEmoticonList
                2 -> animalEmoticonList
                3 -> deviceEmoticonList
                4 -> gooseEmoticonList
                5 -> bundamEmoticonList
                else -> throw IllegalStateException("Unknown emoticon index: $index.")
            }
        }
        mEmoticons.put(index, emoticons)

        return emoticons
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun preload() {
        if (!init.compareAndSet(false, true)) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            for (i in emotionTypeTitles.indices) {
                getEmoticonsByIndex(i)
            }
        }
    }

    companion object {
        private val FORMAT_LEAD_ZERO_3 = DecimalFormat("000")
        const val ASSET_PATH_EMOTICON = "file:///android_asset/image/emoticon/"

        private fun emoticon(emoticonFileName: String, emoticonEntity: String): Emoticon {
            return Emoticon(ASSET_PATH_EMOTICON + emoticonFileName, emoticonEntity)
        }

        private fun createEmotionList(
            size: Int,
            fileDir: String,
            entityDir: String
        ): MutableList<Emoticon> {
            val result = mutableListOf<Emoticon>()
            for (i in 1..size) {
                val code = FORMAT_LEAD_ZERO_3.format(i)

                result.add(emoticon("${fileDir}/${code}.", "[${entityDir}:${code}]"))
            }
            return result
        }
    }
}
