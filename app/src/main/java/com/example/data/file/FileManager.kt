package com.example.data.file

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val extension: String = "",
    val children: List<FileNode> = emptyList()
)

class FileManager(private val context: Context) {

    private val rootProjectsDir: File
        get() {
            val dir = File(context.filesDir, "devhub_projects")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getProjectsDirectory(): File = rootProjectsDir

    fun getProjectDirectory(projectPathName: String): File {
        return File(rootProjectsDir, projectPathName)
    }

    suspend fun createProject(
        projectName: String,
        templateName: String
    ): Pair<File, Map<String, String>> = withContext(Dispatchers.IO) {
        val sanitizedName = projectName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        var projectDir = File(rootProjectsDir, sanitizedName)
        var counter = 1
        while (projectDir.exists()) {
            projectDir = File(rootProjectsDir, "${sanitizedName}_$counter")
            counter++
        }
        projectDir.mkdirs()

        val filesMap = ProjectTemplates.getTemplateFiles(templateName, projectDir.name)
        for ((relativePath, content) in filesMap) {
            val targetFile = File(projectDir, relativePath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content)
        }

        Pair(projectDir, filesMap)
    }

    suspend fun listFileTree(directory: File): List<FileNode> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()
        val files = directory.listFiles() ?: return@withContext emptyList()
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                if (file.isDirectory) {
                    FileNode(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = true,
                        lastModified = file.lastModified(),
                        children = listFileTree(file)
                    )
                } else {
                    FileNode(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        extension = file.extension
                    )
                }
            }
    }

    suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists() && file.isFile) {
            file.readText()
        } else {
            ""
        }
    }

    suspend fun writeFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createFile(parentDir: File, fileName: String, initialContent: String = ""): File? = withContext(Dispatchers.IO) {
        try {
            val target = File(parentDir, fileName.trim())
            target.parentFile?.mkdirs()
            if (!target.exists()) {
                target.createNewFile()
                if (initialContent.isNotEmpty()) {
                    target.writeText(initialContent)
                }
            }
            target
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createDirectory(parentDir: File, dirName: String): File? = withContext(Dispatchers.IO) {
        try {
            val target = File(parentDir, dirName.trim())
            if (!target.exists()) {
                target.mkdirs()
            }
            target
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFileOrDirectory(fileOrDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (fileOrDir.isDirectory) {
                fileOrDir.deleteRecursively()
            } else {
                fileOrDir.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameFile(file: File, newName: String): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(file.parentFile, newName.trim())
            if (file.renameTo(dest)) dest else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun duplicateProject(srcProjectDir: File, newName: String): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(rootProjectsDir, newName.trim())
            if (dest.exists()) return@withContext null
            srcProjectDir.copyRecursively(dest, overwrite = false)
            dest
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun calculateDirectoryStats(directory: File): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var fileCount = 0
        var totalBytes = 0L
        if (directory.exists()) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    fileCount++
                    totalBytes += file.length()
                }
            }
        }
        Pair(fileCount, totalBytes)
    }

    suspend fun searchInFiles(projectDir: File, query: String): List<Pair<File, List<String>>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<File, List<String>>>()
        if (query.isBlank() || !projectDir.exists()) return@withContext results

        projectDir.walkTopDown().forEach { file ->
            if (file.isFile && !file.name.endsWith(".zip") && !file.name.endsWith(".png") && !file.name.endsWith(".jpg")) {
                try {
                    val matchingLines = mutableListOf<String>()
                    file.forEachLine { line ->
                        if (line.contains(query, ignoreCase = true)) {
                            matchingLines.add(line.trim())
                        }
                    }
                    if (matchingLines.isNotEmpty() || file.name.contains(query, ignoreCase = true)) {
                        results.add(Pair(file, matchingLines))
                    }
                } catch (_: Exception) {}
            }
        }
        results
    }

    // ZIP EXPORT
    suspend fun exportProjectToZip(projectDir: File, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(outputStream).use { zipOut ->
                projectDir.walkTopDown().forEach { file ->
                    val relativePath = file.relativeTo(projectDir).path
                    if (relativePath.isNotEmpty()) {
                        if (file.isDirectory) {
                            zipOut.putNextEntry(ZipEntry("$relativePath/"))
                            zipOut.closeEntry()
                        } else {
                            zipOut.putNextEntry(ZipEntry(relativePath))
                            file.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ZIP IMPORT
    suspend fun importProjectFromZip(inputStream: InputStream, targetProjectName: String): File? = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(rootProjectsDir, targetProjectName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))
            if (!targetDir.exists()) targetDir.mkdirs()

            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Protect against Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Invalid zip entry: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            targetDir
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
