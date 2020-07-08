@file:Suppress("unused", "UNCHECKED_CAST")
package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.InputStream
import kotlin.reflect.KProperty

class ClassSignature(id: String) : Entity(id) {
    val methods = ArrayList<MethodSignature>()
    val fields = ArrayList<FieldSignature>()
}

class ClassProperty<T>(index: Int, parse: (ClassNode) -> T) :
        EntityProperty<ClassNode, T>(index, parse)

class ClassAnnotationsProperty(index: Int, getAnnotations: (ClassNode) -> List<Any?>?) :
        AnnotationsProperty<ClassNode, ClassSignature>(index, getAnnotations)

operator fun <T> ClassProperty<T>.provideDelegate(x: Nothing?, kProperty: KProperty<*>): ClassProperty<T> =
        apply { name = kProperty.name }

operator fun <T> ClassProperty<T>.getValue(x: ClassSignature, kp: Any?) =
        x.data[this] as T

val classProperties = ArrayList<ClassProperty<*>>()

val classAnnotationsProperties = ArrayList<ClassAnnotationsProperty>()

fun <T> classProperty(parser: (ClassNode) -> T): ClassProperty<T> =
        ClassProperty(classProperties.size, parser).also {
            classProperties.add(it)
        }

fun classAnnotationsProperty(parser: (ClassNode) -> List<Any?>?): ClassAnnotationsProperty =
        ClassAnnotationsProperty(classAnnotationsProperties.size, parser).also {
            classAnnotationsProperties.add(it)
        }

val ClassSignature.className by classProperty { it.name }
val ClassSignature.flags by classProperty { it.access }
val ClassSignature.flagsBits by classProperty { it.access.toString(2) }
val ClassSignature.flagsList by classProperty { it.access.classFlags() }
val ClassSignature.signature by classProperty { it.signature }
val ClassSignature.superClassName by classProperty { it.superName }
val ClassSignature.superInterfaceNames by classProperty { it.interfaces.map { itf -> itf as String } }
val ClassSignature.sourceFile by classProperty { it.sourceFile }
//val ClassSignature.sourceDebug by classProperty { it.sourceDebug }
val ClassSignature.outerClass by classProperty { it.outerClass }
val ClassSignature.outerMethod by classProperty { it.outerMethod }
val ClassSignature.outerMethodDesc by classProperty { it.outerMethodDesc }

// TODO inner classes?
//val ClassSignature.innerClasses by classProperty { ... }

val ClassSignature.visibleAnnotations by classAnnotationsProperty { it.visibleAnnotations }
val ClassSignature.invisibleAnnotations by classAnnotationsProperty { it.invisibleAnnotations }
//val ClassSignature.visibleTypeAnnotations by classAnnotationsProperty { it.visibleTypeAnnotations }
//val ClassSignature.invisibleTypeAnnotations by classAnnotationsProperty { it.invisibleTypeAnnotations }

fun parseClassNode(input: InputStream): ClassNode =
    ClassNode().also { ClassReader(input).accept(it, ClassReader.SKIP_CODE) }

fun parseClassSignature(id: String, classNode: ClassNode): ClassSignature {
    val classSignature = ClassSignature(id)
    for (cp in classProperties) {
        classSignature.data[cp] = cp.parse(classNode)
    }
    for (cap in classAnnotationsProperties) {
        classSignature.annotations[cap] = cap.parse(classNode)
    }
    classNode.methods.mapTo(classSignature.methods) { parseMethodSignature(it as MethodNode) }
    classNode.fields.mapTo(classSignature.fields) { parseFieldSignature(it as FieldNode) }
    return classSignature
}

