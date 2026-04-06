package com.gnomecromancer.archaeo.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

class ArchaeoPanel : JPanel(BorderLayout()) {

    private val textPane = JTextPane().apply {
        isEditable = false
        contentType = "text/html"
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
        font = Font("JetBrains Mono", Font.PLAIN, 13).let {
            // fallback to monospaced if JetBrains Mono not available
            if (it.family == "JetBrains Mono") it else Font(Font.MONOSPACED, Font.PLAIN, 13)
        }
        background = null
    }

    init {
        add(JScrollPane(textPane), BorderLayout.CENTER)
    }

    fun setContent(header: String, narrative: String) {
        val html = buildHtml(header, narrative)
        SwingUtilities.invokeLater {
            textPane.contentType = "text/html"
            textPane.text = html
            textPane.caretPosition = 0
        }
    }

    fun setLoading(message: String) {
        SwingUtilities.invokeLater {
            textPane.contentType = "text/html"
            textPane.text = buildHtml("", "<em>$message</em>")
        }
    }

    fun setError(message: String) {
        SwingUtilities.invokeLater {
            textPane.contentType = "text/html"
            textPane.text = buildHtml("Error", "<span style='color:#e06c75'>$message</span>")
        }
    }

    private fun buildHtml(header: String, body: String): String {
        val headerHtml = if (header.isNotBlank()) "<h3>${header.replace("`", "<code>").replace("</code>", "</code>")}</h3>" else ""
        // Convert basic markdown to HTML
        val bodyHtml = body
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("`([^`]+)`".toRegex(), "<code>$1</code>")
            .replace("\n\n", "</p><p>")
            .replace("\n", "<br>")
            .let { "<p>$it</p>" }

        return """
            <html><body style='font-family: sans-serif; padding: 8px; font-size: 13px;'>
            $headerHtml
            $bodyHtml
            </body></html>
        """.trimIndent()
    }

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
