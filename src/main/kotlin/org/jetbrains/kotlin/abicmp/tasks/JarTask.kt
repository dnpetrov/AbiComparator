package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.reports.JarReport
import org.jetbrains.kotlin.abicmp.reports.REPORT_CSS
import org.jetbrains.kotlin.abicmp.reports.isNotEmpty
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode
import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.util.jar.JarFile

class JarTask(
        header: String,
        private val jarFile1: JarFile,
        private val jarFile2: JarFile,
        private val header1: String,
        private val header2: String,
        private val outputFile: File
) : Runnable {

    private val ignoreAnonymousLocalClasses = true

    private val report = JarReport(header, header1, header2, jarFile1.name, jarFile2.name)

    private var totalDiffs = 0

    private val names1 = HashSet<String>()

    override fun run() {
        println("Comparing jars: ${jarFile1.name}, ${jarFile2.name}")

        addJarsInfo()

        checkJarFile1()
        checkJarFile2()

        writeReportIfRequired()
    }

    private fun checkJarFile1() {
        jarFile1.stream().forEach { entry1 ->
            val name1 = entry1.name
            if (name1.endsWith(".class")) {
                names1.add(name1)
                val entry2 = jarFile2.getEntry(name1)
                if (entry2 == null) {
                    val classNode = parseClassNode(jarFile1.getInputStream(entry1))
                    if (!classNode.shouldBeIgnored()) {
                        report.addMissingClassName2("$name1 ${classNode.access.classFlags()}")
                        ++totalDiffs
                    }
                } else {
                    val class1 = parseClassNode(jarFile1.getInputStream(entry1))
                    val class2 = parseClassNode(jarFile2.getInputStream(entry2))

                    if (!class1.shouldBeIgnored() || !class2.shouldBeIgnored()) {
                        println("Comparing classes: ${class1.name}")
                        val classReport = report.classReport(class1.name)
                        val classTask = ClassTask(class1, class2, classReport)
                        classTask.run()
                        if (classReport.isNotEmpty()) {
                            ++totalDiffs
                        }
                    } else {
                        println("Skipping $name1")
                    }
                }
            }
        }
    }

    private fun parseClassNode(input: InputStream): ClassNode =
            ClassNode().also { ClassReader(input).accept(it, ClassReader.SKIP_CODE) }

    private fun checkJarFile2() {
        jarFile2.stream().forEach { entry2 ->
            val name2 = entry2.name
            if (name2.endsWith(".class") && name2 !in names1) {
                val classNode = parseClassNode(jarFile2.getInputStream(entry2))
                if (!classNode.shouldBeIgnored()) {
                    report.addMissingClassName1("$name2 ${classNode.access.classFlags()}")
                    ++totalDiffs
                }
            }
        }
    }

    private fun writeReportIfRequired() {
        if (report.isNotEmpty()) {
            PrintWriter(outputFile).use { out ->
                out.tag("html") {
                    out.tag("head") {
                        out.printCss()
                    }
                    out.tag("body") {
                        report.write(out)
                    }
                }
            }
        }
    }

    private fun PrintWriter.printCss() {
        tag("style", REPORT_CSS)
    }

    private fun ClassNode.shouldBeIgnored(): Boolean {
        if (access.isSynthetic()) return true
        if (ignoreAnonymousLocalClasses && isAnonymousLocalClass()) return true
        return false
    }

    private fun ClassNode.isAnonymousLocalClass() =
            innerClasses.notNullList<InnerClassNode>().any {
                it.name == this.name && it.innerName == null
            }

    private fun addJarsInfo() {
        report.info {
            tag("p") {
                tag("b", header1)
                println(": ${jarFile1.name}")
            }
            tag("p") {
                tag("b", header2)
                println(": ${jarFile2.name}")
            }
        }
    }
}

