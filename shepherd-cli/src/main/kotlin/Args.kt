@file:OptIn(ExperimentalCli::class)

import com.github.mvysny.shepherd.api.FakeClient
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ShepherdClient
import kotlinx.cli.*

/**
 * @property fake use fake client
 * @property command which command was called
 * @property listProjects
 */
data class Args(
    val fake: Boolean,
    val command: Command,
    val project: ProjectId?
) {

    fun createClient(): ShepherdClient = FakeClient

    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("shepherd-cli")
            val fake by parser.option(ArgType.Boolean, "fake", description = "Use fake client").default(false)
            val project by parser.option(ArgType.String, "project", shortName = "p", description = "Project ID")

            parser.subcommands(ListProjects(), ShowProject())
            val parserResult = parser.parse(args)
            val commandName = parserResult.commandName.takeUnless { it == parser.programName }
            val cmd = Command.values().firstOrNull { it.argName == commandName }
            require(cmd != null) { "Invalid command '$commandName'. Please run with -h to learn the proper usage" }

            return Args(
                fake,
                cmd,
                project?.let { ProjectId(it) }
            )
        }
    }
}

class ListProjects: Subcommand(Command.ListProjects.argName, "List all projects") {
    override fun execute() {}
}
class ShowProject: Subcommand(Command.ShowProject.argName, "Show project info") {
    override fun execute() {}
}
