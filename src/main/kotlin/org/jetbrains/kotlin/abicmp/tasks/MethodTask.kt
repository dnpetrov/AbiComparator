package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.checkers.MethodAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.MethodParameterAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.methodPropertyChecker
import org.jetbrains.kotlin.abicmp.methodFlags
import org.jetbrains.kotlin.abicmp.notNullList
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.tag
import org.jetbrains.kotlin.abicmp.toAnnotationArgumentValue
import org.objectweb.asm.tree.MethodNode

class MethodTask(
        private val method1: MethodNode,
        private val method2: MethodNode,
        private val report: MethodReport
) : Runnable {

    private val methodCheckers = listOf(
            methodPropertyChecker(MethodNode::access) { v -> "${v.toString(2)} ${v.methodFlags()}"},
            methodPropertyChecker("methodName", MethodNode::name),
            methodPropertyChecker(MethodNode::desc),
            methodPropertyChecker(MethodNode::signature),
            methodPropertyChecker("exceptions") { it.exceptions.notNullList<String>().sorted() },
            methodPropertyChecker("annotationDefault") { it.annotationDefault?.toAnnotationArgumentValue() },
            MethodAnnotationsChecker(MethodNode::visibleAnnotations),
            MethodAnnotationsChecker(MethodNode::invisibleAnnotations),
            MethodParameterAnnotationsChecker(MethodNode::visibleParameterAnnotations),
            MethodParameterAnnotationsChecker(MethodNode::invisibleParameterAnnotations)
    )

    override fun run() {
        addMethodInfo()

        for (checker in methodCheckers) {
            checker.check(method1, method2, report)
        }
    }

    private fun addMethodInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${method1.access.methodFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${method2.access.methodFlags()}")
            }
        }
    }
}
