package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.objectweb.asm.tree.FieldNode

abstract class FieldPropertyChecker<T>(name: String) :
        PropertyChecker<T, FieldNode>("field.$name"),
        FieldChecker {

    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val value1 = getProperty(field1)
        val value2 = getProperty(field2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(
                    defectType,
                    NamedDiffEntry(
                            name,
                            valueToHtml(value1, value2),
                            valueToHtml(value2, value1)
                    )
            )
        }
    }
}