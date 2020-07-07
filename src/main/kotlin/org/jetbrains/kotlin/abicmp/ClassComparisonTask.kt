@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.Opcodes

class ClassComparisonTask(
        private val cs1: ClassSignature,
        private val cs2: ClassSignature,
        private val report: Report
) : Runnable {
    private val classDiff = ClassDiff(cs1.className)

    val hasDiff get() = classDiff.isNotEmpty()

    override fun run() {
        classDiff.classInfo1 = cs1.flags.classFlags()
        classDiff.classInfo2 = cs2.flags.classFlags()

        compareClassProperties()
        compareClassAnnotations()
        compareMethods()

        if (classDiff.isNotEmpty()) {
            report.reportClassDiff(classDiff)
        }
    }

    private fun compareClassProperties() {
        for (classProperty in classProperties) {
            val valueToHtml = classProperty.valueToHtml as (Any?) -> String
            val diff = classProperty.diff as (Any?, Any?) -> String?

            val val1 = cs1.data[classProperty]
            val val2 = cs2.data[classProperty]
            val diffDetails = diff(val1, val2)
            if (diffDetails != null) {
                val propertyDiff = PropertyDiff(classProperty.name, valueToHtml(val1), valueToHtml(val2), diffDetails)
                classDiff.propertyDiffs.add(propertyDiff)
            }
        }
    }

    private fun compareClassAnnotations() {
        for (classAnnotationsProperty in classAnnotationsProperties) {
            val anns1 = cs1.annotations[classAnnotationsProperty].orEmpty()
            val anns2 = cs2.annotations[classAnnotationsProperty].orEmpty()
            val diff = compareAnnotations(classAnnotationsProperty.name, anns1, anns2)
            if (diff != null) {
                classDiff.annotationsDiffs.add(diff)
            }
        }
    }

    private fun compareMethods() {
        val methods1 = cs1.methods.preprocessMethods()
        val methods2 = cs2.methods.preprocessMethods()
        var i1 = 0
        var i2 = 0
        val size1 = methods1.size
        val size2 = methods2.size
        var hasMethodsListDiff = false
        val methodsListDiff1 = ArrayList<String>()
        val methodsListDiff2 = ArrayList<String>()
        while (i1 < size1 || i2 < size2) {
            when {
                i1 < size1 && i2 < size2 -> {
                    val m1 = methods1[i1]
                    val m2 = methods2[i2]
                    when {
                        m1.id == m2.id -> {
                            compareMethods(m1, m2)
                            ++i1
                            ++i2
                        }
                        m1.id < m2.id -> {
                            hasMethodsListDiff = true
                            methodsListDiff1.add(m1.methodDiffLine())
                            methodsListDiff2.add("---")
                            ++i1
                        }
                        else -> {
                            hasMethodsListDiff = true
                            methodsListDiff1.add("---")
                            methodsListDiff2.add(m2.methodDiffLine())
                            ++i2
                        }
                    }
                }
                i1 < size1 -> {
                    val m1 = methods1[i1]
                    hasMethodsListDiff = true
                    methodsListDiff1.add(m1.methodDiffLine())
                    methodsListDiff2.add("---")
                    ++i1
                }
                else -> {
                    val m2 = methods2[i2]
                    hasMethodsListDiff = true
                    methodsListDiff1.add("---")
                    methodsListDiff2.add(m2.methodDiffLine())
                    ++i2
                }
            }
        }

        if (hasMethodsListDiff) {
            classDiff.methodsListDiff = MethodsListDiff(methodsListDiff1, methodsListDiff2)
        }
    }

    private fun MethodSignature.methodDiffLine() =
            "${flags.toString(2)} $id ${flags.methodFlags()}"

    private fun compareMethods(m1: MethodSignature, m2: MethodSignature) {
        // TODO
    }

    private fun List<MethodSignature>.preprocessMethods() =
            filter {
                val flags = it.flags
                flags and Opcodes.ACC_SYNTHETIC == 0
            }.sortedBy { it.id }

}