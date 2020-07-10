package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.compareAnnotations
import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty1

interface FieldChecker : Checker {
    fun check(field1: FieldNode, field2: FieldNode, report: FieldReport)
}

abstract class FieldPropertyChecker<T>(name: String) : PropertyChecker<T, FieldNode>(name), FieldChecker {
    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val value1 = getProperty(field1)
        val value2 = getProperty(field2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, valueToHtml(value1), valueToHtml(value2)))
        }
    }
}

inline fun <T> fieldPropertyChecker(name: String, crossinline get: (FieldNode) -> T) =
        object : FieldPropertyChecker<T>(name) {
            override fun getProperty(node: FieldNode): T =
                    get(node)
        }

fun <T> fieldPropertyChecker(fieldProperty: KProperty1<FieldNode, T>) =
        fieldPropertyChecker(fieldProperty.name) { fieldProperty.get(it) }

fun <T> fieldPropertyChecker(name: String, fieldProperty: KProperty1<FieldNode, T>) =
        fieldPropertyChecker(name) { fieldProperty.get(it) }

class FieldAnnotationsChecker(annotationsProperty: KProperty1<FieldNode, List<Any?>?>) :
        AnnotationsChecker<FieldNode>(annotationsProperty), FieldChecker {

    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val anns1 = getAnnotations(field1)
        val anns2 = getAnnotations(field2)
        val listDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(name, listDiff.diff1, listDiff.diff2)
    }
}

