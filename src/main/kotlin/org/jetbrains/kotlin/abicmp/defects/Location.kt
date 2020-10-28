package org.jetbrains.kotlin.abicmp.defects

sealed class Location : Comparable<Location> {
    abstract val jarFileName: String
    open val className: String? get() = null
    open val methodName: String? get() = null
    open val fieldName: String? get() = null

    enum class Kind {
        JAR_FILE, CLASS, METHOD, FIELD
    }

    abstract val kind: Kind

    abstract fun reportString(): String

    override fun compareTo(other: Location): Int =
            compareValuesBy(
                    this, other,
                    { it.kind },
                    { it.jarFileName },
                    { it.className },
                    { it.methodName },
                    { it.fieldName }
            )

    data class JarFile(
            override val jarFileName: String
    ) : Location() {
        override val kind get() = Kind.JAR_FILE
        override fun reportString(): String = "Jar file $jarFileName"
    }

    data class Class(
            override val jarFileName: String,
            override val className: String
    ) : Location() {
        override val kind get() = Kind.CLASS
        override fun reportString(): String = "Class $className in jar file $jarFileName"
        fun method(methodName: String) = Method(jarFileName, className, methodName)
        fun field(fieldName: String) = Field(jarFileName, className, fieldName)
    }

    data class Method(
            override val jarFileName: String,
            override val className: String,
            override val methodName: String
    ) : Location() {
        override val kind get() = Kind.METHOD
        override fun reportString(): String = "Method $methodName in class $className, jar file $jarFileName"
    }

    data class Field(
            override val jarFileName: String,
            override val className: String,
            override val fieldName: String
    ) : Location() {
        override val kind get() = Kind.FIELD
        override fun reportString(): String = "Field $fieldName in class $className, jar file $jarFileName"
    }
}
