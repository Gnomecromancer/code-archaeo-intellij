package com.gnomecromancer.archaeo.actions

import com.gnomecromancer.archaeo.services.ArchaeoSettings
import com.gnomecromancer.archaeo.services.ClaudeService
import com.gnomecromancer.archaeo.services.GitService
import com.gnomecromancer.archaeo.ui.ArchaeoPanel
import com.gnomecromancer.archaeo.ui.ArchaeoToolWindowFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.ToolWindowManager

class ArchaeoFileAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && !file.isDirectory
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val settings = ArchaeoSettings.instance

        if (settings.apiKey.isBlank()) {
            ArchaeoPanel.showError(project, "No API key configured. Go to Settings → Tools → Code Archaeo.")
            return
        }

        val filePath = file.path
        val fileName = file.name

        object : Task.Backgroundable(project, "Archaeo: analyzing $fileName…", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching git history…"
                val git = GitService()
                val commits = git.getFileCommits(filePath, settings.maxCommits)

                if (commits.isEmpty()) {
                    ArchaeoPanel.showError(project, "No git history found for $fileName")
                    return
                }

                indicator.text = "Synthesizing narrative (${commits.size} commits)…"
                val history = git.formatForPrompt(commits, functionName = null)
                val claude = ClaudeService(settings.apiKey, settings.model)

                val narrative = try {
                    claude.synthesize(history, filePath, functionName = null, commitCount = commits.size)
                } catch (ex: Exception) {
                    ArchaeoPanel.showError(project, "Claude API error: ${ex.message}")
                    return
                }

                val header = "# $fileName\n_${commits.size} commits analyzed_"
                ArchaeoPanel.showNarrative(project, header, narrative)
            }
        }.queue()

        // Open the tool window
        ToolWindowManager.getInstance(project).getToolWindow("Code Archaeo")?.show()
    }
}
