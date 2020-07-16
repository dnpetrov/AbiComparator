package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.kotlin.abicmp.methodFlags
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.tasks.methodId
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class MethodsListChecker : ClassChecker {
    override val name = "class.methods"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val methods1 = class1.methods.listOfNotNull<MethodNode>().associateBy { it.methodId() }
        val methods2 = class2.methods.listOfNotNull<MethodNode>().associateBy { it.methodId() }

        val relevantMethodIds = methods1.keys.union(methods2.keys)
                .filter {
                    val method1 = methods1[it]
                    val method2 = methods2[it]
                    (method1 != null && !method1.access.isSynthetic() ||
                            method2 != null && !method2.access.isSynthetic())
                }.toSet()

        val methodIds1 = methods1.keys.intersect(relevantMethodIds).sorted()

        val methodIds2 = methods2.keys.intersect(relevantMethodIds).sorted()

        val listDiff = compareLists(methodIds1, methodIds2) ?: return
        report.addMethodListDiffs(
                listDiff.diff1.map { it.toMethodWithFlags(methods1) },
                listDiff.diff2.map { it.toMethodWithFlags(methods2) }
        )
    }

    private fun String.toMethodWithFlags(methods: Map<String, MethodNode>): String {
        val method = methods[this] ?: return this
        return "$this ${method.access.methodFlags()}"
    }
}