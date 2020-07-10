package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.ListDiff

const val MISSING = "---"

fun compareLists(list1: List<String>, list2: List<String>): ListDiff? {
    var hasDiff = false
    val diffs1 = ArrayList<String>()
    val diffs2 = ArrayList<String>()
    var i1 = 0
    var i2 = 0
    while (i1 < list1.size || i2 < list2.size) {
        val s1 = list1.getOrNull(i1)
        val s2 = list2.getOrNull(i2)

        if (s1 == s2) {
            ++i1
            ++i2
            continue
        }

        hasDiff = true

        when {
            s1 == null && s2 == null ->
                break // really should not happen
            s1 == null -> {
                diffs1.add(MISSING)
                diffs2.add(s2!!)
                ++i2
            }
            s2 == null -> {
                diffs1.add(s1)
                diffs2.add(MISSING)
                ++i1
            }
            s1 < s2 -> {
                diffs1.add(s1)
                diffs2.add(MISSING)
                ++i1
            }
            s1 > s2 -> {
                diffs1.add(MISSING)
                diffs2.add(s2)
                ++i2
            }
        }
    }

    return if (hasDiff) ListDiff(diffs1, diffs2) else null
}