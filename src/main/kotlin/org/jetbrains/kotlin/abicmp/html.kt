package org.jetbrains.kotlin.abicmp

import org.apache.commons.text.StringEscapeUtils

fun String?.escapeHtml(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(this)
}

fun Any?.toHtmlString(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(toString())
}