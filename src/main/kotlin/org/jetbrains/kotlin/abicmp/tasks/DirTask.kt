package org.jetbrains.kotlin.abicmp.tasks

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.jar.JarFile

class DirTask(
        private val dir1: File,
        private val dir2: File,
        private val id1: String?,
        private val id2: String?,
        private val header1: String,
        private val header2: String,
        private val reportDir: File,
        private val checkerConfiguration: CheckerConfiguration
) : Runnable {

    private val executor = Executors.newWorkStealingPool()

    private val lastNameIndex = HashMap<String, Int>()
    private val tasks = ArrayList<Future<*>>()

    override fun run() {
        println("Comparing directories: $dir1, $dir2")
        walkRecursively(dir1, dir2)
        tasks.forEach { it.get() }
        println("Done")
    }

    private fun walkRecursively(subdir1: File, subdir2: File) {
        val files1 = subdir1.listFiles() ?: return
        for (file1 in files1) {
            val file2 = File(subdir2, file1.name.replaceIfNotNull(id1, id2))
            if (file1.canRead() && file2.exists() && file2.canRead()) {
                if (file1.isDirectory) {
                    if (file2.isDirectory) {
                        println("Comparing subdirectories: $file1, $file2")
                        walkRecursively(file1, file2)
                    }
                } else if (file1.name.endsWith(".jar")) {
                    println("Comparing jars: $file1, $file2")
                    val index0 = lastNameIndex.getOrElse(file1.name) { 0 }
                    val index = index0 + 1
                    lastNameIndex[file1.name] = index
                    val jarTaskHeader = file1.name.replaceIfNotNull(id1, "").replace(".jar", "")
                    val reportFile = File(reportDir, "$jarTaskHeader-REPORT-$index.html")
                    val jarTask = JarTask(
                            jarTaskHeader,
                            JarFile(file1), JarFile(file2),
                            header1, header2,
                            reportFile, checkerConfiguration
                    )
                    tasks.add(executor.submit(jarTask))
                }
            }
        }
    }

    private fun String.replaceIfNotNull(pattern: String?, replaceWith: String?) =
            if (pattern != null && replaceWith != null)
                replace(pattern, replaceWith)
            else
                this
}