import com.github.mvysny.shepherd.api.ShepherdClient

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
    };
    abstract fun run(args: Args, client: ShepherdClient)
}
