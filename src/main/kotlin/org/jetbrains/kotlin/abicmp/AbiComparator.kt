@file:Suppress("UNCHECKED_CAST", "unused")

package org.jetbrains.kotlin.abicmp

import org.jetbrains.kotlin.abicmp.tasks.DirTask
import java.io.File

fun main() {
    val dir1 = "C:\\WORK\\jars-atrium-jvm"
    val dir2 = "C:\\WORK\\jars-atrium-jvm-ir"
    val id1 = null
    val id2 = null
    val reportPath = "C:\\WORK\\jars-atrium-report"

    val reportDir = File(reportPath)
    reportDir.deleteRecursively()
    reportDir.mkdirs()

    val header1 = "JVM"
    val header2 = "JVM_IR"

    DirTask(File(dir1), File(dir2), id1, id2, header1, header2, reportDir, ).run()
}


