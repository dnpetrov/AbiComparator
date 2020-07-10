package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class JarReport(
        private val header: String,
        private val header1: String,
        private val header2: String,
        private val jarFileName1: String,
        private val jarFileName2: String
) : ComparisonReport {
    private val infoParagraphs = ArrayList<String>()

    private val classReports = ArrayList<ClassReport>()

    private val missingClassNames1 = HashSet<String>()
    private val missingClassNames2 = HashSet<String>()

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

    fun classReport(classInternalName: String) =
            ClassReport(classInternalName, header1, header2).also { classReports.add(it) }

    private fun getFilteredClassReports(): List<ClassReport> =
            classReports.filter { !it.isEmpty() }.sortedBy { it.classInternalName }

    fun addMissingClassName1(classInternalName: String) {
        missingClassNames1.add(classInternalName)
    }

    fun addMissingClassName2(classInternalName: String) {
        missingClassNames2.add(classInternalName)
    }

    override fun isEmpty(): Boolean =
            missingClassNames1.isEmpty() && missingClassNames2.isEmpty() &&
                    getFilteredClassReports().isEmpty()

    override fun write(output: PrintWriter) {
        output.tag("h1", header)

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        for (classReport in getFilteredClassReports()) {
            classReport.write(output)
        }

        writeMissingClasses(output, jarFileName1, missingClassNames1)
        writeMissingClasses(output, jarFileName2, missingClassNames2)
    }

    private fun writeMissingClasses(output: PrintWriter, name: String, missing: Collection<String>) {
        if (missing.isNotEmpty()) {
            output.tag("p", "Classes missing in $name: <b>${missing.size}</b>")
            output.tag("ul") {
                for (className in missing) {
                    output.tag("li", className)
                }
            }
        }
    }
}

