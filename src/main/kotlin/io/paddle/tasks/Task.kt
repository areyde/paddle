package io.paddle.tasks

import io.paddle.terminal.TerminalUI
import io.paddle.utils.Hashable
import io.paddle.utils.hashable

abstract class Task {
    abstract val id: String

    open val dependencies: List<Task> = emptyList()

    open val inputs: List<Hashable> = emptyList()
    open val outputs: List<Hashable> = emptyList()

    abstract fun act()

    private val isIncremental: Boolean
        get() = inputs.isNotEmpty()

    private fun isUpToDate(): Boolean {
        return inputs.isNotEmpty() && IncrementalCache.isUpToDate(id, inputs.hashable(), outputs.hashable()) && dependencies.all { it.isUpToDate() }
    }

    fun run() {
        if (isUpToDate()) {
            TerminalUI.echoln("> Task :${id}: ${TerminalUI.colored("UP-TO-DATE", TerminalUI.Color.GREEN)}")
            return
        }

        for (dep in dependencies) {
            dep.run()
        }

        TerminalUI.echoln("> Task :${id}: ${TerminalUI.colored("EXECUTE", TerminalUI.Color.YELLOW)}")

        act()

        if (isIncremental) {
            IncrementalCache.update(id, inputs.hashable(), outputs.hashable())
        }
    }
}
