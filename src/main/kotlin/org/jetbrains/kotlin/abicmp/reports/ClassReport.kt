package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.escapeHtml
import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class ClassReport(
        val classInternalName: String,
        val header1: String,
        val header2: String
) : ComparisonReport {
    private val infoParagraphs = ArrayList<String>()

    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private val annotationDiffs = ArrayList<NamedDiffEntry>()
    private val methodListDiffs = ArrayList<DiffEntry>()
    private val methodReports = ArrayList<MethodReport>()
    private val fieldListDiffs = ArrayList<DiffEntry>()
    private val fieldReports = ArrayList<FieldReport>()

    override fun isEmpty(): Boolean =
            propertyDiffs.isEmpty() &&
                    annotationDiffs.isEmpty() &&
                    methodListDiffs.isEmpty() &&
                    getFilteredMethodReports().isEmpty() &&
                    fieldListDiffs.isEmpty() &&
                    getFilteredFieldReports().isEmpty()

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

    fun addMethodListDiffs(values1: List<String>, values2: List<String>) {
        values1.zip(values2).forEach { (v1, v2) ->
            methodListDiffs.add(DiffEntry(v1, v2))
        }
    }

    fun addFieldListDiffs(values1: List<String>, values2: List<String>) {
        values1.zip(values2).forEach { (v1, v2) ->
            fieldListDiffs.add(DiffEntry(v1, v2))
        }
    }

    fun methodReport(methodId: String): MethodReport =
            MethodReport(methodId, header1, header2).also { methodReports.add(it) }

    fun fieldReport(fieldId: String): FieldReport =
            FieldReport(fieldId, header1, header2).also { fieldReports.add(it) }

    private fun getFilteredMethodReports() =
            methodReports.filter { !it.isEmpty() }.sortedBy { it.methodId }

    private fun getFilteredFieldReports() =
            fieldReports.filter { !it.isEmpty() }.sortedBy { it.fieldId }

    override fun write(output: PrintWriter) {
        output.tag("h1", "CLASS " + classInternalName.escapeHtml())

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        output.propertyDiffTable(header1, header2, propertyDiffs)

        output.annotationDiffTable(header1, header2, annotationDiffs)

        output.listDiff(header1, header2, methodListDiffs)

        for (mr in getFilteredMethodReports()) {
            mr.write(output)
        }

        output.listDiff(header1, header2, fieldListDiffs)

        for (fr in getFilteredFieldReports()) {
            fr.write(output)
        }
    }
}