@file:Suppress("UNCHECKED_CAST", "unused")

package org.jetbrains.kotlin.abicmp

import org.jetbrains.kotlin.abicmp.tasks.DirTask
import java.io.File

fun main() {
    val dir1 = "C:\\WORK\\jars-kotlin-jvm"
    val dir2 = "C:\\WORK\\jars-kotlin-jvm-ir"
    // C:\WORK\jars-kotlin-jvm\jvm-abi-gen-1.4.255-20200706.124141-1.jar
    val id1 = "124141"
    // C:\WORK\jars-kotlin-jvm-ir\jvm-abi-gen-1.4.255-20200706.125644-1.jar
    val id2 = "125644"
    val reportPath = "C:\\WORK\\jars-kotlin-report"

    val reportDir = File(reportPath)
    reportDir.deleteRecursively()
    reportDir.mkdirs()

    val header1 = "JVM"
    val header2 = "JVM_IR"

    DirTask(File(dir1), File(dir2), id1, id2, header1, header2, reportDir, ).run()
}


