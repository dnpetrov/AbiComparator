package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.tree.AnnotationNode
import kotlin.reflect.KProperty

data class AnnotationEntry(val desc: String, val values: List<Any?>)

fun List<Any?>?.toAnnotations() =
        this?.run {
            mapNotNull { it.toAnnotation() }
                    .sortedBy { it.desc }
        } ?: emptyList()

fun Any?.toAnnotation(): AnnotationEntry? {
    val ann = this as? AnnotationNode ?: return null
    return AnnotationEntry(ann.desc, ann.values.orEmpty().map { it!!.toAnnotationArgumentValue() })
}

private fun Any.toAnnotationArgumentValue(): Any =
        if (this is Array<*>)
            toList().map { it!!.toAnnotationArgumentValue() }
        else
            this

abstract class AnnotationsProperty<S, E : Entity>(index: Int, getAnnotations: (S) -> List<Any?>?) :
        EntityProperty<S, List<AnnotationEntry>>(index, { getAnnotations(it).toAnnotations() })

operator fun <S, E : Entity> AnnotationsProperty<S, E>
        .provideDelegate(x: Nothing?, kProperty: KProperty<*>): AnnotationsProperty<S, E> {
    name = kProperty.name
    return this
}

operator fun <S, E : Entity> AnnotationsProperty<S, E>.getValue(x: E, kProperty: KProperty<*>): List<AnnotationEntry> =
        x.annotations[this] ?: emptyList()

