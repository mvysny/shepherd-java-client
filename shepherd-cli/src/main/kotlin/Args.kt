@file:OptIn(ExperimentalCli::class)

import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.shepherd.api.LinuxShepherdClient
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ShepherdClient
import kotlinx.cli.*

val host = "v-herd.eu"

/**
 * Parsed command-line parameters.
 * @property fake use fake client
 * @property command which command was called
 * @property project The project ID to control via the subcommands. Some subcommands do not require this.
 */
data class Args(
    val fake: Boolean,
    val command: Command,
    val project: ProjectId?,
    val deleteSubcommand: DeleteSubcommand,
    val createSubcommand: CreateSubcommand,
    val updateSubcommand: UpdateSubcommand
) {

    fun createClient(): ShepherdClient = if (fake) FakeShepherdClient else LinuxShepherdClient()

    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("shepherd-cli")
            val fake by parser.option(ArgType.Boolean, "fake", description = "Use fake client which provides fake data. Good for testing.").default(false)
            val project by parser.option(ArgType.String, "project", shortName = "p", description = "The project ID to control via the subcommands. Some subcommands do not require this.")

            val deleteSubcommand = DeleteSubcommand()
            val createSubcommand = CreateSubcommand()
            val updateSubcommand = UpdateSubcommand()
            parser.subcommands(ListProjectSubcommand(), ShowProjectSubcommand(), LogsSubcommand(), createSubcommand, deleteSubcommand, MetricsSubcommand(), updateSubcommand)
            val parserResult = parser.parse(args)
            val commandName = parserResult.commandName.takeUnless { it == parser.programName }
            val cmd = Command.values().firstOrNull { it.argName == commandName }
            require(cmd != null) { "Invalid command '$commandName'. Please run with -h to learn the proper usage" }

            return Args(
                fake,
                cmd,
                project?.let { ProjectId(it) },
                deleteSubcommand,
                createSubcommand,
                updateSubcommand
            )
        }
    }
}

class ListProjectSubcommand: Subcommand(Command.ListProjects.argName, "List all projects: their IDs and a quick info about the project: the description, the owner and such") {
    override fun execute() {} // implemented elsewhere, since this function doesn't have access to
}
class ShowProjectSubcommand: Subcommand(Command.ShowProject.argName, "Show project information as a pretty-printed JSON") {
    override fun execute() {}
}
class LogsSubcommand: Subcommand(Command.Logs.argName, "Prints the runtime logs of the main pod of given project") {
    override fun execute() {}
}
class DeleteSubcommand: Subcommand(Command.Delete.argName, "Deletes a project. Dangerous operation, requires -y to confirm") {
    val yes by option(ArgType.Boolean, "yes", "y", "Confirms that you're really sure to delete the project").required()
    override fun execute() {}
}
class CreateSubcommand: Subcommand(Command.Create.argName, "Creates a new project. Fails if the project already exists.") {
    val jsonFile by option(ArgType.String, "file", "f", "The JSON file describing the project").required()
    override fun execute() {}
}
class MetricsSubcommand: Subcommand(Command.Metrics.argName, "Shows basic metrics of the main app pod.") {
    override fun execute() {}
}
class UpdateSubcommand: Subcommand(Command.Update.argName, "Updates a project with the new configuration. Fails if the project doesn't exist yet.") {
    val jsonFile by option(ArgType.String, "file", "f", "The JSON file describing the project").required()
    override fun execute() {}
}
