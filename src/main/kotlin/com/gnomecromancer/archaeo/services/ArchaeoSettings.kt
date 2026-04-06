package com.gnomecromancer.archaeo.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "ArchaeoSettings",
    storages = [Storage("archaeo.xml")]
)
class ArchaeoSettings : PersistentStateComponent<ArchaeoSettings.State> {

    data class State(
        var cliPath: String = "C:\\Users\\eliwo\\.local\\bin\\claude.exe",
        var maxCommits: Int = 50,
        var model: String = "claude-sonnet-4-6",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var cliPath: String
        get() = state.cliPath
        set(value) { state.cliPath = value }

    var maxCommits: Int
        get() = state.maxCommits
        set(value) { state.maxCommits = value }

    var model: String
        get() = state.model
        set(value) { state.model = value }

    companion object {
        val instance: ArchaeoSettings
            get() = ApplicationManager.getApplication().getService(ArchaeoSettings::class.java)
    }
}
