@file:Suppress("UNCHECKED_CAST", "unused")

package org.jetbrains.kotlin.abicmp

import java.io.File
import java.util.concurrent.Executors

fun main() {
    val path1 = "C:\\WORK\\jars-no-ir"
    val path2 = "C:\\WORK\\jars-with-ir"
    val id1 = "124141"
    val id2 = "125644"
    val reportPath = "C:\\WORK\\jars-comparison-report"

    val reportDir = File(reportPath)
    reportDir.deleteRecursively()
    reportDir.mkdirs()

    val executor = Executors.newWorkStealingPool()
    val task = DirComparisonTask(File(path1), File(path2), reportDir, id1, id2, executor)
    task.run()
}


