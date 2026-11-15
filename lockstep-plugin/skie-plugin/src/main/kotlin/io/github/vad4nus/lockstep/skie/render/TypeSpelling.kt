package io.github.vad4nus.lockstep.skie.render

private val qualifier = Regex("([A-Za-z_][A-Za-z0-9_]*)\\.")

private val identifier = Regex("[A-Za-z_][A-Za-z0-9_.]*")

internal fun referenceEnums(type: String, enums: Set<String>): String {
    if (enums.isEmpty()) return type
    val result = StringBuilder()
    val stack = ArrayDeque<Boolean>()
    var index = 0
    while (index < type.length) {
        val match = identifier.matchAt(type, index)
        if (match == null) {
            val char = type[index]
            when (char) {
                '[' -> stack.addLast(false)
                ']', '>' -> stack.removeLastOrNull()
            }
            result.append(char)
            index++
            continue
        }
        val token = match.value
        val next = match.range.last + 1
        if (next < type.length && type[next] == '<') {
            stack.addLast(!token.startsWith("Swift."))
            result.append(token).append('<')
            index = next + 1
            continue
        }
        val simple = token.substringAfterLast('.')
        if (simple in enums && stack.any { it }) {
            result.append(token.removeSuffix(simple)).append("__").append(simple)
        } else {
            result.append(token)
        }
        index = next
    }
    return result.toString()
}

internal fun importsFor(types: List<String>, modules: Set<String>): List<String> =
    types.flatMap { type -> qualifier.findAll(type).map { it.groupValues[1] }.toList() }
        .filter { it in modules && it != "Swift" && it != "Foundation" }
        .distinct()
        .sorted()
