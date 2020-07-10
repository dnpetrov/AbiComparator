@file:Suppress("UNCHECKED_CAST", "unused")

package org.jetbrains.kotlin.abicmp

import org.jetbrains.kotlin.abicmp.tasks.DirTask
import java.io.File

fun main() {
    val dir1 = "C:\\WORK\\jars-compiled-jvm"
    val dir2 = "C:\\WORK\\jars-compiled-jvm-ir"
    val id1 = "124141"
    val id2 = "125644"
    val reportPath = "C:\\WORK\\jars-comparison-report2"

    val reportDir = File(reportPath)
    reportDir.deleteRecursively()
    reportDir.mkdirs()

    val header1 = "JVM"
    val header2 = "JVM_IR"

    DirTask(File(dir1), File(dir2), id1, id2, header1, header2, reportDir, ).run()
}


