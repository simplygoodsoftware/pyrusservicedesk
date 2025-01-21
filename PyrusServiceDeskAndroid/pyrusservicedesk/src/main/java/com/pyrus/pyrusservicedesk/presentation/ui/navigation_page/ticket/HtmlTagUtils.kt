package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

internal object HtmlTagUtils {

    fun extractButtons(body: String?): List<CommentEntryV2.ButtonEntry> {
        if (body == null) {
            return emptyList()
        }

        val doc = Jsoup.parse(body)
        val res = ArrayList<CommentEntryV2.ButtonEntry>()
        for (child in doc.body().children()) {
            val tagName = child.tagName()
            if (tagName == "a") {
                val attr = child.attributes()
                val href = attr.get("href")
                if (href.isNullOrBlank()) {
                    continue
                }
                val dataType = attr.get("data-type")

                if (dataType == "button") {
                    res +=  CommentEntryV2.ButtonEntry.Link(child.text(), href)
                }
            }
            else if (tagName == "button") {
                res += CommentEntryV2.ButtonEntry.Simple(child.text())
            }
        }


        return res
    }

    fun cleanTags(htmlText: String): String {
        val doc = Jsoup.parse(htmlText)
        val resText = StringBuilder()
        for (node in doc.body().childNodes()) {
            if (node is TextNode) {
                resText.append(node.wholeText)
            }
            else if (node is Element) {
                when (node.tagName()) {
                    "br",
                    "p" -> resText.append(node.wholeText())
                    "a" -> {
                        if (!node.attributes().hasKey("data-type")) {
                            resText.append(node)
                        }
                    }
                    else -> {}
                }
            }
        }
        return resText.toString()
    }

}