package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.ButtonEntry
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

internal object HtmlTagUtils {

    fun extractButtons(comment: Comment): List<ButtonEntry> {
        if (comment.body == null) {
            return emptyList()
        }

        val doc = Jsoup.parse(comment.body)
        val res = ArrayList<ButtonEntry>()
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
                    res +=  ButtonEntry.Link(child.text(), href)
                }
            }
            else if (tagName == "button") {
                res += ButtonEntry.Simple(child.text())
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