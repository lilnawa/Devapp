package com.example.engine.terminal

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.engine.python.PythonInterpreter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TerminalLine(
    val text: String,
    val type: TerminalLineType = TerminalLineType.OUTPUT
)

enum class TerminalLineType {
    INPUT,
    OUTPUT,
    SUCCESS,
    ERROR,
    WARNING,
    SYSTEM
}

class TerminalEngine(
    private val context: Context,
    initialDirectory: File? = null
) {
    private val pythonInterpreter = PythonInterpreter()
    var currentDirectory: File = initialDirectory ?: File(context.filesDir, "devhub_projects")
        private set

    val history = mutableListOf<String>()

    init {
        if (!currentDirectory.exists()) {
            currentDirectory.mkdirs()
        }
    }

    fun setDirectory(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            currentDirectory = dir
        }
    }

    suspend fun executeCommand(commandLine: String): List<TerminalLine> = withContext(Dispatchers.IO) {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        history.add(trimmed)
        val result = mutableListOf<TerminalLine>()

        // Split tokens respecting simple quotes
        val tokens = tokenizeCommand(trimmed)
        val cmd = tokens.firstOrNull()?.lowercase() ?: return@withContext emptyList()
        val args = if (tokens.size > 1) tokens.subList(1, tokens.size) else emptyList()

        when (cmd) {
            "help" -> {
                result.add(TerminalLine("DevHub Mobile Terminal v1.0.0 (ARM64 / Android Linux)", TerminalLineType.SYSTEM))
                result.add(TerminalLine("Available Built-in Commands:", TerminalLineType.SYSTEM))
                result.add(TerminalLine("  pwd                   Print current working directory"))
                result.add(TerminalLine("  ls [-l -a]            List directory contents and file sizes"))
                result.add(TerminalLine("  cd <path>             Change directory (.. supported)"))
                result.add(TerminalLine("  cat <file>            Display file content"))
                result.add(TerminalLine("  echo <text> [> file]  Output text or redirect to file"))
                result.add(TerminalLine("  touch <file>          Create a new empty file"))
                result.add(TerminalLine("  mkdir <dir>           Create a new folder"))
                result.add(TerminalLine("  rm [-r] <target>      Remove file or directory"))
                result.add(TerminalLine("  python <script.py>    Execute Python script via embedded engine"))
                result.add(TerminalLine("  git <status|log|...>  Version control operations"))
                result.add(TerminalLine("  uname -a              Display Android kernel and architecture"))
                result.add(TerminalLine("  df -h                 Display storage filesystem usage"))
                result.add(TerminalLine("  env                   Show active environment variables"))
                result.add(TerminalLine("  clear                 Clear terminal screen"))
            }

            "pwd" -> {
                result.add(TerminalLine(currentDirectory.absolutePath, TerminalLineType.OUTPUT))
            }

            "ls" -> {
                val showAll = args.contains("-a") || args.contains("-la") || args.contains("-al")
                val longFormat = args.contains("-l") || args.contains("-la") || args.contains("-al")
                val files = currentDirectory.listFiles() ?: emptyArray()

                val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                if (longFormat) {
                    val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                    result.add(TerminalLine("total ${sorted.size}", TerminalLineType.SYSTEM))
                    for (file in sorted) {
                        if (!showAll && file.name.startsWith(".")) continue
                        val perm = if (file.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                        val size = if (file.isDirectory) "4096" else file.length().toString()
                        val date = sdf.format(Date(file.lastModified()))
                        val type = if (file.isDirectory) TerminalLineType.SYSTEM else TerminalLineType.OUTPUT
                        result.add(TerminalLine("$perm 1 devhub devhub ${size.padStart(8)} $date ${file.name}", type))
                    }
                } else {
                    val names = sorted
                        .filter { showAll || !it.name.startsWith(".") }
                        .joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                    result.add(TerminalLine(names.ifEmpty { "(empty directory)" }, TerminalLineType.OUTPUT))
                }
            }

            "cd" -> {
                if (args.isEmpty() || args[0] == "~") {
                    currentDirectory = File(context.filesDir, "devhub_projects")
                    result.add(TerminalLine(currentDirectory.absolutePath, TerminalLineType.OUTPUT))
                } else {
                    val target = resolveFile(args[0])
                    if (!target.exists()) {
                        result.add(TerminalLine("cd: no such file or directory: ${args[0]}", TerminalLineType.ERROR))
                    } else if (!target.isDirectory) {
                        result.add(TerminalLine("cd: not a directory: ${args[0]}", TerminalLineType.ERROR))
                    } else {
                        currentDirectory = target.canonicalFile
                    }
                }
            }

            "cat" -> {
                if (args.isEmpty()) {
                    result.add(TerminalLine("cat: missing file operand", TerminalLineType.ERROR))
                } else {
                    val file = resolveFile(args[0])
                    if (!file.exists()) {
                        result.add(TerminalLine("cat: ${args[0]}: No such file or directory", TerminalLineType.ERROR))
                    } else if (file.isDirectory) {
                        result.add(TerminalLine("cat: ${args[0]}: Is a directory", TerminalLineType.ERROR))
                    } else {
                        file.readLines().forEach {
                            result.add(TerminalLine(it, TerminalLineType.OUTPUT))
                        }
                    }
                }
            }

            "echo" -> {
                val fullEcho = trimmed.removePrefix("echo").trim()
                if (fullEcho.contains(">")) {
                    val append = fullEcho.contains(">>")
                    val delimiter = if (append) ">>" else ">"
                    val parts = fullEcho.split(delimiter, limit = 2)
                    val textToEcho = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                    val fileName = parts[1].trim()
                    val targetFile = resolveFile(fileName)
                    targetFile.parentFile?.mkdirs()
                    if (append && targetFile.exists()) {
                        targetFile.appendText("\n$textToEcho")
                    } else {
                        targetFile.writeText(textToEcho)
                    }
                    result.add(TerminalLine("Wrote to ${targetFile.name}", TerminalLineType.SUCCESS))
                } else {
                    val text = fullEcho.removeSurrounding("\"").removeSurrounding("'")
                    result.add(TerminalLine(text, TerminalLineType.OUTPUT))
                }
            }

            "touch" -> {
                if (args.isEmpty()) {
                    result.add(TerminalLine("touch: missing file operand", TerminalLineType.ERROR))
                } else {
                    for (fileName in args) {
                        val file = resolveFile(fileName)
                        file.parentFile?.mkdirs()
                        if (!file.exists()) {
                            file.createNewFile()
                        } else {
                            file.setLastModified(System.currentTimeMillis())
                        }
                    }
                    result.add(TerminalLine("Updated ${args.size} file(s)", TerminalLineType.SUCCESS))
                }
            }

            "mkdir" -> {
                if (args.isEmpty()) {
                    result.add(TerminalLine("mkdir: missing operand", TerminalLineType.ERROR))
                } else {
                    for (dirName in args) {
                        val dir = resolveFile(dirName)
                        if (dir.mkdirs() || dir.exists()) {
                            result.add(TerminalLine("Created directory: $dirName", TerminalLineType.SUCCESS))
                        } else {
                            result.add(TerminalLine("mkdir: cannot create directory '$dirName'", TerminalLineType.ERROR))
                        }
                    }
                }
            }

            "rm" -> {
                val recursive = args.contains("-r") || args.contains("-rf")
                val targets = args.filter { !it.startsWith("-") }
                if (targets.isEmpty()) {
                    result.add(TerminalLine("rm: missing operand", TerminalLineType.ERROR))
                } else {
                    for (targetName in targets) {
                        val file = resolveFile(targetName)
                        if (!file.exists()) {
                            result.add(TerminalLine("rm: cannot remove '$targetName': No such file or directory", TerminalLineType.ERROR))
                        } else if (file.isDirectory && !recursive) {
                            result.add(TerminalLine("rm: cannot remove '$targetName': Is a directory (use -r)", TerminalLineType.ERROR))
                        } else {
                            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                            if (deleted) {
                                result.add(TerminalLine("Removed '$targetName'", TerminalLineType.SUCCESS))
                            } else {
                                result.add(TerminalLine("rm: cannot remove '$targetName': Permission denied", TerminalLineType.ERROR))
                            }
                        }
                    }
                }
            }

            "python", "python3" -> {
                if (args.isEmpty()) {
                    result.add(TerminalLine("DevHub Python 3.12 (Embedded Mobile Runtime)", TerminalLineType.SYSTEM))
                    result.add(TerminalLine("Use 'python <file.py>' or 'python -c \"print(1+1)\"'", TerminalLineType.SYSTEM))
                } else if (args[0] == "-c") {
                    val code = trimmed.substringAfter("-c").trim().removeSurrounding("\"").removeSurrounding("'")
                    result.add(TerminalLine("Executing code snippet...", TerminalLineType.SYSTEM))
                    val execResult = pythonInterpreter.execute(code)
                    if (execResult.output.isNotBlank()) {
                        execResult.output.lines().forEach { result.add(TerminalLine(it, TerminalLineType.OUTPUT)) }
                    }
                    if (execResult.errors.isNotBlank()) {
                        execResult.errors.lines().forEach { result.add(TerminalLine(it, TerminalLineType.ERROR)) }
                    }
                } else {
                    val file = resolveFile(args[0])
                    if (!file.exists()) {
                        result.add(TerminalLine("python: can't open file '${args[0]}': [Errno 2] No such file or directory", TerminalLineType.ERROR))
                    } else {
                        result.add(TerminalLine("Running ${file.name}...", TerminalLineType.SYSTEM))
                        val scriptContent = file.readText()
                        val execResult = pythonInterpreter.execute(scriptContent)
                        if (execResult.output.isNotBlank()) {
                            execResult.output.lines().forEach { result.add(TerminalLine(it, TerminalLineType.OUTPUT)) }
                        }
                        if (execResult.errors.isNotBlank()) {
                            execResult.errors.lines().forEach { result.add(TerminalLine(it, TerminalLineType.ERROR)) }
                        }
                        result.add(TerminalLine("Process finished with exit code ${execResult.exitCode} (${execResult.executionTimeMs}ms)", if (execResult.exitCode == 0) TerminalLineType.SUCCESS else TerminalLineType.ERROR))
                    }
                }
            }

            "git" -> {
                handleGitCommand(args, result)
            }

            "uname" -> {
                result.add(TerminalLine("Linux localhost ${Build.VERSION.RELEASE} ${Build.SUPPORTED_ABIS.firstOrNull() ?: "aarch64"} Android DevHub", TerminalLineType.OUTPUT))
            }

            "df" -> {
                try {
                    val stat = StatFs(context.filesDir.path)
                    val bytesTotal = stat.totalBytes
                    val bytesAvailable = stat.availableBytes
                    val totalMb = bytesTotal / (1024 * 1024)
                    val availMb = bytesAvailable / (1024 * 1024)
                    result.add(TerminalLine("Filesystem      Size  Used  Avail Use% Mounted on", TerminalLineType.SYSTEM))
                    result.add(TerminalLine("/data/user/0    ${totalMb}M  ${totalMb - availMb}M  ${availMb}M  ${((totalMb - availMb) * 100 / totalMb.coerceAtLeast(1))}% /data", TerminalLineType.OUTPUT))
                } catch (e: Exception) {
                    result.add(TerminalLine("Storage telemetry unavailable: ${e.message}", TerminalLineType.WARNING))
                }
            }

            "env" -> {
                result.add(TerminalLine("SHELL=/system/bin/devhub-sh", TerminalLineType.OUTPUT))
                result.add(TerminalLine("TERM=xterm-256color", TerminalLineType.OUTPUT))
                result.add(TerminalLine("HOME=${context.filesDir.absolutePath}", TerminalLineType.OUTPUT))
                result.add(TerminalLine("PWD=${currentDirectory.absolutePath}", TerminalLineType.OUTPUT))
                result.add(TerminalLine("USER=devhub", TerminalLineType.OUTPUT))
                result.add(TerminalLine("ANDROID_SDK_ROOT=/system/devhub/android-sdk", TerminalLineType.OUTPUT))
                result.add(TerminalLine("PYTHON_VERSION=3.12.0", TerminalLineType.OUTPUT))
            }

            "clear" -> {
                // Return empty to clear
            }

            else -> {
                result.add(TerminalLine("devhub-sh: command not found: $cmd. Type 'help' for available commands.", TerminalLineType.ERROR))
            }
        }

        result
    }

    private fun handleGitCommand(args: List<String>, result: MutableList<TerminalLine>) {
        if (args.isEmpty()) {
            result.add(TerminalLine("usage: git <command> [<args>]", TerminalLineType.SYSTEM))
            result.add(TerminalLine("Supported: status, log, branch, init, add, commit, push", TerminalLineType.SYSTEM))
            return
        }

        when (args[0]) {
            "status" -> {
                result.add(TerminalLine("On branch main", TerminalLineType.SYSTEM))
                result.add(TerminalLine("Your branch is up to date with 'origin/main'.", TerminalLineType.OUTPUT))
                val files = currentDirectory.listFiles() ?: emptyArray()
                result.add(TerminalLine("Working directory clean (${files.size} tracked items)", TerminalLineType.SUCCESS))
            }
            "init" -> {
                val gitDir = File(currentDirectory, ".git")
                gitDir.mkdirs()
                result.add(TerminalLine("Initialized empty Git repository in ${gitDir.absolutePath}", TerminalLineType.SUCCESS))
            }
            "branch" -> {
                result.add(TerminalLine("* main", TerminalLineType.SUCCESS))
            }
            "log" -> {
                result.add(TerminalLine("commit a78f3d10c2b5e (HEAD -> main)", TerminalLineType.WARNING))
                result.add(TerminalLine("Author: DevHub Developer <developer@devhub.app>", TerminalLineType.OUTPUT))
                result.add(TerminalLine("Date:   ${Date()}", TerminalLineType.OUTPUT))
                result.add(TerminalLine("\n    Initial commit from DevHub Mobile IDE\n", TerminalLineType.SYSTEM))
            }
            "add" -> {
                result.add(TerminalLine("Staged changes for commit.", TerminalLineType.SUCCESS))
            }
            "commit" -> {
                result.add(TerminalLine("[main 9b8c21a] Project files committed.", TerminalLineType.SUCCESS))
            }
            "push" -> {
                result.add(TerminalLine("Everything up-to-date with remote origin.", TerminalLineType.SUCCESS))
            }
            else -> {
                result.add(TerminalLine("git: '${args[0]}' is not a recognized git command in local sandbox", TerminalLineType.ERROR))
            }
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            File(path)
        } else if (path.startsWith("~")) {
            File(context.filesDir, "devhub_projects/${path.removePrefix("~").removePrefix("/")}")
        } else {
            File(currentDirectory, path)
        }
    }

    private fun tokenizeCommand(command: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        var quoteChar = ' '
        val current = StringBuilder()

        for (ch in command) {
            when {
                (ch == '"' || ch == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                inQuotes && ch == quoteChar -> {
                    inQuotes = false
                }
                !inQuotes && ch.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.setLength(0)
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
