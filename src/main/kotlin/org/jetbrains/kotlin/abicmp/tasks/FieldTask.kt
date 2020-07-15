package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.checkers.FieldAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.fieldPropertyChecker
import org.jetbrains.kotlin.abicmp.fieldFlags
import org.jetbrains.kotlin.abicmp.methodFlags
import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.kotlin.abicmp.tag
import org.objectweb.asm.tree.FieldNode

class FieldTask(
        private val field1: FieldNode,
        private val field2: FieldNode,
        private val report: FieldReport
) : Runnable {

    private val fieldCheckers = listOf(
            fieldPropertyChecker(FieldNode::access) { v -> "${v.toString(2)} ${v.fieldFlags()}"},
            fieldPropertyChecker("fieldName", FieldNode::name),
            fieldPropertyChecker(FieldNode::desc),
            fieldPropertyChecker(FieldNode::signature),
            fieldPropertyChecker("initialValue", FieldNode::value),
            FieldAnnotationsChecker(FieldNode::visibleAnnotations),
            FieldAnnotationsChecker(FieldNode::invisibleAnnotations)
    )

    override fun run() {
        addFieldInfo()

        for (checker in fieldCheckers) {
            checker.check(field1, field2, report)
        }
    }

    private fun addFieldInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${field1.access.fieldFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${field2.access.fieldFlags()}")
            }
        }
    }
}
