package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.collections.ArrayList

class JarComparisonTask(
        private val jarFile1: File,
        private val jarFile2: File,
        reportFile: File
) : Runnable {

    private val names1 = TreeSet<String>()

    private var totalDiffs = 0

    private val entriesMissingInJar2 = ArrayList<String>()
    private val entriesMissingInJar1 = ArrayList<String>()
    private val jar1 = JarFile(jarFile1)
    private val jar2 = JarFile(jarFile2)
    private val report = Report(reportFile, jarFile1.parent, jarFile2.parent)

    override fun run() {
        report.begin()

        jar1.stream().forEach { entry1 ->
            val name1 = entry1.name
            if (name1.endsWith(".class")) {
                names1.add(name1)
                val entry2 = jar2.getEntry(name1)
                if (entry2 == null) {
                    val classNode = parseClassNode(jar1.getInputStream(entry1))
                    if (!classNode.isSynthetic()) {
                        entriesMissingInJar2.add(name1 + " " + classNode.access.classFlags())
                    }
                } else {
                    println("Comparing classes $name1")
                    val classId1 = jarFile1.path + ":" + name1
                    val classId2 = jarFile2.path + ":" + name1

                    val classNode1 = parseClassNode(jar1.getInputStream(entry1))
                    val classNode2 = parseClassNode(jar2.getInputStream(entry2))
                    if (!classNode1.isSynthetic() || !classNode2.isSynthetic()) {
                        val cs1 = parseClassSignature(classId1, classNode1)
                        val cs2 = parseClassSignature(classId2, classNode2)
                        val classTask = ClassComparisonTask(cs1, cs2, report)
                        classTask.run()
                        if (classTask.hasDiff) {
                            ++totalDiffs
                        }
                    }
                }
            }
        }

        jar2.stream().forEach { entry2 ->
            val name2 = entry2.name
            if (name2.endsWith(".class") && name2 !in names1) {
                val classNode = parseClassNode(jar2.getInputStream(entry2))
                if (!classNode.isSynthetic()) {
                    entriesMissingInJar1.add(name2 + " " + classNode.access.classFlags())
                }
            }
        }

        if (entriesMissingInJar1.isNotEmpty()) {
            report.reportMissingEntries(jarFile1.path, entriesMissingInJar1)
            ++totalDiffs
        }

        if (entriesMissingInJar2.isNotEmpty()) {
            report.reportMissingEntries(jarFile2.path, entriesMissingInJar2)
            ++totalDiffs
        }

        report.println("<p>Total diffs: <b>$totalDiffs</b></p>")

        report.end()

        if (totalDiffs > 0) {
            report.save()
        }
    }

    private fun ClassNode.isSynthetic() = access and Opcodes.ACC_SYNTHETIC != 0
}