package me.ykrank.s1next.view.page.post.render

import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Post
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

class PostRenderMapper {

    fun map(posts: List<Post>): PostRenderResult {
        val items = mutableListOf<Any>()
        val headers = mutableMapOf<Int, Int>()
        val footers = mutableMapOf<Int, Int>()
        val ranges = mutableMapOf<Int, IntRange>()

        posts.forEach { post ->
            val start = items.size
            if (post.hide != Post.HIDE_NO) {
                items += post
                headers[post.id] = start
                ranges[post.id] = start..start
                return@forEach
            }

            headers[post.id] = items.size
            items += PostRenderItem.Header(post)

            val html = if (post.isTrade) post.extraHtml else post.reply
            items.addAll(parseContent(post, html.orEmpty()))

            footers[post.id] = items.size
            items += PostRenderItem.Footer(post)
            ranges[post.id] = start until items.size
        }

        return PostRenderResult(
            items,
            PostRenderIndex(headers, footers, ranges)
        )
    }

    private fun parseContent(post: Post, html: String): List<PostRenderItem> {
        if (html.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val body = Jsoup.parseBodyFragment(html).body()
            val blocks = mutableListOf<PostRenderItem>()
            val buffer = StringBuilder()
            var blockIndex = 0

            fun flushText() {
                val part = buffer.toString()
                buffer.clear()
                if (isBlankTextBlock(part)) {
                    return
                }
                val item = if (isComplexHtml(part)) {
                    PostRenderItem.FallbackHtmlBlock(post, part, blockIndex++)
                } else {
                    PostRenderItem.TextBlock(post, part, blockIndex++)
                }
                blocks += item
            }

            body.childNodes().forEach { node ->
                val images = standaloneImages(node)
                if (images.isNullOrEmpty()) {
                    buffer.append(node.outerHtml())
                } else {
                    flushText()
                    images.forEach { image ->
                        val url = normalizeImageUrl(image.attr("src"))
                        if (url.isNullOrBlank()) {
                            buffer.append(image.outerHtml())
                        } else {
                            blocks += PostRenderItem.ImageBlock(
                                post,
                                url,
                                image.attr("width").toPositiveIntOrNull(),
                                image.attr("height").toPositiveIntOrNull(),
                                blockIndex++,
                                blocks.lastOrNull() is PostRenderItem.ImageBlock,
                            )
                        }
                    }
                }
            }
            flushText()
            if (blocks.isEmpty()) {
                listOf(PostRenderItem.FallbackHtmlBlock(post, html, 0))
            } else {
                blocks
            }
        }.getOrElse {
            listOf(PostRenderItem.FallbackHtmlBlock(post, html, 0))
        }
    }

    private fun standaloneImages(node: Node): List<Element>? {
        val element = node as? Element ?: return null
        if (element.tagName().equals("img", ignoreCase = true)) {
            return if (element.isStandalonePostImage()) listOf(element) else null
        }
        if (element.hasComplexContainer()) {
            return null
        }
        val images = element.select("img")
        if (images.isEmpty()) {
            return null
        }
        if (images.any { !it.isStandalonePostImage() }) {
            return null
        }
        val clone = element.clone()
        clone.select("img").remove()
        clone.select("br").remove()
        val hasOnlyWhitespaceText = clone.childNodes().all {
            it is TextNode && it.text().isBlank()
        }
        return if (clone.text().isBlank() && hasOnlyWhitespaceText) {
            images
        } else {
            null
        }
    }

    private fun Element.isStandalonePostImage(): Boolean {
        return !isEmoticonImage(attr("src"))
    }

    private fun Element.hasComplexContainer(): Boolean {
        if (tagName().lowercase() in COMPLEX_TAGS) {
            return true
        }
        val className = className().lowercase()
        return COMPLEX_CLASS_PARTS.any { className.contains(it) }
    }

    private fun isComplexHtml(html: String): Boolean {
        val lower = html.lowercase()
        return COMPLEX_TAGS.any { lower.contains("<$it") } ||
            COMPLEX_CLASS_PARTS.any { lower.contains("class=\"") && lower.contains(it) }
    }

    private fun isBlankTextBlock(html: String): Boolean {
        if (html.isBlank()) {
            return true
        }
        // 含媒体标签就不算 blank，无需再走 Jsoup.parseBodyFragment
        if (html.contains("<img", ignoreCase = true) ||
            html.contains("<video", ignoreCase = true) ||
            html.contains("<iframe", ignoreCase = true) ||
            html.contains("<object", ignoreCase = true) ||
            html.contains("<embed", ignoreCase = true)
        ) {
            return false
        }
        // 去掉标签和空白实体后，看是否还有可见字符
        val stripped = html
            .replace("<[^>]*>".toRegex(), "")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace('\u00A0', ' ')
            .trim()
        return stripped.isEmpty()
    }

    private fun normalizeImageUrl(src: String?): String? {
        if (src.isNullOrBlank()) {
            return null
        }
        return if (src.startsWith("http://") || src.startsWith("https://")) {
            src
        } else {
            Api.BASE_URL + src.removePrefix("/")
        }
    }

    private fun isEmoticonImage(src: String?): Boolean {
        if (src.isNullOrBlank()) {
            return false
        }
        val normalized = normalizeImageUrl(src)
        return Api.parseEmoticonName(src) != null ||
            Api.parseEmoticonName(src.removePrefix("/")) != null ||
            Api.parseEmoticonName(normalized) != null ||
            src.contains("/image/smiley/") ||
            src.startsWith("image/smiley/")
    }

    private fun String.toPositiveIntOrNull(): Int? {
        return trim().removeSuffix("px").toIntOrNull()?.takeIf { it > 0 }
    }

    companion object {
        private val COMPLEX_TAGS = setOf("blockquote", "table", "pre", "code")
        private val COMPLEX_CLASS_PARTS = setOf("quote", "blockcode")
        private const val MEDIA_TAGS_SELECTOR = "img,video,iframe,object,embed"
    }
}
