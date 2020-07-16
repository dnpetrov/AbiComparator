package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.toAnnotations
import org.jetbrains.kotlin.abicmp.toHtmlString
import kotlin.reflect.KProperty1

interface Checker {
    val name: String
}

abstract class PropertyChecker<T, E>(override val name: String) : Checker {
    protected open fun areEqual(value1: T, value2: T) =
            value1 == value2

    protected open fun valueToHtml(value: T, other: T): String =
            value.toHtmlString()

    protected abstract fun getProperty(node: E): T
}

abstract class AnnotationsChecker<N>(
        private val annotationsProperty: KProperty1<N, List<Any?>?>
) : Checker {

    protected fun getAnnotations(node: N) =
            annotationsProperty.get(node).orEmpty().toAnnotations()
}
