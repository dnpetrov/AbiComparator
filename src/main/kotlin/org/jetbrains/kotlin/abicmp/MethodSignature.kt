@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KProperty

class MethodSignature(id: String) : Entity(id)

class MethodProperty<T>(index: Int, parse: (MethodNode) -> T) :
        EntityProperty<MethodNode, T>(index, parse)

class MethodAnnotationsProperty(index: Int, getAnnotations: (MethodNode) -> List<Any?>?) :
        AnnotationsProperty<MethodNode, MethodSignature>(index, getAnnotations)

operator fun <T> MethodProperty<T>.provideDelegate(x: Nothing?, kProperty: KProperty<*>): MethodProperty<T> =
        apply { name = kProperty.name }

operator fun <T> MethodProperty<T>.getValue(x: MethodSignature, kProperty: KProperty<*>) =
        x.data[this] as T

val methodProperties = ArrayList<MethodProperty<*>>()
val methodAnnotationsProperties = ArrayList<MethodAnnotationsProperty>()

fun <T> methodProperty(parser: (MethodNode) -> T) =
        MethodProperty<T>(methodProperties.size, parser).also {
            methodProperties.add(it)
        }

fun methodAnnotationsProperty(parser: (MethodNode) -> List<Any?>?) =
        MethodAnnotationsProperty(methodAnnotationsProperties.size, parser).also {
            methodAnnotationsProperties.add(it)
        }

val MethodSignature.methodName by methodProperty { it.name }
val MethodSignature.desc by methodProperty { it.desc }
val MethodSignature.signature by methodProperty { it.signature }
val MethodSignature.exceptions by methodProperty { it.exceptions }
val MethodSignature.flags by methodProperty { it.access }

val MethodSignature.visibleAnnotations by methodAnnotationsProperty { it.visibleAnnotations }
val MethodSignature.invisibleAnnotations by methodAnnotationsProperty { it.invisibleAnnotations }
//val MethodSignature.visibleTypeAnnotations by methodAnnotationsProperty { it.visibleTypeAnnotations }
//val MethodSignature.invisibleTypeAnnotations by methodAnnotationsProperty { it.invisibleTypeAnnotations }

fun parseMethodSignature(methodNode: MethodNode): MethodSignature {
    val methodSignature = MethodSignature(methodNode.name + methodNode.desc)
    for (mp in methodProperties) {
        methodSignature.data[mp] = mp.parse(methodNode)
    }
    for (mp in methodAnnotationsProperties) {
        methodSignature.annotations[mp] = mp.parse(methodNode)
    }
    // TODO parameters
    return methodSignature
}