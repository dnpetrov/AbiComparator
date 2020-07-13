package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.escapeHtml
import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class MethodReport(
        val methodId: String,
        val header1: String,
        val header2: String
) : ComparisonReport {
    private val infoParagraphs = ArrayList<String>()

    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private val annotationDiffs = ArrayList<NamedDiffEntry>()

    fun addInfo(info: String) {
        infoParagraphs.add(info)
    }

    inline fun info(fm: PrintWriter.() -> Unit) {
        val bytes = ByteArrayOutputStream()
        val ps = PrintWriter(bytes)
        ps.fm()
        ps.close()
        addInfo(String(bytes.toByteArray()))
    }

    fun addPropertyDiff(diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
    }

    fun addAnnotationDiffs(name: String, values1: List<String>, values2: List<String>) {
        values1.zip(values2).forEach { (v1, v2) ->
            annotationDiffs.add(NamedDiffEntry(name, v1, v2))
        }
    }

    override fun isEmpty(): Boolean =
            propertyDiffs.isEmpty() &&
                    annotationDiffs.isEmpty()

    override fun write(output: PrintWriter) {
        output.tag("h2", "&gt; METHOD " + methodId.escapeHtml())

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        output.propertyDiffTable(header1, header2, propertyDiffs)
        output.annotationDiffTable(header1, header2, annotationDiffs)
    }
}