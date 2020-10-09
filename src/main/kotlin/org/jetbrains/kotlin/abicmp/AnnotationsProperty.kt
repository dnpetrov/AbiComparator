package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.tree.AnnotationNode

data class AnnotationEntry(val desc: String, val values: List<Pair<String, Any?>>)

fun List<Any?>?.toAnnotations() =
        this?.run {
            mapNotNull { it.toAnnotation() }
                    .sortedBy { it.desc }
        } ?: emptyList()

fun Any?.toAnnotation(): AnnotationEntry? {
    val ann = this as? AnnotationNode ?: return null
    val annValues = ann.values.orEmpty()
    val iter = annValues.iterator()
    val values = ArrayList<Pair<String, Any?>>()
    while (iter.hasNext()) {
        val key = iter.next() as String
        val value = iter.next()!!.toAnnotationArgumentValue()
        values.add(key to value)
    }
    return AnnotationEntry(ann.desc, values.sortedBy { it.first })
}

fun Any.toAnnotationArgumentValue(): Any =
        when (this) {
            is Array<*> -> map { it!!.toAnnotationArgumentValue() }
            is List<*> -> map { it!!.toAnnotationArgumentValue() }
            is AnnotationNode -> this.toAnnotation()!!
            else -> this
        }

