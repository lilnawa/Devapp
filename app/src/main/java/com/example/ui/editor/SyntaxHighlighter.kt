package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag
import com.example.ui.theme.SyntaxType
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KEYWORDS_KOTLIN_JAVA = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
        "companion", "const", "constructor", "continue", "data", "do", "else", "enum",
        "expect", "false", "final", "finally", "for", "fun", "get", "if", "import",
        "in", "infix", "init", "inline", "inner", "interface", "internal", "is",
        "lateinit", "noinline", "null", "object", "open", "operator", "out", "override",
        "package", "private", "protected", "public", "reified", "return", "sealed",
        "set", "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias",
        "val", "var", "vararg", "when", "while", "void", "static", "new", "switch", "case", "default"
    )

    private val KEYWORDS_PYTHON = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
        "raise", "return", "True", "try", "while", "with", "yield", "print", "len", "range"
    )

    private val KEYWORDS_JS = setOf(
        "async", "await", "break", "case", "catch", "class", "const", "continue", "debugger",
        "default", "delete", "do", "else", "export", "extends", "finally", "for", "function",
        "if", "import", "in", "instanceof", "let", "new", "null", "return", "super", "switch",
        "this", "throw", "true", "false", "try", "typeof", "var", "void", "while", "with", "yield",
        "document", "window", "console", "require", "undefined"
    )

    private val KEYWORDS_SQL = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "JOIN", "LEFT",
        "RIGHT", "INNER", "OUTER", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT",
        "CREATE", "TABLE", "DROP", "ALTER", "INDEX", "PRIMARY", "KEY", "NOT", "NULL",
        "AND", "OR", "AS", "DISTINCT", "UNION", "ALL", "EXISTS", "BETWEEN", "LIKE", "IN"
    )

    private val KEYWORDS_C_CPP = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
        "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
        "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
        "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "include", "define"
    )

    fun detectLanguage(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "HTML"
            "css" -> "CSS"
            "js", "ts", "jsx", "tsx" -> "JavaScript"
            "py" -> "Python"
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "php" -> "PHP"
            "c", "h" -> "C"
            "cpp", "hpp", "cc" -> "C++"
            "json" -> "JSON"
            "xml" -> "XML"
            "sql" -> "SQL"
            "md", "markdown" -> "Markdown"
            else -> "Plain Text"
        }
    }

    fun highlight(text: String, language: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        if (text.length > 50000) {
            // For massive files, return plain text to preserve 60fps scrolling
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            append(text)

            when (language) {
                "HTML", "XML" -> highlightXmlHtml(text, this)
                "CSS" -> highlightCss(text, this)
                "Python" -> highlightGeneric(text, this, KEYWORDS_PYTHON, "#", false)
                "JavaScript" -> highlightGeneric(text, this, KEYWORDS_JS, "//", true)
                "Kotlin", "Java" -> highlightGeneric(text, this, KEYWORDS_KOTLIN_JAVA, "//", true)
                "C", "C++" -> highlightGeneric(text, this, KEYWORDS_C_CPP, "//", true)
                "SQL" -> highlightGeneric(text, this, KEYWORDS_SQL, "--", false, caseInsensitive = true)
                "JSON" -> highlightJson(text, this)
                "Markdown" -> highlightMarkdown(text, this)
                else -> highlightGeneric(text, this, KEYWORDS_JS, "//", true)
            }
        }
    }

    private fun highlightGeneric(
        text: String,
        builder: AnnotatedString.Builder,
        keywords: Set<String>,
        singleLineCommentPrefix: String,
        hasMultiLineComment: Boolean,
        caseInsensitive: Boolean = false
    ) {
        val length = text.length

        // Numbers: e.g. 123, 0x1A, 3.14
        val numberPattern = Pattern.compile("\\b(\\d+(\\.\\d+)?|0x[0-9a-fA-F]+)\\b")
        val numberMatcher = numberPattern.matcher(text)
        while (numberMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxNumber), numberMatcher.start(), numberMatcher.end())
        }

        // Strings: "..." or '...'
        val stringPattern = Pattern.compile("(\"[^\"]*\"|'[^']*')")
        val stringMatcher = stringPattern.matcher(text)
        while (stringMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxString), stringMatcher.start(), stringMatcher.end())
        }

        // Functions: e.g. functionName(...)
        val funcPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()")
        val funcMatcher = funcPattern.matcher(text)
        while (funcMatcher.find()) {
            val name = funcMatcher.group(1) ?: ""
            if (!keywords.contains(name)) {
                builder.addStyle(SpanStyle(color = SyntaxFunction), funcMatcher.start(1), funcMatcher.end(1))
            }
        }

        // Words / Keywords
        val wordPattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        val wordMatcher = wordPattern.matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            val match = if (caseInsensitive) keywords.any { it.equals(word, ignoreCase = true) } else keywords.contains(word)
            if (match) {
                builder.addStyle(
                    SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold),
                    wordMatcher.start(),
                    wordMatcher.end()
                )
            } else if (word.first().isUpperCase()) {
                builder.addStyle(SpanStyle(color = SyntaxType), wordMatcher.start(), wordMatcher.end())
            }
        }

        // Single line comments
        val commentPattern = Pattern.compile(Pattern.quote(singleLineCommentPrefix) + ".*")
        val commentMatcher = commentPattern.matcher(text)
        while (commentMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxComment), commentMatcher.start(), commentMatcher.end())
        }

        // Multi-line comments: /* ... */
        if (hasMultiLineComment) {
            val mlCommentPattern = Pattern.compile("/\\*[\\s\\S]*?\\*/")
            val mlMatcher = mlCommentPattern.matcher(text)
            while (mlMatcher.find()) {
                builder.addStyle(SpanStyle(color = SyntaxComment), mlMatcher.start(), mlMatcher.end())
            }
        }
    }

    private fun highlightXmlHtml(text: String, builder: AnnotatedString.Builder) {
        // Tag names: <tag or </tag
        val tagPattern = Pattern.compile("</?([a-zA-Z0-9-]+)")
        val tagMatcher = tagPattern.matcher(text)
        while (tagMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), tagMatcher.start(1), tagMatcher.end(1))
        }

        // Attributes: attr=
        val attrPattern = Pattern.compile("\\b([a-zA-Z0-9-:]+)(?=\\s*=)")
        val attrMatcher = attrPattern.matcher(text)
        while (attrMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxType), attrMatcher.start(1), attrMatcher.end(1))
        }

        // Strings / Attribute values
        val strPattern = Pattern.compile("\"[^\"]*\"|'[^']*'")
        val strMatcher = strPattern.matcher(text)
        while (strMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
        }

        // Comments: <!-- ... -->
        val commentPattern = Pattern.compile("<!--[\\s\\S]*?-->")
        val commentMatcher = commentPattern.matcher(text)
        while (commentMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxComment), commentMatcher.start(), commentMatcher.end())
        }
    }

    private fun highlightCss(text: String, builder: AnnotatedString.Builder) {
        // Selectors
        val selectorPattern = Pattern.compile("(^|\\n)\\s*([.#a-zA-Z0-9_:\\-\\s,>+~]+)(?=\\s*\\{)")
        val selMatcher = selectorPattern.matcher(text)
        while (selMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Medium), selMatcher.start(2), selMatcher.end(2))
        }

        // Properties: color:
        val propPattern = Pattern.compile("\\b([a-zA-Z0-9-]+)\\s*(?=:)")
        val propMatcher = propPattern.matcher(text)
        while (propMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxKeyword), propMatcher.start(1), propMatcher.end(1))
        }

        // Values & Units
        val unitPattern = Pattern.compile("\\b(\\d+(\\.\\d+)?(px|rem|em|vh|vw|%|s|ms)?|#[0-9a-fA-F]{3,8})\\b")
        val unitMatcher = unitPattern.matcher(text)
        while (unitMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxNumber), unitMatcher.start(), unitMatcher.end())
        }

        // Comments: /* ... */
        val commentPattern = Pattern.compile("/\\*[\\s\\S]*?\\*/")
        val commentMatcher = commentPattern.matcher(text)
        while (commentMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxComment), commentMatcher.start(), commentMatcher.end())
        }
    }

    private fun highlightJson(text: String, builder: AnnotatedString.Builder) {
        // Keys: "key":
        val keyPattern = Pattern.compile("\"([^\"]+)\"\\s*:")
        val keyMatcher = keyPattern.matcher(text)
        while (keyMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold), keyMatcher.start(1), keyMatcher.end(1))
        }

        // String values
        val strPattern = Pattern.compile(":\\s*\"([^\"]*)\"")
        val strMatcher = strPattern.matcher(text)
        while (strMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxString), strMatcher.start(1), strMatcher.end(1))
        }

        // Numbers & Booleans
        val valPattern = Pattern.compile("\\b(true|false|null|\\d+(\\.\\d+)?)\\b")
        val valMatcher = valPattern.matcher(text)
        while (valMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxNumber), valMatcher.start(), valMatcher.end())
        }
    }

    private fun highlightMarkdown(text: String, builder: AnnotatedString.Builder) {
        // Headings: #, ##, ###
        val headingPattern = Pattern.compile("^(#{1,6}\\s+.*)$", Pattern.MULTILINE)
        val headingMatcher = headingPattern.matcher(text)
        while (headingMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), headingMatcher.start(), headingMatcher.end())
        }

        // Code blocks: ``` ... ```
        val codePattern = Pattern.compile("```[\\s\\S]*?```|`[^`]+`")
        val codeMatcher = codePattern.matcher(text)
        while (codeMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxFunction, fontFamily = FontFamily.Monospace), codeMatcher.start(), codeMatcher.end())
        }

        // Links: [title](url)
        val linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
        val linkMatcher = linkPattern.matcher(text)
        while (linkMatcher.find()) {
            builder.addStyle(SpanStyle(color = SyntaxString), linkMatcher.start(), linkMatcher.end())
        }
    }
}
