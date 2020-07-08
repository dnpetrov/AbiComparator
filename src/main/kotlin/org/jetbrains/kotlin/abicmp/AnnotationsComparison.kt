package org.jetbrains.kotlin.abicmp

import org.objectweb.asm.Type

val IGNORED_ANNOTATIONS = listOf("Lkotlin/Metadata;", "Lkotlin/coroutines/jvm/internal/DebugMetadata;")

fun compareAnnotations(
        propertyName: String,
        annotations1: List<AnnotationEntry>,
        annotations2: List<AnnotationEntry>
) : ListDiff? {
    var hasDiff = false

    val anns1Sorted = annotations1.preprocessAnnotations()
    val anns2Sorted = annotations2.preprocessAnnotations()

    val diff1 = ArrayList<String>()
    val diff2 = ArrayList<String>()

    var i1 = 0
    var i2 = 0
    val size1 = anns1Sorted.size
    val size2 = anns2Sorted.size
    while (i1 < size1 || i2 < size2) {
        if (i1 < size1 && i2 < size2) {
            val ann1 = anns1Sorted[i1]
            val ann2 = anns2Sorted[i2]

            // TODO proper comparison for annotation argument values?
            if (ann1.fullString() == ann2.fullString()) {
                ++i1
                ++i2
                diff1.add("== ${ann1.shortString()}")
                diff2.add("== ${ann2.shortString()}")
            } else {
                hasDiff = true
                when {
                    ann1.desc == ann2.desc -> {
                        ++i1
                        ++i2
                        diff1.add("!= ${ann1.fullString()}")
                        diff2.add("!= ${ann2.fullString()}")
                    }
                    ann1.desc < ann2.desc -> {
                        ++i1
                        diff1.add("+ ${ann1.shortString()}")
                        diff2.add("---")
                    }
                    else -> {
                        ++i2
                        diff1.add("---")
                        diff2.add("+ ${ann2.shortString()}")
                    }
                }
            }
        } else {
            hasDiff = true
            if (i1 < size1) {
                val ann1 = anns1Sorted[i1]
                ++i1
                diff1.add("+ ${ann1.shortString()}")
                diff2.add("---")
            } else {
                val ann2 = anns2Sorted[i2]
                ++i2
                diff1.add("---")
                diff2.add("+ ${ann2.shortString()}")
            }
        }
    }

    return if (hasDiff) ListDiff(propertyName, diff1, diff2) else null
}

private fun List<AnnotationEntry>.preprocessAnnotations() =
        filter { it.desc !in IGNORED_ANNOTATIONS }.sortedBy { it.fullString() }

private fun AnnotationEntry.shortString(): String =
        if (values.isEmpty())
            "@$desc"
        else
            "@$desc(...)"

private fun AnnotationEntry.fullString(): String =
        if (values.isEmpty())
            "@$desc"
        else
            "@$desc( ${values.joinToString { it.toValueString() }} )"

private fun Pair<String, Any?>.toValueString(): String =
        "$first: ${second.toValueString()}"

private fun Any?.toValueString(): String =
        when (this) {
            null -> "NULL"
            is Type -> "<$descriptor>"
            is List<*> -> joinToString(separator = ", ", prefix = "#{ ", postfix = " }") { it.toValueString() }
            is Array<*> -> toList().toValueString()
            else -> toString()
        }