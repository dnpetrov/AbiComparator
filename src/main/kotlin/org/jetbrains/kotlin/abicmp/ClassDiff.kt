package org.jetbrains.kotlin.abicmp

class PropertyDiff(
        val property: String,
        val value1: String,
        val value2: String
)

class ListDiff(
        val property: String,
        val diff1: List<String>,
        val diff2: List<String>
)

class MemberDiff(
        val id: String,
        val info1: String,
        val info2: String,
        val propertyDiffs: List<PropertyDiff>,
        val annotationsDiff: List<ListDiff>
) {
    val parameterAnnotationsDiff = ArrayList<ListDiff?>()

    fun isNotEmpty() =
            propertyDiffs.isNotEmpty() ||
                    annotationsDiff.isNotEmpty() ||
                    parameterAnnotationsDiff.any { it != null }
}

class ClassDiff(val name: String) {
    var classInfo1 = ""
    var classInfo2 = ""

    val propertyDiffs = ArrayList<PropertyDiff>()
    val annotationsDiffs = ArrayList<ListDiff>()
    val structureDiffs = ArrayList<ListDiff>()
    val memberDiffs = ArrayList<MemberDiff>()

    fun isNotEmpty() =
            propertyDiffs.isNotEmpty() ||
                    annotationsDiffs.isNotEmpty() ||
                    structureDiffs.isNotEmpty() ||
                    memberDiffs.isNotEmpty()
}

