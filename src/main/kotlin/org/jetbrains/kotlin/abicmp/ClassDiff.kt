package org.jetbrains.kotlin.abicmp

class PropertyDiff(
        val property: String,
        val value1: String,
        val value2: String,
        val details: String = "..."
)

class AnnotationsDiff(
        val property: String,
        val diff1: List<String>,
        val diff2: List<String>
)

class MethodsListDiff(
        val diff1: List<String>,
        val diff2: List<String>
)

class ClassDiff(val name: String) {
    var classInfo1 = ""
    var classInfo2 = ""
    val propertyDiffs = ArrayList<PropertyDiff>()
    val annotationsDiffs = ArrayList<AnnotationsDiff>()
    var methodsListDiff: MethodsListDiff? = null

    fun isNotEmpty() =
            propertyDiffs.isNotEmpty() ||
                    annotationsDiffs.isNotEmpty() ||
                    methodsListDiff != null
}

