package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.checkers.ClassAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.classPropertyChecker
import org.jetbrains.kotlin.abicmp.checkers.compareLists
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode

class ClassTask(
        private val class1: ClassNode,
        private val class2: ClassNode,
        private val report: ClassReport
) : Runnable {

    private val classCheckers = listOf(
            classPropertyChecker(ClassNode::version),
            classPropertyChecker(ClassNode::access) { v -> "${v.toString(2)} ${v.classFlags()}"},
            classPropertyChecker("internalName", ClassNode::name),
            classPropertyChecker(ClassNode::signature),
            classPropertyChecker("superClassInternalName", ClassNode::superName),
            classPropertyChecker("superInterfaces") { it.interfaces.cast<List<String>>().sorted() },
            classPropertyChecker(ClassNode::sourceFile),
            classPropertyChecker(ClassNode::outerClass),
            classPropertyChecker(ClassNode::outerMethod),
            classPropertyChecker(ClassNode::outerMethodDesc),
            ClassAnnotationsChecker(ClassNode::visibleAnnotations),
            ClassAnnotationsChecker(ClassNode::invisibleAnnotations)
    )

    private val methods1 = class1.methods.notNullList<MethodNode>().associateBy { it.methodId() }
    private val methods2 = class2.methods.notNullList<MethodNode>().associateBy { it.methodId() }

    private val fields1 = class1.fields.notNullList<FieldNode>().associateBy { it.fieldId() }
    private val fields2 = class2.fields.notNullList<FieldNode>().associateBy { it.fieldId() }

    override fun run() {
        addClassInfo()

        for (checker in classCheckers) {
            checker.check(class1, class2, report)
        }

        checkInnerClasses()

        checkMethodsList()
        checkMethods()

        checkFieldsList()
        checkFields()
    }

    private fun addClassInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${class1.access.classFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${class2.access.classFlags()}")
            }
        }
    }

    private fun checkInnerClasses() {
        val innerClasses1 = class1.innerClasses.notNullList<InnerClassNode>()
                .filter { it.innerName != null && it.innerName != "WhenMappings" }
                .associateBy { it.name }
        val innerClasses2 = class2.innerClasses.notNullList<InnerClassNode>()
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

    private fun checkMethodsList() {
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

    private fun checkMethods() {
        val commonIds = methods1.keys.intersect(methods2.keys).sorted()
        for (id in commonIds) {
            val method1 = methods1[id]!!
            val method2 = methods2[id]!!
            if (method1.access.isSynthetic() && method2.access.isSynthetic()) continue
            val methodReport = report.methodReport(id)
            MethodTask(method1, method2, methodReport).run()
        }
    }

    private fun checkFieldsList() {
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

    private fun checkFields() {
        val commonIds = fields1.keys.intersect(fields2.keys).sorted()
        for (id in commonIds) {
            val field1 = fields1[id]!!
            val field2 = fields2[id]!!
            if (field1.access.isSynthetic() && field2.access.isSynthetic()) continue
            val fieldReport = report.fieldReport(id)
            FieldTask(field1, field2, fieldReport).run()
        }
    }

    private fun MethodNode.methodId() = "$name$desc"

    private fun FieldNode.fieldId() = "$name:$desc"
}

