@file:OptIn(ExperimentalCli::class)

import com.github.mvysny.shepherd.api.FakeClient
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ShepherdClient
import kotlinx.cli.*

/**
 * Parsed command-line parameters.
 * @property fake use fake client
 * @property command which command was called
 * @property project The project ID to control via the subcommands. Some subcommands do not require this.
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
            val fake by parser.option(ArgType.Boolean, "fake", description = "Use fake client which provides fake data. Good for testing.").default(false)
            val project by parser.option(ArgType.String, "project", shortName = "p", description = "The project ID to control via the subcommands. Some subcommands do not require this.")

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

class ListProjects: Subcommand(Command.ListProjects.argName, "List all projects: their IDs and a quick info about the project: the description, the owner and such") {
    override fun execute() {} // implemented elsewhere, since this function doesn't have access to
}
class ShowProject: Subcommand(Command.ShowProject.argName, "Show project information as a pretty-printed JSON") {
    override fun execute() {}
}
