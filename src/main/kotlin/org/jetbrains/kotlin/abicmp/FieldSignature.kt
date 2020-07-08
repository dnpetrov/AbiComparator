@file:Suppress("unused")

package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty

class FieldSignature(id: String) : Entity(id)

class FieldProperty<T>(index: Int, parse: (FieldNode) -> T) :
        EntityProperty<FieldNode, T>(index, parse)

class FieldAnnotationsProperty(index: Int, getAnnotations: (FieldNode) -> List<Any?>?) :
        AnnotationsProperty<FieldNode, FieldSignature>(index, getAnnotations)

operator fun <T> FieldProperty<T>.provideDelegate(x: Nothing?, kProperty: KProperty<*>): FieldProperty<T> =
        apply { name = kProperty.name }

operator fun <T> FieldProperty<T>.getValue(x: FieldSignature, kProperty: KProperty<*>) =
        x.data[this] as T

val fieldProperties = ArrayList<FieldProperty<*>>()
val fieldAnnotationProperties = ArrayList<FieldAnnotationsProperty>()

fun <T> fieldProperty(parser: (FieldNode) -> T) =
        FieldProperty(fieldProperties.size, parser).also {
            fieldProperties.add(it)
        }

fun fieldAnnotationsProperty(parser: (FieldNode) -> List<Any?>?) =
        FieldAnnotationsProperty(fieldAnnotationProperties.size, parser).also {
            fieldAnnotationProperties.add(it)
        }

val FieldSignature.fieldName by fieldProperty { it.name }
val FieldSignature.desc by fieldProperty { it.desc }
val FieldSignature.signature by fieldProperty { it.signature }
val FieldSignature.initialValue by fieldProperty { it.value }
val FieldSignature.flags by fieldProperty { it.access }
val FieldSignature.flagsBits by fieldProperty { it.access.toString(2) }
val FieldSignature.flagsList by fieldProperty { it.access.fieldFlags() }

val FieldSignature.visibleAnnotations by fieldAnnotationsProperty { it.visibleAnnotations }
val FieldSignature.invisibleAnnotations by fieldAnnotationsProperty { it.invisibleAnnotations }

fun parseFieldSignature(fieldNode: FieldNode): FieldSignature {
    val fieldSignature = FieldSignature("${fieldNode.name}:${fieldNode.desc}")
    for (fp in fieldProperties) {
        fieldSignature.data[fp] = fp.parse(fieldNode)
    }
    for (fp in fieldAnnotationProperties) {
        fieldSignature.annotations[fp] = fp.parse(fieldNode)
    }
    return fieldSignature
}