package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.classFlags
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode

class InnerClassesListChecker : ClassChecker {
    override val name = "class.innerClasses"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val innerClasses1 = class1.loadInnerClasses()
        val innerClasses2 = class2.loadInnerClasses()

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

    private fun ClassNode.loadInnerClasses(): Map<String, InnerClassNode> =
        innerClasses.listOfNotNull<InnerClassNode>()
            .filterNot {
                it.innerName == null || it.innerName == "WhenMappings" || isSamAdapterName(it.name)
            }
            .associateBy { it.name }


    private fun InnerClassNode.toInnerClassLine(): String =
            "INNER_CLASS $name $outerName $innerName ${access.toString(2)} ${access.classFlags()}"
}

fun isSamAdapterName(name: String): Boolean =
    "\$sam$" in name && name.endsWith("$0")
