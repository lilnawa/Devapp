package com.example.data.github

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class GitHubUser(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val publicRepos: Int,
    val bio: String?
)

data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val cloneUrl: String,
    val stars: Int,
    val forks: Int,
    val language: String?,
    val isPrivate: Boolean,
    val defaultBranch: String
)

class GitHubService(
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun getAuthenticatedUser(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevHub-Mobile-IDE")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API Error: ${response.code} ${response.message}"))
                }
                val body = response.body?.string() ?: "{}"
                val json = JSONObject(body)
                val user = GitHubUser(
                    login = json.optString("login", ""),
                    name = json.optNullableString("name"),
                    avatarUrl = json.optNullableString("avatar_url"),
                    publicRepos = json.optInt("public_repos", 0),
                    bio = json.optNullableString("bio")
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepositories(token: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user/repos?sort=updated&per_page=30")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevHub-Mobile-IDE")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(body)
                val repos = mutableListOf<GitHubRepo>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    repos.add(parseRepo(item))
                }
                Result.success(repos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRepositories(query: String, token: String? = null): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url("https://api.github.com/search/repositories?q=${java.net.URLEncoder.encode(query, "UTF-8")}&sort=stars&order=desc&per_page=20")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevHub-Mobile-IDE")

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Search Error HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: "{}"
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: JSONArray()
                val repos = mutableListOf<GitHubRepo>()
                for (i in 0 until items.length()) {
                    repos.add(parseRepo(items.getJSONObject(i)))
                }
                Result.success(repos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReadme(fullName: String, token: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url("https://api.github.com/repos/$fullName/readme")
                .header("Accept", "application/vnd.github.raw")
                .header("User-Agent", "DevHub-Mobile-IDE")

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("README not found (${response.code})"))
                }
                Result.success(response.body?.string() ?: "# No README available")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cloneRepoFiles(
        fullName: String,
        targetDir: File,
        token: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Fetch default branch contents
            val reqBuilder = Request.Builder()
                .url("https://api.github.com/repos/$fullName/contents")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevHub-Mobile-IDE")

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Clone failed: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(body)
                var fileCount = 0

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val type = item.optString("type")
                    val name = item.optString("name")
                    val downloadUrl = item.optString("download_url")

                    if (type == "file" && downloadUrl.isNotBlank()) {
                        val fileReq = Request.Builder().url(downloadUrl).build()
                        client.newCall(fileReq).execute().use { fileResp ->
                            if (fileResp.isSuccessful) {
                                val destFile = File(targetDir, name)
                                destFile.parentFile?.mkdirs()
                                destFile.writeBytes(fileResp.body?.bytes() ?: byteArrayOf())
                                fileCount++
                            }
                        }
                    }
                }

                // If no root files found or empty, save README
                if (fileCount == 0) {
                    val readme = getReadme(fullName, token).getOrNull() ?: "# $fullName\n\nCloned into DevHub."
                    File(targetDir, "README.md").writeText(readme)
                    fileCount = 1
                }

                Result.success(fileCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitFile(
        fullName: String,
        path: String,
        content: String,
        commitMessage: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First check if file already exists to get SHA
            val checkReq = Request.Builder()
                .url("https://api.github.com/repos/$fullName/contents/$path")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("User-Agent", "DevHub-Mobile-IDE")
                .build()

            var sha: String? = null
            client.newCall(checkReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    sha = json.optNullableString("sha")
                }
            }

            val base64Content = android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP)
            val jsonPayload = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                if (sha != null) {
                    put("sha", sha)
                }
            }

            val request = Request.Builder()
                .url("https://api.github.com/repos/$fullName/contents/$path")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevHub-Mobile-IDE")
                .put(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Commit successful: $path")
                } else {
                    Result.failure(Exception("Commit error ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRepo(item: JSONObject): GitHubRepo {
        return GitHubRepo(
            id = item.optLong("id", 0L),
            name = item.optString("name", "repo"),
            fullName = item.optString("full_name", "repo"),
            description = item.optNullableString("description"),
            htmlUrl = item.optString("html_url", ""),
            cloneUrl = item.optString("clone_url", ""),
            stars = item.optInt("stargazers_count", 0),
            forks = item.optInt("forks_count", 0),
            language = item.optString("language", "Code"),
            isPrivate = item.optBoolean("private", false),
            defaultBranch = item.optString("default_branch", "main")
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key) else null
    }
}
