package io.paddle.idea.execution

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import io.paddle.idea.PaddleManager
import io.paddle.idea.execution.cmdline.PaddleCommandLine
import io.paddle.idea.execution.providers.*
import io.paddle.plugin.python.tasks.run.RunTask
import io.paddle.project.PaddleProjectProvider
import java.io.File
import java.util.*

class PaddleRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    ExternalSystemRunConfiguration(PaddleManager.ID, project, factory, name) {

    init {
        isDebugServerProcess = true
        isReattachDebugProcess = true
    }

    var rawCommandLine: String
        get() {
            val commandLine = StringJoiner(" ")
            for (taskName in settings.taskNames) {
                commandLine.add(taskName)
            }
            return commandLine.toString()
        }
        set(value) {
            commandLine = PaddleCommandLine.parse(value)
        }

    var commandLine: PaddleCommandLine
        get() = PaddleCommandLine.parse(rawCommandLine)
        set(value) {
            settings.taskNames = value.tasksAndArguments.toList()
        }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        if (env.runProfile !is PaddleRunConfiguration) return null

        val runProfile = env.runProfile as PaddleRunConfiguration
        val moduleDir = runProfile.settings.externalProjectPath?.let { File(it) } ?: return null
        val rootDir = env.project.basePath?.let { File(it) } ?: return null

        val taskName = (env.runProfile as PaddleRunConfiguration).settings.taskNames.first()

        val paddleProject = PaddleProjectProvider.getInstance(rootDir).getProject(moduleDir)
        val task = paddleProject?.tasks?.get(taskName) ?: return null
        val ctx = PaddleTaskRunProfileStateContext(moduleDir, rootDir, env, this)

        return when (task) {
            is RunTask -> PaddleTaskRunProfileStateProvider.findInstance(PythonScriptCommandLineStateProvider::class.java)?.getState(task, ctx)
            else -> ExternalSystemRunnableState(settings, project, ToolWindowId.DEBUG == executor.id, this, env)
        }
    }
}
