@file:Suppress("SameParameterValue")

package org.jetbrains.kotlin.abicmp

import java.io.*
import java.nio.charset.StandardCharsets

const val REPORT_CSS = """
table, th, td {
  border: 1px solid black;
  border-collapse: collapse;
  padding: 2px;
}
th {
  background: #B0B0B0;
}
td {
  background: #EFEFB0;
}
"""

class Report(
        private val outFile: File,
        private val header1: String,
        private val header2: String
) {
    private val outBuf = ByteArrayOutputStream()
    private val out = PrintStream(outBuf)

    fun begin() {
        out.println("<html><head>")
        printCss()
        out.println("</head><body>")
    }

    private fun printCss() {
        tag("style", REPORT_CSS)
    }

    private inline fun tag(tag: String, body: () -> Unit) {
        out.print("<$tag>")
        body()
        out.println("</$tag>")
    }

    private fun tag(tag: String, content: String) {
        tag(tag) {
            out.print(content)
        }
    }

    private fun list(listTag: String, itemTag: String, vararg items: String) {
        tag(listTag) {
            for (item in items) {
                tag(itemTag, item)
            }
        }
    }

    private fun space() {
        out.print("&nbsp;")
    }

    fun reportClassDiff(diff: ClassDiff) {
        tag("h1", diff.name)

        tag("p", "$header1: ${diff.classInfo1}")
        tag("p", "$header2: ${diff.classInfo2}")

        if (diff.propertyDiffs.isNotEmpty()) {
            tag("table") {
                list("tr", "th", "Property", header1, header2, "Details")
                for (propertyDiff in diff.propertyDiffs) {
                    list("tr", "td", propertyDiff.property, propertyDiff.value1, propertyDiff.value2, propertyDiff.details)
                }
            }
            space()
        }

        if (diff.annotationsDiffs.isNotEmpty()) {
            tag("table") {
                list("tr", "th", "Property", header1, header2)
                for (annDiff in diff.annotationsDiffs) {
                    annDiff.diff1.zip(annDiff.diff2).forEach { (entry1, entry2) ->
                        list("tr", "td", annDiff.property, entry1.toHtmlDiff(), entry2.toHtmlDiff())
                    }
                }
            }
            space()
        }

        val methodsListDiff = diff.methodsListDiff
        if (methodsListDiff != null) {
            tag("table") {
                list("tr", "th", header1, header2)
                methodsListDiff.diff1.zip(methodsListDiff.diff2).forEach { (entry1, entry2) ->
                    list("tr", "td", entry1.toHtmlDiff(), entry2.toHtmlDiff())
                }
            }
            space()
        }
    }

    private fun String.toHtmlDiff() = "<code>${escapeHtml()}</code>"

    private fun List<String>.toHtmlDiff(): String =
            joinToString(separator = "<br>") { it.toHtmlDiff() }

    fun reportMissingEntries(artifact: String, entries: List<String>) {
        tag("p") {
            out.println("Entries missing in $artifact: <b>${entries.size}</b></br>")
            tag("ul") {
                for (entry in entries) {
                    tag("li", entry)
                }
            }
        }
    }

    fun println(s: String) {
        out.println(s)
    }

    fun end() {
        out.println("</body></html>")
        out.close()
    }

    fun save() {
        PrintStream(FileOutputStream(outFile)).use { fileOut ->
            fileOut.print(String(outBuf.toByteArray()))
        }
    }
}