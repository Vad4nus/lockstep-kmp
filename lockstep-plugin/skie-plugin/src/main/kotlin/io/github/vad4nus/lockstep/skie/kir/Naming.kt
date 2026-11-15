package io.github.vad4nus.lockstep.skie.kir

internal fun lowerCamel(name: String): String {
    val upperRun = name.takeWhile { it.isUpperCase() }.length
    return when {
        upperRun == 0 -> name
        upperRun == 1 -> name.replaceFirstChar { it.lowercaseChar() }
        upperRun == name.length -> name.lowercase()
        else -> name.substring(0, upperRun - 1).lowercase() + name.substring(upperRun - 1)
    }
}

internal fun caseName(kotlinFqName: String): String = lowerCamel(kotlinFqName.substringAfterLast('.'))

internal fun generatedSwiftName(swiftName: String): String = swiftName.replace(".", "")

internal fun kotlinSwiftName(swiftName: String, replaced: List<Boolean>): String {
    val parts = swiftName.split('.')
    if (parts.size != replaced.size) return if (replaced.last()) "__$swiftName" else swiftName
    return parts.zip(replaced).joinToString(".") { (part, flag) -> if (flag) "__$part" else part }
}

internal fun duplicates(names: List<String>): Set<String> =
    names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
