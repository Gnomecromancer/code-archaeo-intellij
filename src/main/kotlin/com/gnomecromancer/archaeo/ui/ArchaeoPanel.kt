package com.gnomecromancer.archaeo.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.BorderLayout
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class ArchaeoPanel : JPanel(BorderLayout()) {

    private val editorPane = JEditorPane().apply {
        isEditable = false
        val kit = HTMLEditorKit()
        // Base stylesheet for readable output
        kit.styleSheet.addRule("body { font-family: sans-serif; font-size: 13pt; margin: 8px; }")
        kit.styleSheet.addRule("h3 { margin-top: 4px; margin-bottom: 6px; }")
        kit.styleSheet.addRule("p { margin-top: 0; margin-bottom: 8px; }")
        kit.styleSheet.addRule("code { font-family: monospace; background: #f0f0f0; padding: 1px 3px; }")
        editorKit = kit
        document = kit.createDefaultDocument()
    }

    init {
        add(JScrollPane(editorPane), BorderLayout.CENTER)
    }

    fun setContent(header: String, narrative: String) {
        SwingUtilities.invokeLater {
            editorPane.text = buildHtml(header, narrative)
            editorPane.caretPosition = 0
        }
    }

    fun setError(message: String) {
        SwingUtilities.invokeLater {
            editorPane.text = buildHtml("Error", "<span style='color:red'>$message</span>")
        }
    }

    private fun buildHtml(header: String, body: String): String {
        val headerHtml = if (header.isNotBlank()) "<h3>${header.escapeHtml()}</h3>" else ""
        val bodyHtml = body
            .escapeHtml()
            .replace("`([^`\n]+)`".toRegex(), "<code>$1</code>")
            .replace("\n\n", "</p><p>")
            .replace("\n", "<br/>")
            .let { "<p>$it</p>" }

        return "<html><body>$headerHtml$bodyHtml</body></html>"
    }

    private fun String.escapeHtml() = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    companion object {
        fun showNarrative(project: Project, header: String, narrative: String) {
            ApplicationManager.getApplication().invokeLater {
                getPanel(project)?.setContent(header, narrative)
            }
        }

        fun showError(project: Project, message: String) {
            ApplicationManager.getApplication().invokeLater {
                getPanel(project)?.setError(message)
            }
        }

        private fun getPanel(project: Project): ArchaeoPanel? {
            val tw = ToolWindowManager.getInstance(project).getToolWindow("Code Archaeo") ?: return null
            val content = tw.contentManager.getContent(0) ?: return null
            return content.component as? ArchaeoPanel
        }
    }
}
