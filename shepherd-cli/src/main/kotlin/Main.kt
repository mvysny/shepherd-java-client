import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ShepherdClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val a = Args.parse(args)
    a.command.run(a, a.createClient())
}

enum class Command(val argName: String) {
    ListProjects("list") {
        override fun run(args: Args, client: ShepherdClient) {
            client.getAllProjects().forEach { projectId ->
                val project = client.getProjectInfo(projectId)
                println("${projectId.id}: ${project.description}; ${project.gitRepo} (${project.owner})")
            }
        }
    },
    ShowProject("show") {
        override fun run(args: Args, client: ShepherdClient) {
            val project = client.getProjectInfo(requireProjectId(args))
            val json = Json { prettyPrint = true }
            println(json.encodeToString(project))
        }
    }
    ;
    abstract fun run(args: Args, client: ShepherdClient)

    protected fun requireProjectId(args: Args): ProjectId {
        require(args.project != null) { "This command requires the project ID" }
        return args.project
    }
}
