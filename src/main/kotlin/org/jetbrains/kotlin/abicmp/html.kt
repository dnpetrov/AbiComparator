package org.jetbrains.kotlin.abicmp

import org.apache.commons.text.StringEscapeUtils
import java.io.PrintWriter

fun String?.escapeHtml(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(this)
}

fun Any?.toHtmlString(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(toString()).replace("\n", "<br>")
}

fun PrintWriter.tag(tagName: String) {
    print("<$tagName/>")
}

inline fun PrintWriter.tag(tagName: String, body: () -> Unit) {
    print("<$tagName>")
    body()
    println("</$tagName>")
}

fun PrintWriter.tag(tagName: String, content: String) {
    println("<$tagName>$content</$tagName>")
}

inline fun PrintWriter.table(body: () -> Unit) {
    tag("table", body)
}

fun PrintWriter.tableHeader(vararg headers: String) {
    tag("tr") {
        for (header in headers) {
            tag("th", header)
        }
    }
}

fun PrintWriter.tableData(vararg data: String) {
    tag("tr") {
        for (d in data) {
            tag("td", d)
        }
    }
}