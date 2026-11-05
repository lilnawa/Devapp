package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.python.PythonInterpreter
import com.example.engine.terminal.TerminalEngine
import com.example.ui.editor.SyntaxHighlighter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("nawa hub", appName)
    }

    @Test
    fun testSyntaxHighlighterLanguageDetection() {
        assertEquals("HTML", SyntaxHighlighter.detectLanguage("index.html"))
        assertEquals("Python", SyntaxHighlighter.detectLanguage("main.py"))
        assertEquals("Kotlin", SyntaxHighlighter.detectLanguage("MainActivity.kt"))
        assertEquals("Java", SyntaxHighlighter.detectLanguage("Main.java"))
        assertEquals("JavaScript", SyntaxHighlighter.detectLanguage("script.js"))
        assertEquals("CSS", SyntaxHighlighter.detectLanguage("style.css"))
        assertEquals("Markdown", SyntaxHighlighter.detectLanguage("README.md"))
        assertEquals("JSON", SyntaxHighlighter.detectLanguage("config.json"))
    }

    @Test
    fun testPythonInterpreterExecution() = runBlocking {
        val interpreter = PythonInterpreter()
        val script = """
            x = 10
            y = 20
            print("SUM:", x + y)
        """.trimIndent()

        val result = interpreter.execute(script)
        assertEquals(0, result.exitCode)
        assertTrue(result.output.contains("SUM: 30"))
    }

    @Test
    fun testTerminalCommands() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TerminalEngine(context)

        val helpLines = engine.executeCommand("help")
        assertTrue(helpLines.any { it.text.contains("DevHub Mobile Terminal") })

        val pwdLines = engine.executeCommand("pwd")
        assertTrue(pwdLines.isNotEmpty())
    }
}
