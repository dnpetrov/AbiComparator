package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.defects.*
import org.jetbrains.kotlin.abicmp.isBridge
import org.jetbrains.kotlin.abicmp.isPrivate
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode
import kotlin.math.max
import kotlin.reflect.KProperty1

const val ignoreMissingNullabilityAnnotationsOnInvisibleMethods = true

interface MethodChecker : Checker {
    fun check(method1: MethodNode, method2: MethodNode, report: MethodReport)
}

abstract class MethodPropertyChecker<T>(name: String) :
        PropertyChecker<T, MethodNode>("method.$name"),
        MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val value1 = getProperty(method1)
        val value2 = getProperty(method2)
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

            override fun valueToHtml(value: T, other: T): String =
                    html(value)
        }

fun <T> methodPropertyChecker(name: String, methodProperty: KProperty1<MethodNode, T>) =
        methodPropertyChecker(name) { methodProperty.get(it) }

fun areEquallyInvisible(method1: MethodNode, method2: MethodNode) =
        method1.access.isPrivate() && method2.access.isPrivate() ||
                method1.access.isSynthetic() && method2.access.isSynthetic() ||
                method1.access.isBridge() && method2.access.isBridge() ||
                method1.name.contains('-') && method2.name.contains('-')

fun List<AnnotationEntry>.ignoreMissingNullabilityAnnotationsOnMethod(
        method1: MethodNode,
        method2: MethodNode,
        anns1: List<AnnotationEntry>
) =
        if (ignoreMissingNullabilityAnnotationsOnInvisibleMethods &&
                areEquallyInvisible(method1, method2) &&
                anns1.none { it.isNullabilityAnnotation() }
        )
            filterNot { it.isNullabilityAnnotation() }
        else
            this

class MethodAnnotationsChecker(annotationsProperty: KProperty1<MethodNode, List<Any?>?>) :
        AnnotationsChecker<MethodNode>("method.${annotationsProperty.name}", annotationsProperty),
        MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val anns1 = getAnnotations(method1)
        val anns2 = getAnnotations(method2).ignoreMissingNullabilityAnnotationsOnMethod(method1, method2, anns1)
        val annDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(this, annDiff)
    }
}

class MethodParameterAnnotationsChecker(
        private val parameterAnnotationsProperty: KProperty1<MethodNode, Array<List<AnnotationNode?>?>?>
) : MethodChecker {

    override val name = "method.parameters.${parameterAnnotationsProperty.name}"

    val mismatchDefect = DefectType("${name}.mismatch", "Value parameter annotation mismatch", METHOD_A, VP_INDEX_A, VALUE1_A, VALUE2_A)
    val missing1Defect = DefectType("${name}.missing1", "Missing value parameter annotation in #1", METHOD_A, VP_INDEX_A, VALUE2_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing value parameter annotation in #2", METHOD_A, VP_INDEX_A, VALUE1_A)

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val paramAnnsList1 = parameterAnnotationsProperty.get(method1)?.toList().orEmpty()
        val paramAnnsList2 = parameterAnnotationsProperty.get(method2)?.toList().orEmpty()
        for (i in 0 until max(paramAnnsList1.size, paramAnnsList2.size)) {
            val anns1 = paramAnnsList1.getOrElse(i) { emptyList() }.toAnnotations()
            val anns2 = paramAnnsList2.getOrElse(i) { emptyList() }.toAnnotations()
                    .ignoreMissingNullabilityAnnotationsOnMethod(method1, method2, anns1)
            val annDiff = compareAnnotations(anns1, anns2) ?: continue
            report.addValueParameterAnnotationDiffs(this, i, annDiff)
        }
    }
}
