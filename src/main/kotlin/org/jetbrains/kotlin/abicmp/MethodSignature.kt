@file:Suppress("UNCHECKED_CAST", "unused")

package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.tree.MethodNode
import java.lang.AssertionError
import kotlin.reflect.KProperty

class MethodSignature(id: String) : Entity(id) {
    val parameters = ArrayList<ParameterSignature>()
}

class ParameterSignature(id: String) : Entity(id)

class MethodProperty<T>(index: Int, parse: (MethodNode) -> T) :
        EntityProperty<MethodNode, T>(index, parse)

class MethodAnnotationsProperty(index: Int, getAnnotations: (MethodNode) -> List<Any?>?) :
        AnnotationsProperty<MethodNode, MethodSignature>(index, getAnnotations)

class ParameterAnnotationsProperty(index: Int) :
        AnnotationsProperty<MethodNode, MethodSignature>(index, { throw AssertionError() })

operator fun <T> MethodProperty<T>.provideDelegate(x: Nothing?, kProperty: KProperty<*>): MethodProperty<T> =
        apply { name = kProperty.name }

operator fun <T> MethodProperty<T>.getValue(x: MethodSignature, kProperty: KProperty<*>) =
        x.data[this] as T

val methodProperties = ArrayList<MethodProperty<*>>()
val methodAnnotationsProperties = ArrayList<MethodAnnotationsProperty>()

fun <T> methodProperty(parser: (MethodNode) -> T) =
        MethodProperty(methodProperties.size, parser).also {
            methodProperties.add(it)
        }

fun methodAnnotationsProperty(parser: (MethodNode) -> List<Any?>?) =
        MethodAnnotationsProperty(methodAnnotationsProperties.size, parser).also {
            methodAnnotationsProperties.add(it)
        }

val MethodSignature.methodName by methodProperty { it.name }
val MethodSignature.desc by methodProperty { it.desc }
val MethodSignature.signature by methodProperty { it.signature }
val MethodSignature.exceptions by methodProperty { it.exceptions.map { it as String }.sorted() }
val MethodSignature.flags by methodProperty { it.access }
val MethodSignature.flagsBiys by methodProperty { it.access.toString(2) }
val MethodSignature.flagsList by methodProperty { it.access.methodFlags() }

val MethodSignature.visibleAnnotations by methodAnnotationsProperty { it.visibleAnnotations }
val MethodSignature.invisibleAnnotations by methodAnnotationsProperty { it.invisibleAnnotations }
//val MethodSignature.visibleTypeAnnotations by methodAnnotationsProperty { it.visibleTypeAnnotations }
//val MethodSignature.invisibleTypeAnnotations by methodAnnotationsProperty { it.invisibleTypeAnnotations }

val visibleParameterAnnotations = ParameterAnnotationsProperty(0)
val invisibleParameterAnnotations = ParameterAnnotationsProperty(1)
val parameterAnnotationsProperties = listOf(visibleParameterAnnotations, invisibleParameterAnnotations)

fun parseMethodSignature(methodNode: MethodNode): MethodSignature {
    val methodSignature = MethodSignature(methodNode.name + methodNode.desc)
    for (mp in methodProperties) {
        methodSignature.data[mp] = mp.parse(methodNode)
    }
    for (mp in methodAnnotationsProperties) {
        methodSignature.annotations[mp] = mp.parse(methodNode)
    }
    for (i in methodNode.visibleParameterAnnotations.orEmpty().indices) {
        val parameterSignature = ParameterSignature("p$i")
        methodSignature.parameters.add(parameterSignature)
        parameterSignature.annotations[visibleParameterAnnotations] =
                methodNode.visibleParameterAnnotations.orEmpty().getOrNull(i).toAnnotations()
        parameterSignature.annotations[invisibleParameterAnnotations] =
                methodNode.invisibleParameterAnnotations.orEmpty().getOrNull(i).toAnnotations()
    }
    return methodSignature
}