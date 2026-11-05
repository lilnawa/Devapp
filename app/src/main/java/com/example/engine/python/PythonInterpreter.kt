package com.example.engine.python

import java.io.StringWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionResult(
    val output: String,
    val errors: String = "",
    val exitCode: Int = 0,
    val executionTimeMs: Long = 0L
)

class PythonInterpreter {

    suspend fun execute(script: String): ExecutionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        try {
            val lines = script.lines()
            val variables = mutableMapOf<String, Any>()
            val functions = mutableMapOf<String, List<String>>() // function name -> body lines

            // Built-in standard library simulated values
            variables["__name__"] = "__main__"
            variables["pi"] = Math.PI
            variables["e"] = Math.E

            var i = 0
            while (i < lines.size) {
                val rawLine = lines[i]
                val trimmed = rawLine.trim()

                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    i++
                    continue
                }

                // Handle imports (simulated)
                if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                    stdout.appendLine("[DevHub Python] Loaded module: ${trimmed.removePrefix("import ").removePrefix("from ").substringBefore(" ")}")
                    i++
                    continue
                }

                // Function definitions: def func(...):
                if (trimmed.startsWith("def ")) {
                    val funcHeader = trimmed.removePrefix("def ").substringBefore(":")
                    val funcName = funcHeader.substringBefore("(").trim()
                    val body = mutableListOf<String>()
                    i++
                    while (i < lines.size && (lines[i].startsWith("    ") || lines[i].startsWith("\t") || lines[i].isBlank())) {
                        if (lines[i].isNotBlank()) {
                            body.add(lines[i].trim())
                        }
                        i++
                    }
                    functions[funcName] = body
                    continue
                }

                // If statements
                if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                    val condition = trimmed.removePrefix("if ").removeSuffix(":")
                    val condResult = evaluateCondition(condition, variables)
                    val ifBody = mutableListOf<String>()
                    val elseBody = mutableListOf<String>()
                    i++
                    var collectingElse = false
                    while (i < lines.size && (lines[i].startsWith("    ") || lines[i].startsWith("\t") || lines[i].trim().startsWith("else:") || lines[i].isBlank())) {
                        val currentTrimmed = lines[i].trim()
                        if (currentTrimmed == "else:") {
                            collectingElse = true
                        } else if (currentTrimmed.isNotBlank()) {
                            if (collectingElse) elseBody.add(currentTrimmed) else ifBody.add(currentTrimmed)
                        }
                        i++
                    }

                    val blockToRun = if (condResult) ifBody else elseBody
                    for (line in blockToRun) {
                        executeSingleStatement(line, variables, stdout, stderr, i)
                    }
                    continue
                }

                // For loop: for i in range(n):
                if (trimmed.startsWith("for ") && trimmed.contains(" in range(") && trimmed.endsWith(":")) {
                    val varName = trimmed.substringAfter("for ").substringBefore(" in ").trim()
                    val rangeArgs = trimmed.substringAfter("range(").substringBefore("):").split(",").map { it.trim() }
                    val range: IntRange = if (rangeArgs.size == 1) {
                        val max = (evaluateExpression(rangeArgs[0], variables) as? Number)?.toInt() ?: 0
                        0 until max
                    } else {
                        val start = (evaluateExpression(rangeArgs[0], variables) as? Number)?.toInt() ?: 0
                        val end = (evaluateExpression(rangeArgs[1], variables) as? Number)?.toInt() ?: 0
                        start until end
                    }

                    val loopBody = mutableListOf<String>()
                    i++
                    while (i < lines.size && (lines[i].startsWith("    ") || lines[i].startsWith("\t") || lines[i].isBlank())) {
                        if (lines[i].isNotBlank()) loopBody.add(lines[i].trim())
                        i++
                    }

                    for (step in range) {
                        variables[varName] = step
                        for (statement in loopBody) {
                            executeSingleStatement(statement, variables, stdout, stderr, i)
                        }
                    }
                    continue
                }

                // Execute normal statement
                executeSingleStatement(trimmed, variables, stdout, stderr, i + 1)
                i++
            }
        } catch (e: Exception) {
            stderr.appendLine("Traceback (most recent call last):")
            stderr.appendLine("  RuntimeError: ${e.message}")
        }

        val elapsed = System.currentTimeMillis() - startTime
        ExecutionResult(
            output = stdout.toString().ifEmpty { "Script executed without stdout output." },
            errors = stderr.toString(),
            exitCode = if (stderr.isEmpty()) 0 else 1,
            executionTimeMs = elapsed
        )
    }

    private fun executeSingleStatement(
        stmt: String,
        variables: MutableMap<String, Any>,
        stdout: StringBuilder,
        stderr: StringBuilder,
        lineNumber: Int
    ) {
        val trimmed = stmt.trim()

        // print(...)
        if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
            val content = trimmed.removePrefix("print(").removeSuffix(")")
            val evaluated = formatPrintContent(content, variables)
            stdout.appendLine(evaluated)
            return
        }

        // Variable assignment: x = ...
        if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("!=") && !trimmed.contains("<=") && !trimmed.contains(">=")) {
            val parts = trimmed.split("=", limit = 2)
            val varName = parts[0].trim()
            val expr = parts[1].trim()
            val value = evaluateExpression(expr, variables)
            variables[varName] = value
            return
        }

        // Method call: list.append(x)
        if (trimmed.contains(".append(")) {
            val listName = trimmed.substringBefore(".append(").trim()
            val itemExpr = trimmed.substringAfter(".append(").removeSuffix(")").trim()
            val list = variables[listName] as? MutableList<Any>
            if (list != null) {
                list.add(evaluateExpression(itemExpr, variables))
            }
            return
        }
    }

    private fun formatPrintContent(raw: String, variables: Map<String, Any>): String {
        // Handle f"..." strings
        if (raw.startsWith("f\"") && raw.endsWith("\"") || raw.startsWith("f'") && raw.endsWith("'")) {
            var text = raw.substring(2, raw.length - 1)
            val regex = Regex("\\{([^}]+)\\}")
            return regex.replace(text) { match ->
                val expr = match.groupValues[1].trim()
                evaluateExpression(expr, variables).toString()
            }
        }

        // String literal "..."
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length - 1)
        }

        // Comma separated prints
        val parts = splitByTopLevelComma(raw)
        return parts.joinToString(" ") { part ->
            evaluateExpression(part.trim(), variables).toString()
        }
    }

    private fun splitByTopLevelComma(input: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        var inQuotes = false

        for (ch in input) {
            when {
                ch == '"' || ch == '\'' -> inQuotes = !inQuotes
                !inQuotes && (ch == '(' || ch == '[' || ch == '{') -> depth++
                !inQuotes && (ch == ')' || ch == ']' || ch == '}') -> depth--
                !inQuotes && depth == 0 && ch == ',' -> {
                    result.add(current.toString())
                    current = StringBuilder()
                    continue
                }
            }
            current.append(ch)
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun evaluateCondition(cond: String, variables: Map<String, Any>): Boolean {
        val trimmed = cond.trim()
        return when {
            trimmed.contains("==") -> {
                val p = trimmed.split("==")
                evaluateExpression(p[0].trim(), variables) == evaluateExpression(p[1].trim(), variables)
            }
            trimmed.contains("!=") -> {
                val p = trimmed.split("!=")
                evaluateExpression(p[0].trim(), variables) != evaluateExpression(p[1].trim(), variables)
            }
            trimmed.contains(">=") -> {
                val p = trimmed.split(">=")
                val a = (evaluateExpression(p[0].trim(), variables) as? Number)?.toDouble() ?: 0.0
                val b = (evaluateExpression(p[1].trim(), variables) as? Number)?.toDouble() ?: 0.0
                a >= b
            }
            trimmed.contains("<=") -> {
                val p = trimmed.split("<=")
                val a = (evaluateExpression(p[0].trim(), variables) as? Number)?.toDouble() ?: 0.0
                val b = (evaluateExpression(p[1].trim(), variables) as? Number)?.toDouble() ?: 0.0
                a <= b
            }
            trimmed.contains(">") -> {
                val p = trimmed.split(">")
                val a = (evaluateExpression(p[0].trim(), variables) as? Number)?.toDouble() ?: 0.0
                val b = (evaluateExpression(p[1].trim(), variables) as? Number)?.toDouble() ?: 0.0
                a > b
            }
            trimmed.contains("<") -> {
                val p = trimmed.split("<")
                val a = (evaluateExpression(p[0].trim(), variables) as? Number)?.toDouble() ?: 0.0
                val b = (evaluateExpression(p[1].trim(), variables) as? Number)?.toDouble() ?: 0.0
                a < b
            }
            trimmed == "True" -> true
            trimmed == "False" -> false
            else -> variables[trimmed] != null && variables[trimmed] != 0 && variables[trimmed] != false
        }
    }

    private fun evaluateExpression(expr: String, variables: Map<String, Any>): Any {
        val trimmed = expr.trim()

        if (trimmed.isEmpty()) return ""

        // String literals
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // List literal: [...]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return mutableListOf<Any>()
            val items = splitByTopLevelComma(inner).map { evaluateExpression(it.trim(), variables) }
            return items.toMutableList()
        }

        // Number literals
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toLongOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // Booleans
        if (trimmed.equals("true", ignoreCase = true)) return true
        if (trimmed.equals("false", ignoreCase = true)) return false

        // Builtin functions: len(...)
        if (trimmed.startsWith("len(") && trimmed.endsWith(")")) {
            val arg = evaluateExpression(trimmed.removePrefix("len(").removeSuffix(")").trim(), variables)
            return when (arg) {
                is List<*> -> arg.size
                is String -> arg.length
                else -> 0
            }
        }

        // Builtin functions: sum(...)
        if (trimmed.startsWith("sum(") && trimmed.endsWith(")")) {
            val arg = evaluateExpression(trimmed.removePrefix("sum(").removeSuffix(")").trim(), variables)
            return if (arg is List<*>) {
                arg.sumOf { (it as? Number)?.toDouble() ?: 0.0 }
            } else 0
        }

        // Multiplication / arithmetic: e.g. "=" * 45
        if (trimmed.contains("*") && !trimmed.startsWith("*")) {
            val parts = trimmed.split("*", limit = 2)
            val left = evaluateExpression(parts[0].trim(), variables)
            val right = evaluateExpression(parts[1].trim(), variables)
            if (left is String && right is Number) {
                return left.repeat(right.toInt())
            }
            if (left is Number && right is Number) {
                return left.toDouble() * right.toDouble()
            }
        }

        // Addition: e.g. a + b
        if (trimmed.contains("+")) {
            val parts = trimmed.split("+", limit = 2)
            val left = evaluateExpression(parts[0].trim(), variables)
            val right = evaluateExpression(parts[1].trim(), variables)
            if (left is Number && right is Number) {
                return left.toDouble() + right.toDouble()
            }
            return "$left$right"
        }

        // Variable lookup
        if (variables.containsKey(trimmed)) {
            return variables[trimmed] ?: ""
        }

        return trimmed
    }
}
