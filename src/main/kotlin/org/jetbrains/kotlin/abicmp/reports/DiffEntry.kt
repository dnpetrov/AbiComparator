package org.jetbrains.kotlin.abicmp.reports

class DiffEntry(val value1: String, val value2: String)

class NamedDiffEntry(val name: String, val value1: String, val value2: String)

class ListDiff(val diff1: List<String>, val diff2: List<String>)

class TextDiffEntry(val pos1: Int, val lines1: List<String>, val pos2: Int, val lines2: List<String>)