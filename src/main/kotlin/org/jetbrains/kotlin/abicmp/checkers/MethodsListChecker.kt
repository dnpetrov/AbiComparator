package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.tasks.fieldId
import org.jetbrains.kotlin.abicmp.tasks.methodId
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode

class InnerClassesListChecker : ClassChecker {
    override val name = "class.innerClasses"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val innerClasses1 = class1.innerClasses.listOfNotNull<InnerClassNode>()
                .filter { it.innerName != null && it.innerName != "WhenMappings" }
                .associateBy { it.name }
        val innerClasses2 = class2.innerClasses.listOfNotNull<InnerClassNode>()
                .filter { it.innerName != null && it.innerName != "WhenMappings" }
                .associateBy { it.name }

        val relevantInnerClassNames =
                innerClasses1.keys.union(innerClasses2.keys).filter {
                    val ic1 = innerClasses1[it]
                    val ic2 = innerClasses2[it]
                    ic1 != null && !ic1.access.isSynthetic() ||
                            ic2 != null && ic2.access.isSynthetic()
                }
        val innerClassNames1 = innerClasses1.keys.filter { it in relevantInnerClassNames }.sorted()
        val innerClassNames2 = innerClasses2.keys.filter { it in relevantInnerClassNames }.sorted()

        val listDiff = compareLists(innerClassNames1, innerClassNames2) ?: return

        report.addInnerClassesDiffs(
                listDiff.diff1.map {
                    innerClasses1[it]?.toInnerClassLine() ?: "---"
                },
                listDiff.diff2.map {
                    innerClasses2[it]?.toInnerClassLine() ?: "---"
                }
        )
    }

    private fun InnerClassNode.toInnerClassLine(): String =
            "INNER_CLASS $name $outerName $innerName ${access.toString(2)} ${access.classFlags()}"
}

class MethodsListChecker : ClassChecker {
    override val name = "class.methods"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val methods1 = class1.methods.listOfNotNull<MethodNode>().associateBy { it.methodId() }
        val methods2 = class2.methods.listOfNotNull<MethodNode>().associateBy { it.methodId() }

        val relevantMethodIds = methods1.keys.union(methods2.keys)
                .filter {
                    val method1 = methods1[it]
                    val method2 = methods2[it]
                    (method1 != null && !method1.access.isSynthetic() ||
                            method2 != null && !method2.access.isSynthetic())
                }.toSet()

        val methodIds1 = methods1.keys.intersect(relevantMethodIds).sorted()

        val methodIds2 = methods2.keys.intersect(relevantMethodIds).sorted()

        val listDiff = compareLists(methodIds1, methodIds2) ?: return
        report.addMethodListDiffs(
                listDiff.diff1.map { it.toMethodWithFlags(methods1) },
                listDiff.diff2.map { it.toMethodWithFlags(methods2) }
        )
    }

    private fun String.toMethodWithFlags(methods: Map<String, MethodNode>): String {
        val method = methods[this] ?: return this
        return "$this ${method.access.methodFlags()}"
    }
}

class FieldsListChecker : ClassChecker {
    override val name = "class.fields"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val fields1 = class1.fields.listOfNotNull<FieldNode>().associateBy { it.fieldId() }
        val fields2 = class2.fields.listOfNotNull<FieldNode>().associateBy { it.fieldId() }

        val relevantFieldIds = fields1.keys.union(fields2.keys)
                .filter {
                    val field1 = fields1[it]
                    val field2 = fields2[it]
                    !(field1 != null && !field1.access.isSynthetic() ||
                            field2 != null && !field2.access.isSynthetic())
                }.toSet()

        val fieldIds1 = fields1.keys.intersect(relevantFieldIds).sorted()

        val fieldIds2 = fields2.keys.intersect(relevantFieldIds).sorted()

        val listDiff = compareLists(fieldIds1, fieldIds2) ?: return
        report.addFieldListDiffs(
                listDiff.diff1.map { it.toFieldWithFlags(fields1) },
                listDiff.diff2.map { it.toFieldWithFlags(fields2) }
        )
    }

    private fun String.toFieldWithFlags(fields: Map<String, FieldNode>): String {
        val field = fields[this] ?: return this
        return "$this ${field.access.fieldFlags()}"
    }
}