package org.jetbrains.kotlin.abicmp

import org.jetbrains.kotlin.abicmp.tasks.DirTask
import org.jetbrains.kotlin.abicmp.tasks.checkerConfiguration
import java.io.File

fun main() {
    checkKotlin()
}

private fun checkKotlin() {
    val dir1 = "C:\\WORK\\jars-kotlin-jvm"
    val dir2 = "C:\\WORK\\jars-kotlin-jvm-ir"
    val id1 = "130533"
    val id2 = "132228"
    val reportPath = "C:\\WORK\\jars-kotlin-report"

    val header1 = "JVM"
    val header2 = "JVM_IR"

    val checkerConfiguration = checkerConfiguration {}
//    val checkerConfiguration = checkerConfiguration {
//        enableExclusively("class.metadata")
//    }

    val reportDir = File(reportPath)
    reportDir.deleteRecursively()
    reportDir.mkdirs()

    println("Checkers:")
    println(checkerConfiguration.enabledCheckers.joinToString(separator = "\n") { " * ${it.name}" })

    DirTask(File(dir1), File(dir2), id1, id2, header1, header2, reportDir, checkerConfiguration).run()
}


