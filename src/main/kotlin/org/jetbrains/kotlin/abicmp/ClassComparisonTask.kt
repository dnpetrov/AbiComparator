@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.Opcodes

class ClassComparisonTask(
        private val cs1: ClassSignature,
        private val cs2: ClassSignature,
        private val report: Report
) : Runnable {
    private val classDiff = ClassDiff(cs1.className)

    val hasDiff get() = classDiff.isNotEmpty()

    override fun run() {
        classDiff.classInfo1 = cs1.flags.classFlags()
        classDiff.classInfo2 = cs2.flags.classFlags()

        compareClassProperties()
        compareClassAnnotations()
        compareMethods()
        compareFields()

        if (classDiff.isNotEmpty()) {
            report.reportClassDiff(classDiff)
        }
    }

    private fun compareClassProperties() {
        for (classProperty in classProperties) {
            val valueToHtml = classProperty.valueToHtml as (Any?) -> String
            val val1 = cs1.data[classProperty]
            val val2 = cs2.data[classProperty]
            if (val1 != val2) {
                val propertyDiff = PropertyDiff(classProperty.name, valueToHtml(val1), valueToHtml(val2))
                classDiff.propertyDiffs.add(propertyDiff)
            }
        }
    }

    private fun compareClassAnnotations() {
        for (classAnnotationsProperty in classAnnotationsProperties) {
            val anns1 = cs1.annotations[classAnnotationsProperty].orEmpty()
            val anns2 = cs2.annotations[classAnnotationsProperty].orEmpty()
            val diff = compareAnnotations(classAnnotationsProperty.name, anns1, anns2)
            if (diff != null) {
                classDiff.annotationsDiffs.add(diff)
            }
        }
    }

    private fun compareMethods() {
        val methods1 = cs1.methods.preprocessMethods()
        val methods2 = cs2.methods.preprocessMethods()
        var i1 = 0
        var i2 = 0
        val size1 = methods1.size
        val size2 = methods2.size
        var hasMethodsListDiff = false
        val methodsListDiff1 = ArrayList<String>()
        val methodsListDiff2 = ArrayList<String>()
        while (i1 < size1 || i2 < size2) {
            when {
                i1 < size1 && i2 < size2 -> {
                    val m1 = methods1[i1]
                    val m2 = methods2[i2]
                    when {
                        m1.id == m2.id -> {
                            compareMethods(m1, m2)
                            ++i1
                            ++i2
                        }
                        m1.id < m2.id -> {
                            hasMethodsListDiff = true
                            methodsListDiff1.add(m1.methodDiffLine())
                            methodsListDiff2.add("---")
                            ++i1
                        }
                        else -> {
                            hasMethodsListDiff = true
                            methodsListDiff1.add("---")
                            methodsListDiff2.add(m2.methodDiffLine())
                            ++i2
                        }
                    }
                }
                i1 < size1 -> {
                    val m1 = methods1[i1]
                    hasMethodsListDiff = true
                    methodsListDiff1.add(m1.methodDiffLine())
                    methodsListDiff2.add("---")
                    ++i1
                }
                else -> {
                    val m2 = methods2[i2]
                    hasMethodsListDiff = true
                    methodsListDiff1.add("---")
                    methodsListDiff2.add(m2.methodDiffLine())
                    ++i2
                }
            }
        }

        if (hasMethodsListDiff) {
            classDiff.structureDiffs.add(ListDiff("methods", methodsListDiff1, methodsListDiff2))
        }
    }

    private fun compareFields() {
        val fields1 = cs1.fields.preprocessFields()
        val fields2 = cs2.fields.preprocessFields()
        var i1 = 0
        var i2 = 0
        val size1 = fields1.size
        val size2 = fields2.size
        var hasFieldsListDiff = false
        val fieldsListDiff1 = ArrayList<String>()
        val fieldsListDiff2 = ArrayList<String>()
        while (i1 < size1 || i2 < size2) {
            when {
                i1 < size1 && i2 < size2 -> {
                    val f1 = fields1[i1]
                    val f2 = fields2[i2]
                    when {
                        f1.id == f2.id -> {
                            compareFields(f1, f2)
                            ++i1
                            ++i2
                        }
                        f1.id < f2.id -> {
                            hasFieldsListDiff = true
                            fieldsListDiff1.add(f1.fieldDiffLine())
                            fieldsListDiff2.add("---")
                            ++i1
                        }
                        else -> {
                            hasFieldsListDiff = true
                            fieldsListDiff1.add("---")
                            fieldsListDiff2.add(f2.fieldDiffLine())
                            ++i2
                        }
                    }
                }
                i1 < size1 -> {
                    val f1 = fields1[i1]
                    hasFieldsListDiff = true
                    fieldsListDiff1.add(f1.fieldDiffLine())
                    fieldsListDiff2.add("---")
                    ++i1
                }
                else -> {
                    val f2 = fields2[i2]
                    hasFieldsListDiff = true
                    fieldsListDiff1.add("---")
                    fieldsListDiff2.add(f2.fieldDiffLine())
                    ++i2
                }
            }
        }

        if (hasFieldsListDiff) {
            classDiff.structureDiffs.add(ListDiff("fields", fieldsListDiff1, fieldsListDiff2))
        }
    }

    private fun MethodSignature.methodDiffLine() =
            "$id ${flags.methodFlags()}"

    private fun FieldSignature.fieldDiffLine() =
            "$id ${flags.fieldFlags()}"

    private fun compareMethods(m1: MethodSignature, m2: MethodSignature) {
        val diff = compareMembers(
                m1, m1.flags.methodFlags(),
                m2, m2.flags.methodFlags(),
                methodProperties, methodAnnotationsProperties
        )

        for ((ps1, ps2) in m1.parameters.zip(m2.parameters)) {
            for (pap in parameterAnnotationsProperties) {
                val anns1 = ps1.annotations[pap].orEmpty()
                val anns2 = ps2.annotations[pap].orEmpty()
                diff.parameterAnnotationsDiff.add(compareAnnotations(pap.name, anns1, anns2))
            }
        }

        if (diff.isNotEmpty()) {
            classDiff.memberDiffs.add(diff)
        }
    }

    private fun compareFields(f1: FieldSignature, f2: FieldSignature) {
        val diff = compareMembers(
                f1, f1.flags.fieldFlags(),
                f2, f2.flags.fieldFlags(),
                fieldProperties, fieldAnnotationProperties
        )
        if (diff.isNotEmpty()) {
            classDiff.memberDiffs.add(diff)
        }
    }

    private fun compareMembers(
            e1: Entity,
            info1: String,
            e2: Entity,
            info2: String,
            properties: List<EntityProperty<*, *>>,
            annotationsProperties: List<AnnotationsProperty<*, *>>
    ) : MemberDiff {
        val propertyDiffs = ArrayList<PropertyDiff>()
        for (mp in properties) {
            val valueToHtml = mp.valueToHtml as (Any?) -> String
            val v1 = e1.data[mp]
            val v2 = e2.data[mp]
            if (v1 != v2) {
                propertyDiffs.add(PropertyDiff(mp.name, valueToHtml(v1), valueToHtml(v2)))
            }
        }

        val annotationsDiffs = ArrayList<ListDiff>()
        for (mp in annotationsProperties) {
            val anns1 = e1.annotations[mp].orEmpty()
            val anns2 = e2.annotations[mp].orEmpty()
            val annsDiff = compareAnnotations(mp.name, anns1, anns2)
            if (annsDiff != null) {
                annotationsDiffs.add(annsDiff)
            }
        }

        return MemberDiff(e1.id, info1, info2, propertyDiffs, annotationsDiffs)
    }

    private fun List<MethodSignature>.preprocessMethods() =
            filter {
                val flags = it.flags
                flags and Opcodes.ACC_SYNTHETIC == 0
            }.sortedBy { it.id }

    private fun List<FieldSignature>.preprocessFields() =
            filter {
                val flags = it.flags
                flags and Opcodes.ACC_SYNTHETIC == 0
            }.sortedBy { it.id }
}