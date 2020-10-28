package org.jetbrains.kotlin.abicmp.defects

class DefectAttribute(val id: String, val htmlId: String)

class DefectType(
        val id: String,
        val messageFormat: String,
        vararg val requiredAttributes: DefectAttribute
) : Comparable<DefectType> {
    override fun compareTo(other: DefectType): Int =
            id.compareTo(other.id)
}

class DefectInfo(
        val type: DefectType,
        val attributes: Map<DefectAttribute, String>
) : Comparable<DefectInfo> {
    init {
        for (requiredAttribute in type.requiredAttributes) {
            if (requiredAttribute !in attributes) {
                throw IllegalArgumentException(
                        "Missing required attribute ${requiredAttribute.id} for defect type ${type.id}"
                )
            }
        }
    }

    val message = formatMessage()

    operator fun get(key: DefectAttribute) = attributes[key]

    override fun compareTo(other: DefectInfo): Int =
            when (val typeCmp = type.compareTo(other.type)) {
                0 -> compareAttributes(other)
                else -> typeCmp
            }

    private fun compareAttributes(other: DefectInfo): Int {
        for ((key, value) in attributes) {
            val otherValue = other[key] ?: return 1
            when (val valueCmp = value.compareTo(otherValue)) {
                0 -> continue
                else -> return valueCmp
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean =
            other is DefectInfo && compareTo(other) == 0

    override fun hashCode(): Int =
            message.hashCode()
}

fun DefectInfo.formatMessage(): String {
    var message = type.messageFormat
    for ((attr, value) in attributes) {
        message = message.replace("[${attr.id}]", value, ignoreCase = false)
    }
    val result = StringBuilder()
    result.append("[")
    result.append(type.id)
    result.append("] ")
    result.append(message)
    return result.toString()
}
