package com.gnomecromancer.archaeo.services

import java.io.File
import java.util.concurrent.TimeUnit

class ClaudeService(
    private val cliPath: String = "claude",
    private val model: String = "claude-sonnet-4-6",
) {

    fun synthesize(history: String, filePath: String, functionName: String?, commitCount: Int): String {
        val target = if (functionName != null) "the function `$functionName` in `$filePath`" else "`$filePath`"
        val plural = if (commitCount == 1) "commit" else "commits"

        val prompt = """
You are a code historian. Given git commits, write a clear narrative of how this code evolved.
Focus on: what problem it was originally solving, key decisions made (infer from messages and diffs),
what got added/removed/redesigned, the arc of the story. Write in flowing prose, not bullet points.
Be specific. Don't just restate commit messages — synthesize meaning. Under 500 words.

Here is the git history for $target ($commitCount $plural, newest first):

$history

Tell the story of how $target got to where it is today.
        """.trimIndent()

        // Write to temp file to avoid Windows 32KB command line limit
        val tempFile = File.createTempFile("archaeo_prompt_", ".txt")
        try {
            tempFile.writeText(prompt, Charsets.UTF_8)

            val process = ProcessBuilder(listOf(cliPath, "--print", "--model", model))
                .redirectErrorStream(false)
                .redirectInput(tempFile)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val errors = process.errorStream.bufferedReader().readText()
            val finished = process.waitFor(120, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("Claude CLI timed out after 120 seconds")
            }
            if (process.exitValue() != 0) {
                val detail = errors.take(300).ifBlank { output.take(300) }
                throw RuntimeException("Claude CLI error (exit ${process.exitValue()}): $detail")
            }

            return output.trim().ifBlank {
                throw RuntimeException("Claude CLI returned no output")
            }
        } finally {
            tempFile.delete()
        }
    }
}
