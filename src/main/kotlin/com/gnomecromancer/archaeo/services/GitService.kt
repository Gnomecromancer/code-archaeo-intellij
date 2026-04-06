package com.gnomecromancer.archaeo.services

import java.io.File

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val date: String,
    val author: String,
    val message: String,
    val diff: String,
)

class GitService {

    fun findRepoRoot(filePath: String): String? {
        val dir = File(filePath).let { if (it.isDirectory) it else it.parentFile } ?: return null
        return runGit(listOf("rev-parse", "--show-toplevel"), dir)?.trim()
    }

    fun getFileCommits(filePath: String, maxCommits: Int = 50): List<GitCommit> {
        val absFile = File(filePath).canonicalFile
        val repoRoot = findRepoRoot(absFile.path) ?: return emptyList()
        val repoDir = File(repoRoot)
        val relPath = absFile.relativeTo(repoDir).path.replace('\\', '/')

        val sep = "\u0001"
        val logOut = runGit(
            listOf(
                "log", "--follow",
                "--max-count=$maxCommits",
                "--format=%H${sep}%h${sep}%ci${sep}%an${sep}%s",
                "--", relPath
            ),
            repoDir
        ) ?: return emptyList()

        return logOut.trim().lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(sep, limit = 5)
                if (parts.size < 5) return@mapNotNull null
                val (hash, shortHash, date, author, message) = parts

                val diff = runGit(
                    listOf("show", "--stat", "--unified=5", hash, "--", relPath),
                    repoDir
                ) ?: ""

                GitCommit(hash, shortHash, date.take(10), author, message, diff)
            }
    }

    fun formatForPrompt(commits: List<GitCommit>, functionName: String?, maxDiffChars: Int = 1500): String {
        return commits.mapIndexed { i, c ->
            var diff = c.diff
            if (functionName != null) {
                diff = narrowToFunction(diff, functionName)
            }
            if (diff.length > maxDiffChars) {
                diff = diff.take(maxDiffChars) + "\n... [truncated]"
            }
            "### Commit ${i + 1}: ${c.shortHash} — ${c.date}\n" +
            "**Author:** ${c.author}\n" +
            "**Message:** ${c.message}\n\n" +
            "```diff\n$diff\n```"
        }.joinToString("\n\n")
    }

    private fun narrowToFunction(diff: String, functionName: String, contextLines: Int = 8): String {
        if (functionName !in diff) return diff
        val lines = diff.lines()
        val relevant = mutableSetOf<Int>()
        lines.forEachIndexed { i, line ->
            if (functionName in line) {
                for (j in maxOf(0, i - contextLines)..minOf(lines.lastIndex, i + contextLines)) {
                    relevant.add(j)
                }
            }
        }
        if (relevant.isEmpty()) return diff
        val result = mutableListOf<String>()
        var prev = -1
        for (i in relevant.sorted()) {
            if (prev != -1 && i > prev + 1) result.add("...")
            result.add(lines[i])
            prev = i
        }
        return result.joinToString("\n")
    }

    private fun runGit(args: List<String>, dir: File): String? {
        return try {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(dir)
                .redirectErrorStream(false)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            if (process.exitValue() != 0) null else output
        } catch (_: Exception) {
            null
        }
    }
}
