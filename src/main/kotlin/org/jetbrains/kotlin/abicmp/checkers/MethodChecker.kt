package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.compareAnnotations
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.kotlin.abicmp.toAnnotations
import org.objectweb.asm.tree.MethodNode
import kotlin.math.max
import kotlin.reflect.KProperty1

interface MethodChecker : Checker {
    fun check(method1: MethodNode, method2: MethodNode, report: MethodReport)
}

abstract class MethodPropertyChecker<T>(name: String) : PropertyChecker<T, MethodNode>(name), MethodChecker {
    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val value1 = getProperty(method1)
        val value2 = getProperty(method2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, valueToHtml(value1), valueToHtml(value2)))
        }
    }
}

inline fun <T> methodPropertyChecker(name: String, crossinline get: (MethodNode) -> T) =
        object : MethodPropertyChecker<T>(name) {
            override fun getProperty(node: MethodNode): T =
                    get(node)
        }

fun <T> methodPropertyChecker(methodProperty: KProperty1<MethodNode, T>) =
        methodPropertyChecker(methodProperty.name) { methodProperty.get(it) }

inline fun <T> methodPropertyChecker(methodProperty: KProperty1<MethodNode, T>, crossinline html: (T) -> String) =
        object : MethodPropertyChecker<T>(methodProperty.name) {
            override fun getProperty(node: MethodNode): T =
                    methodProperty.get(node)

            override fun valueToHtml(value: T): String =
                    html(value)
        }

fun <T> methodPropertyChecker(name: String, methodProperty: KProperty1<MethodNode, T>) =
        methodPropertyChecker(name) { methodProperty.get(it) }

class MethodAnnotationsChecker(annotationsProperty: KProperty1<MethodNode, List<Any?>?>) :
        AnnotationsChecker<MethodNode>(annotationsProperty), MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val anns1 = getAnnotations(method1)
        val anns2 = getAnnotations(method2)
        val listDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(name, listDiff.diff1, listDiff.diff2)
    }
}

class MethodParameterAnnotationsChecker(
        private val parameterAnnotationsProperty: KProperty1<MethodNode, Array<List<Any?>?>?>
) : MethodChecker {
    override val name = parameterAnnotationsProperty.name

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val paramAnnsList1 = parameterAnnotationsProperty.get(method1)?.toList().orEmpty()
        val paramAnnsList2 = parameterAnnotationsProperty.get(method2)?.toList().orEmpty()
        for (i in 0 until max(paramAnnsList1.size, paramAnnsList2.size)) {
            val anns1 = paramAnnsList1.getOrElse(i) { emptyList() }.toAnnotations()
            val anns2 = paramAnnsList2.getOrElse(i) { emptyList() }.toAnnotations()
            val listDiff = compareAnnotations(anns1, anns2) ?: continue
            report.addAnnotationDiffs("p$i:$name", listDiff.diff1, listDiff.diff2)
        }
    }
}
