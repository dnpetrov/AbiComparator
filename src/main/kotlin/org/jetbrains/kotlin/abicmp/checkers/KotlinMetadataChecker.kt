package org.jetbrains.kotlin.abicmp.checkers

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Chunk
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch
import org.jetbrains.kotlin.abicmp.escapeHtml
import org.jetbrains.kotlin.abicmp.metadata.renderKotlinMetadata
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.objectweb.asm.tree.ClassNode

class KotlinMetadataChecker : ClassChecker {
    override val name = "class.metadata"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val metadata1 = class1.renderKotlinMetadata() ?: ""
        val metadata2 = class2.renderKotlinMetadata() ?: ""
        if (metadata1 == metadata2) return

        val patch12 = DiffUtils.diff(metadata1, metadata2, null)

        val patch21 = DiffUtils.diff(metadata2, metadata1, null)

        report.addPropertyDiff(NamedDiffEntry("kotlin.Metadata", patch12.patchToHtml(), patch21.patchToHtml()))
    }

    private fun Patch<String>.patchToHtml(): String =
            deltas.filter {
                it.type == DeltaType.CHANGE || it.type == DeltaType.DELETE
            }.joinToString(separator = "<hr>") { delta ->
                when (delta.type) {
                    DeltaType.CHANGE -> delta.source.chunkToHtml("*")
                    DeltaType.DELETE -> delta.source.chunkToHtml("-")
                    else -> ""
                }
            }

    private fun Chunk<String>.chunkToHtml(prefix: String) = buildString {
        appendLine("@ $position:<br>")
        for (line in lines) {
            appendLine("$prefix ${line.escapeHtml()}<br>")
        }
    }
}