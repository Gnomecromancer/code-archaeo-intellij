package com.gnomecromancer.archaeo.actions

import com.gnomecromancer.archaeo.services.ArchaeoSettings
import com.gnomecromancer.archaeo.services.ClaudeService
import com.gnomecromancer.archaeo.services.GitService
import com.gnomecromancer.archaeo.ui.ArchaeoPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.ToolWindowManager

class ArchaeoFunctionAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = editor != null && file != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val settings = ArchaeoSettings.instance

        if (settings.cliPath.isBlank()) {
            ArchaeoPanel.showError(project, "Claude CLI path not set. Go to Settings → Tools → Code Archaeo.")
            return
        }

        val functionName = getWordAtCaret(editor)
        if (functionName == null) {
            ArchaeoPanel.showError(project, "No symbol found at cursor.")
            return
        }

        val filePath = file.path
        val fileName = file.name

        object : Task.Backgroundable(project, "Archaeo: analyzing $functionName…", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching git history…"
                val git = GitService()
                val commits = git.getFileCommits(filePath, settings.maxCommits)

                if (commits.isEmpty()) {
                    ArchaeoPanel.showError(project, "No git history found for $fileName")
                    return
                }

                indicator.text = "Synthesizing narrative for '$functionName' (${commits.size} commits)…"
                val history = git.formatForPrompt(commits, functionName = functionName)
                val claude = ClaudeService(settings.cliPath, settings.model)

                val narrative = try {
                    claude.synthesize(history, filePath, functionName, commitCount = commits.size)
                } catch (ex: Exception) {
                    ArchaeoPanel.showError(project, "Claude API error: ${ex.message}")
                    return
                }

                val header = "# `$functionName` in $fileName\n_${commits.size} commits analyzed_"
                ArchaeoPanel.showNarrative(project, header, narrative)
            }
        }.queue()

        ToolWindowManager.getInstance(project).getToolWindow("Code Archaeo")?.show()
    }

    private fun getWordAtCaret(editor: Editor): String? {
        val doc = editor.document
        val offset = editor.caretModel.offset
        val text = doc.charsSequence
        if (offset >= text.length) return null

        var start = offset
        var end = offset

        while (start > 0 && isWordChar(text[start - 1])) start--
        while (end < text.length && isWordChar(text[end])) end++

        val word = text.subSequence(start, end).toString().trim()
        return if (word.length >= 2) word else null
    }

    private fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '_'
}
