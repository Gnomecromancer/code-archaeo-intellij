package com.gnomecromancer.archaeo.ui

import com.gnomecromancer.archaeo.services.ArchaeoSettings
import com.intellij.openapi.options.Configurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class ArchaeoSettingsConfigurable : Configurable {

    private lateinit var cliPathField: JTextField
    private lateinit var maxCommitsField: JSpinner
    private lateinit var modelField: JComboBox<String>

    override fun getDisplayName() = "Code Archaeo"

    override fun createComponent(): JPanel {
        val settings = ArchaeoSettings.instance

        cliPathField = JTextField(settings.cliPath, 40)
        maxCommitsField = JSpinner(SpinnerNumberModel(settings.maxCommits, 5, 500, 5))
        modelField = JComboBox(arrayOf("claude-sonnet-4-6", "claude-opus-4-6", "claude-haiku-4-5-20251001")).apply {
            selectedItem = settings.model
        }

        return JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 4, 4, 4)
            }

            fun label(text: String, x: Int, y: Int) {
                gbc.gridx = x; gbc.gridy = y; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
                add(JLabel(text), gbc)
            }
            fun field(comp: JComponent, x: Int, y: Int) {
                gbc.gridx = x; gbc.gridy = y; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
                add(comp, gbc)
            }

            label("Claude CLI path:", 0, 0)
            field(cliPathField, 1, 0)

            label("Model:", 0, 1)
            field(modelField, 1, 1)

            label("Max commits:", 0, 2)
            field(maxCommitsField, 1, 2)

            // hint label
            gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
            add(JLabel("<html><small>Uses your Pro subscription via the Claude CLI — no API key needed.</small></html>"), gbc)

            // Filler
            gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 1.0; gbc.gridwidth = 2
            gbc.fill = GridBagConstraints.BOTH
            add(JPanel(), gbc)
        }
    }

    override fun isModified(): Boolean {
        val s = ArchaeoSettings.instance
        return cliPathField.text != s.cliPath
            || maxCommitsField.value as Int != s.maxCommits
            || modelField.selectedItem as String != s.model
    }

    override fun apply() {
        val s = ArchaeoSettings.instance
        s.cliPath = cliPathField.text
        s.maxCommits = maxCommitsField.value as Int
        s.model = modelField.selectedItem as String
    }

    override fun reset() {
        val s = ArchaeoSettings.instance
        cliPathField.text = s.cliPath
        maxCommitsField.value = s.maxCommits
        modelField.selectedItem = s.model
    }
}
