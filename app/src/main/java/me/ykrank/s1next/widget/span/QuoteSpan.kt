package me.ykrank.s1next.widget.span

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import com.github.ykrank.androidtools.util.ResourceUtil
import me.ykrank.s1next.R

class QuoteSpan(private val stripeColor: Int, private val stripeWidth: Float, private val gap: Float) : LeadingMarginSpan {

    override fun drawLeadingMargin(c: Canvas?, p: Paint?, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence?, start: Int, end: Int, first: Boolean, layout: Layout?) {
        p ?: return
        c ?: return
        val style = p.style
        val paintColor = p.color
        p.style = Paint.Style.FILL
        p.color = stripeColor
        c.drawRect(x.toFloat(), top.toFloat(), x + dir * stripeWidth, bottom.toFloat(), p)
        p.style = style
        p.color = paintColor
        return
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return (stripeWidth + gap).toInt()
    }
}

fun Spanned.replaceQuoteSpans(context: Context): Spanned {
    val quoteSpans = getSpans(0, length, QuoteSpan::class.java)
    // 没有引用块时直接返回自身，避免每次 bind 都全量复制 Spannable
    if (quoteSpans.isEmpty()) return this

    val spannableString = SpannableString(this)
    // color 只取一次，避免循环里反复走 ResourceUtil
    val color = ResourceUtil.getAttrColorInt(context, androidx.appcompat.R.attr.colorPrimaryDark)
    for (quoteSpan in quoteSpans) {
        val start = getSpanStart(quoteSpan)
        val end = getSpanEnd(quoteSpan)
        val flags = getSpanFlags(quoteSpan)
        spannableString.removeSpan(quoteSpan)
        spannableString.setSpan(QuoteSpan(color, 10f, 15f), start, end, flags)
    }
    return spannableString
}
