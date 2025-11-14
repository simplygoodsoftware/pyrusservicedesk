package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

internal object HtmlUtils {

    fun extractButtons(body: String?): List<CommentEntry.ButtonEntry> {
        if (body == null) {
            return emptyList()
        }

        val doc = Jsoup.parse(body)
        val res = ArrayList<CommentEntry.ButtonEntry>()
        for (child in doc.body().children()) {
            val tagName = child.tagName()
            if (tagName == "a") {
                val attr = child.attributes()
                val href = attr.get("href")
                if (href.isBlank()) {
                    continue
                }
                val dataType = attr.get("data-type")

                if (dataType == "button") {
                    res +=  CommentEntry.ButtonEntry.Link(child.text(), href)
                }
            }
            else if (tagName == "button") {
                res += CommentEntry.ButtonEntry.Simple(child.text())
            }
        }

        return res
    }

}

internal fun String.cleanTags(
    br: String = "\n",
    removeLinkTag: Boolean = true,
): String {
    val doc = Jsoup.parse(this)
    val resText = StringBuilder()
    for (node in doc.body().childNodes()) {
        if (node is TextNode) {
            resText.append(node.wholeText)
        }
        else if (node is Element) {
            when (node.tagName()) {
                "br" -> resText.append(br)
                "p" -> resText.append(node.wholeText())
                "a" -> {
                    val attr = node.attributes()
                    val href = attr.get("href")
                    if (href.isBlank()) {
                        if (removeLinkTag) resText.append(node.wholeText())
                        else resText.append(node)
                    }
                    val dataType = attr.get("data-type")

                    if (dataType != "button") {
                        if (removeLinkTag) resText.append(node.wholeText())
                        else resText.append(node)
                    }
                }
                "button" -> {}
                else -> resText.append(node.wholeText())
            }
        }
    }
    return resText.toString()
}