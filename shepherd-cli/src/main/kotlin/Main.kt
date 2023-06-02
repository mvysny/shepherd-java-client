import com.github.mvysny.shepherd.api.Build
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ShepherdClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneId
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val a = Args.parse(args)
    a.createClient().use { client ->
        a.command.run(a, client)
    }
}

enum class Command(val argName: String) {
    /**
     * List all projects: their IDs and a quick info about the project: the description, the owner and such.
     */
    ListProjects("list") {
        override fun run(args: Args, client: ShepherdClient) {
            val projects = client.getAllProjects()
            projects.forEach { v ->
                println("${v.project.id.id}: ${v.project.description} (${v.project.owner})")
                println("   Home Page: ${v.project.resolveWebpage()}")
                println("   Sources: ${v.project.gitRepo.url} , Branch: '${v.project.gitRepo.branch}'")
                println("   Published at: ${v.getPublishedURLs(host)}")
                println("   Last Build: ${v.lastBuild?.formatShort()}")
                println()
            }
            if (projects.isEmpty()) {
                println("No projects registered.")
            }
        }
    },

    /**
     * The `show` command, shows all information about certain project as a JSON.
     */
    ShowProject("show") {
        override fun run(args: Args, client: ShepherdClient) {
            val project = client.getProjectInfo(requireProjectId(args))
            val json = Json { prettyPrint = true }
            println(json.encodeToString(project))
        }
    },

    /**
     * The `logs` command, prints the runtime logs of the main pod of given project.
     */
    Logs("logs") {
        override fun run(args: Args, client: ShepherdClient) {
            println(client.getRunLogs(requireProjectId(args)))
        }
    },

    /**
     * The `delete` command, Deletes a project. Dangerous operation, requires -y to confirm.
     */
    Delete("delete") {
        override fun run(args: Args, client: ShepherdClient) {
            val pid = requireProjectId(args)
            require(args.deleteSubcommand.yes) { "Pass in -y to confirm that you really want to delete $pid" }
            client.deleteProject(pid)
        }
    },

    /**
     * The `create` command, creates a new project. Fails if the project already exists.
     */
    Create("create") {
        override fun run(args: Args, client: ShepherdClient) {
            val project = Project.loadFromFile(Path(args.createSubcommand.jsonFile))
            client.createProject(project)
        }
    },

    /**
     * The `metrics` command, shows basic metrics of the main app pod.
     */
    Metrics("metrics") {
        override fun run(args: Args, client: ShepherdClient) {
            val pid = requireProjectId(args)
            val metrics = client.getRunMetrics(pid)
            println("${pid.id} usage: ${metrics.cpu * 100}% CPU, ${metrics.memoryMb}Mi RAM")
        }
    },

    /**
     * The `update` command, updates a project with the new configuration. Fails if the project doesn't exist yet.
     */
    Update("update") {
        override fun run(args: Args, client: ShepherdClient) {
            val project = Project.loadFromFile(Path(args.updateSubcommand.jsonFile))
            client.updateProject(project)
        }
    },

    /**
     * The `builds` command, lists last 10 builds of given project.
     */
    Builds("builds") {
        override fun run(args: Args, client: ShepherdClient) {
            val pid = requireProjectId(args)
            client.getLastBuilds(pid).forEach { build ->
                println("Build ${build.formatShort()}")
            }
        }
    },

    /**
     * The `buildlog` command, prints the build console log of given project.
     */
    BuildLog("buildlog") {
        override fun run(args: Args, client: ShepherdClient) {
            val pid = requireProjectId(args)
            var buildNumber = args.buildLogSubcommand.buildNumber
            if (buildNumber == null) {
                buildNumber = client.getLastBuilds(pid).maxOfOrNull { it.number }
            }
            require(buildNumber != null) { "Project ${pid.id} has no builds yet" }

            println(client.getBuildLog(pid, buildNumber))
        }
    },

    /**
     * The `stats` command, prints Shepherd+host machine runtime statistics.
     */
    Stats("stats") {
        override fun run(args: Args, client: ShepherdClient) {
            val stats = client.getStats()
            println("Registered projects: ${stats.projectCount}")
            println("Project Memory Quotas:")
            println("   Quota allocated for project runtimes: ${stats.projectMemoryStats.projectRuntimeQuota}")
            println("   Total project quota (runtime+builds): ${stats.projectMemoryStats.totalQuota}")
            println("Builder: max concurrent build jobs: ${stats.concurrentJenkinsBuilders}")
            println("Host Memory: Mem: ${stats.hostMemoryStats.memory}; Swap: ${stats.hostMemoryStats.swap}")
            println("Disk Usage: ${stats.diskUsage}")
        }
    },
    ;

    /**
     * Runs the command, with given [args] over given [client].
     */
    abstract fun run(args: Args, client: ShepherdClient)

    /**
     * Utility function to get the [Args.project], failing if it's missing on the command line.
     */
    protected fun requireProjectId(args: Args): ProjectId {
        require(args.project != null) { "This command requires the project ID" }
        return args.project
    }
}

fun Build.formatShort(): String = "#${number} [${outcome}]: Started ${buildStarted.atZone(ZoneId.systemDefault())}, duration $duration"
