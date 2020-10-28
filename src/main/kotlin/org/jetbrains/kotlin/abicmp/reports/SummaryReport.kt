package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.defects.DefectInfo
import org.jetbrains.kotlin.abicmp.defects.Location
import java.io.File
import java.io.PrintWriter

class SummaryReport {
    private val defectsByInfo: MutableMap<DefectInfo, MutableSet<Location>> = HashMap()

    fun add(defectReport: DefectReport) {
        for (defect in defectReport.defects) {
            defectsByInfo.getOrPut(defect.info) { HashSet() }.add(defect.location)
        }
    }

    fun totalClusters() = defectsByInfo.keys.size
    fun totalDefects() = defectsByInfo.values.sumBy { it.size }

    fun writeReport(outputFile: File) {
        PrintWriter(outputFile).use { out ->
            out.tag("html") {
                out.tag("head") {
                    out.tag("style", REPORT_CSS)
                }
                out.tag("body") {
                    out.writeReportBody()
                }
            }
        }
    }

    private fun PrintWriter.writeReportBody() {
        for (info in defectsByInfo.keys.sorted()) {
            val locations = defectsByInfo[info]!!
            writeDefectInfo(info)
            tag("ul") {
                for (location in locations.toList().sorted()) {
                    tag("li") {
                        writeLocation(location)
                    }
                }
            }
        }
    }

    private fun PrintWriter.writeDefectInfo(info: DefectInfo) {
        tag("p") {
            tag("code") {
                println(info.message)
            }
        }
        table {
            tableData("type", info.type.id)
            for ((attr, value) in info.attributes) {
                tableData(attr.htmlId, "<code>${value.toHtmlString()}</code>")
            }
        }
    }

    private fun PrintWriter.writeLocation(location: Location) {
        println(location.reportString().toHtmlString())
    }

}