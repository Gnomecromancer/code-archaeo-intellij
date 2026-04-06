package com.gnomecromancer.archaeo.services

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ClaudeService(private val apiKey: String, private val model: String = "claude-sonnet-4-6") {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun synthesize(history: String, filePath: String, functionName: String?, commitCount: Int): String {
        val target = if (functionName != null) "the function `$functionName` in `$filePath`" else "`$filePath`"
        val plural = if (commitCount == 1) "commit" else "commits"

        val systemPrompt = """
            You are a code historian. Given a series of git commits, you synthesize a clear,
            narrative account of how code evolved over time. Focus on:
            - What problem the code was originally solving
            - The key decisions made and why (infer from commit messages and diff content)
            - What got added, removed, or redesigned at each significant stage
            - The arc of the story: what changed and what drove those changes

            Write in flowing prose, not bullet points. Be specific about what changed.
            Don't just restate commit messages — synthesize meaning.
            If you see patterns (repeated rewrites, gradual simplification, performance tuning), name them.
            Keep it under 500 words unless the history is unusually complex.
        """.trimIndent()

        val userMessage = """
            Here is the git history for $target ($commitCount $plural, newest first):

            $history

            Tell the story of how $target got to where it is today.
        """.trimIndent()

        val requestBody = buildJsonRequest(systemPrompt, userMessage)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            val msg = extractJsonField(response.body(), "message") ?: response.body().take(200)
            throw RuntimeException("API error ${response.statusCode()}: $msg")
        }

        return extractNarrativeText(response.body())
            ?: throw RuntimeException("Could not parse response from Claude API")
    }

    private fun buildJsonRequest(system: String, user: String): String {
        return """{"model":"$model","max_tokens":1024,"system":${jsonString(system)},"messages":[{"role":"user","content":${jsonString(user)}}]}"""
    }

    private fun extractNarrativeText(json: String): String? {
        // Parse: {"content":[{"type":"text","text":"..."}],...}
        val textPattern = Regex(""""text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return textPattern.find(json)?.groupValues?.get(1)?.unescapeJson()
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex(""""$field"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return pattern.find(json)?.groupValues?.get(1)?.unescapeJson()
    }

    private fun jsonString(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun String.unescapeJson(): String = this
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}
