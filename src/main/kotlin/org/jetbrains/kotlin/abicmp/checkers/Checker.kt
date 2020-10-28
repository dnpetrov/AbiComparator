package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.defects.DefectType
import org.jetbrains.kotlin.abicmp.defects.VALUE1_A
import org.jetbrains.kotlin.abicmp.defects.VALUE2_A
import org.jetbrains.kotlin.abicmp.toAnnotations
import org.jetbrains.kotlin.abicmp.toHtmlString
import kotlin.reflect.KProperty1

interface Checker {
    val name: String
}


abstract class PropertyChecker<T, E>(final override val name: String) : Checker {
    protected val defectType = DefectType(this.name, "Values mismatch: [VALUE1] != [VALUE2]", VALUE1_A, VALUE2_A)

    protected open fun areEqual(value1: T, value2: T) =
            value1 == value2

    protected open fun valueToHtml(value: T, other: T): String =
            value.toHtmlString()

    protected abstract fun getProperty(node: E): T
}


abstract class AnnotationsChecker<N>(
        final override val name: String,
        private val annotationsProperty: KProperty1<N, List<Any?>?>
) : Checker {

    protected fun getAnnotations(node: N) =
            annotationsProperty.get(node).orEmpty().toAnnotations()

    val mismatchDefect = DefectType("${name}.mismatch", "Annotations mismatch: [VALUE1] != [VALUE2]", VALUE1_A, VALUE2_A)
    val missing1Defect = DefectType("${name}.missing1", "Missing annotation in #1: [VALUE2]", VALUE2_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing annotation in #2: [VALUE1]", VALUE1_A)
}
