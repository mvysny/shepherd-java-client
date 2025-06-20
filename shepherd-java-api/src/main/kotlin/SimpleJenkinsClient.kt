package com.github.mvysny.shepherd.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.CookieManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.Instant

/**
 * The Jenkins API documentation is offered by Jenkins itself: go to the running
 * Jenkins instance and search the "REST API" link at the bottom of the screen.
 * @property jenkinsUrl the URL where Jenkins can be controlled
 * @property jenkinsUsername username
 * @property jenkinsPassword password
 * @property shepherdHome where Shepherd is installed, `/opt/shepherd` for [Shepherd Kubernetes](https://github.com/mvysny/shepherd), `/opt/shepherd-traefik` for [Shepherd Traefik](https://github.com/mvysny/shepherd-traefik).
 */
internal class SimpleJenkinsClient @JvmOverloads constructor(
    private val jenkinsUrl: String = "http://localhost:8080",
    private val jenkinsUsername: String = "admin",
    private val jenkinsPassword: String = "admin",
    private val shepherdHome: String = "/opt/shepherd"
) {
    private val ProjectId.jenkinsJobName: String get() = id
    private val Project.jenkinsJobName: String get() = id.jenkinsJobName

    private val httpClient: HttpClient = HttpClient.newBuilder().apply {
        followRedirects(HttpClient.Redirect.NORMAL)
        cookieHandler(CookieManager())  // Jenkins Crumbs require that the session cookie is preserved between requests
    } .build()

    /**
     * Starts a build manually.
     */
    fun build(id: ProjectId) {
        log.info("Running Jenkins build of ${id.jenkinsJobName}")

        val url = URI("$jenkinsUrl/job/${id.jenkinsJobName}/build/api/json")
        val request = url.buildRequest {
            POST(BodyPublishers.noBody())
            basicAuth(jenkinsUsername, jenkinsPassword)
            crumb()
        }
        httpClient.exec(request) {}
    }

    /**
     * Retrieves a new crumb from Jenkins. This prevents CSRF.
     */
    private fun getCrumb(): JenkinsCrumb {
        val url = URI("$jenkinsUrl/crumbIssuer/api/json")
        val request = url.buildRequest {
            basicAuth(jenkinsUsername, jenkinsPassword)
        }
        return httpClient.exec(request) {
            it.json<JenkinsCrumb>(json)
        }
    }

    private fun HttpRequest.Builder.crumb() {
        getCrumb().applyTo(this)
    }

    internal fun getJobXml(project: Project): String {
        val emailNotificationSendTo = project.emailNotificationSendTo.joinToString(" ")
        val envVars = mutableListOf(
            "export BUILD_MEMORY=${project.build.resources.memoryMb}m",
            "export CPU_QUOTA=${(project.build.resources.cpu.toDouble() * 100000).toInt()}"
        )
        if (project.build.buildArgs.isNotEmpty()) {
            // don't wrap $v in "" - --build-arg can't handle that and e.g. Vaadin Key won't be accepted.
            val buildArgs: String =
                project.build.buildArgs.entries.joinToString(" ") { (k, v) -> """--build-arg $k=$v""" }
            envVars.add("export BUILD_ARGS='$buildArgs'")
        }
        if (project.build.dockerFile != null) {
            envVars.add("export DOCKERFILE=${project.build.dockerFile}")
        }
        val credentials = if (project.gitRepo.credentialsID == null) {
            ""
        } else {
            "\n        <credentialsId>${project.gitRepo.credentialsID.escapeXml()}</credentialsId>"
        }

        // you can get the job XML from e.g. http://localhost:8080/job/beverage-buddy-vok/config.xml
        val xml = """
<?xml version='1.1' encoding='UTF-8'?>
<project>
  <actions/>
  <description>${project.description.escapeXml()}. Web page: ${project.resolveWebpage().escapeXml()}. Owner: ${project.owner.toString().escapeXml()}</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <jenkins.model.BuildDiscarderProperty>
      <strategy class="hudson.tasks.LogRotator">
        <daysToKeep>3</daysToKeep>
        <numToKeep>-1</numToKeep>
        <artifactDaysToKeep>-1</artifactDaysToKeep>
        <artifactNumToKeep>-1</artifactNumToKeep>
      </strategy>
    </jenkins.model.BuildDiscarderProperty>
  </properties>
  <scm class="hudson.plugins.git.GitSCM" plugin="git">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>${project.gitRepo.url.escapeXml()}</url>$credentials
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/${project.gitRepo.branch.escapeXml()}</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="empty-list"/>
    <extensions/>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec>H/5 * * * *</spec>
      <ignorePostCommitHooks>false</ignorePostCommitHooks>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>${envVars.joinToString("\n").escapeXml()}
${shepherdHome}/shepherd-build ${project.id.id.escapeXml()}</command>
      <configuredLocalRules/>
    </hudson.tasks.Shell>
  </builders>
  <publishers>
    <hudson.tasks.Mailer plugin="mailer">
      <recipients>${emailNotificationSendTo.escapeXml()}</recipients>
      <dontNotifyEveryUnstableBuild>false</dontNotifyEveryUnstableBuild>
      <sendToIndividuals>false</sendToIndividuals>
    </hudson.tasks.Mailer>
  </publishers>
  <buildWrappers>
    <hudson.plugins.timestamper.TimestamperBuildWrapper plugin="timestamper"/>
    <hudson.plugins.build__timeout.BuildTimeoutWrapper plugin="build-timeout">
      <strategy class="hudson.plugins.build_timeout.impl.AbsoluteTimeOutStrategy">
        <timeoutMinutes>15</timeoutMinutes>
      </strategy>
      <operationList/>
    </hudson.plugins.build__timeout.BuildTimeoutWrapper>
  </buildWrappers>
</project>            
        """.trim()
        return xml
    }

    private fun getJobNames(): Set<String> {
        val url = URI("$jenkinsUrl/api/json?tree=jobs[name]")
        val request = url.buildRequest {
            basicAuth(jenkinsUsername, jenkinsPassword)
        }
        return httpClient.exec(request) { response ->
            val jobs: JenkinsJobNames = response.json<JenkinsJobNames>(json)
            jobs.jobs.map { it.name } .toSet()
        }
    }

    private fun hasJob(id: ProjectId) = getJobNames().contains(id.jenkinsJobName)

    /**
     * Creates a new Jenkins job for given project, or updates existing job if it already exists.
     */
    fun createJob(project: Project) {
        val xml = getJobXml(project)
        if (!hasJob(project.id)) {
            log.info("Creating Jenkins job ${project.jenkinsJobName}")
            val url = URI("$jenkinsUrl/createItem/api/json?name=${project.jenkinsJobName}")
            val request = url.buildRequest {
                POST(BodyPublishers.ofString(xml))
                header("Content-type", "text/xml; charset=utf-8");
                basicAuth(jenkinsUsername, jenkinsPassword)
                // crumbFlag=true is necessary: https://serverfault.com/questions/990224/jenkins-server-throws-403-while-accessing-rest-api-or-using-jenkins-java-client/1131973
                crumb()
            }
            httpClient.exec(request) {}
        } else {
            log.warn("Jenkins job for ${project.id.id} already exists, updating existing instead")
            updateJob(project)
        }
    }

    /**
     * Updates Jenkins job. Fails if the job doesn't exist.
     */
    fun updateJob(project: Project) {
        log.info("Updating Jenkins job ${project.jenkinsJobName}")

        val xml = getJobXml(project)
        val url = URI("$jenkinsUrl/job/${project.jenkinsJobName}/config.xml/api/json")
        val request = url.buildRequest {
            POST(BodyPublishers.ofString(xml))
            header("Content-type", "text/xml; charset=utf-8");
            basicAuth(jenkinsUsername, jenkinsPassword)
            crumb()
        }
        httpClient.exec(request) {}
    }

    /**
     * Deletes Jenkins job for given project. Does nothing if the job doesn't exist - logs
     * a warning log instead.
     *
     * All ongoing builds of the project are automatically canceled.
     *
     * The function returns only after all the builds are canceled and the project is deleted.
     */
    fun deleteJobIfExists(id: ProjectId) {
        if (!hasJob(id)) {
            log.warn("Jenkins job ${id.jenkinsJobName} doesn't exist, not deleting Jenkins job")
            return
        }

        log.info("Deleting Jenkins job ${id.jenkinsJobName}. This also cancels all ongoing builds of this project.")
        // deletes the job (=project in Jenkins terminology), canceling any ongoing builds of this job.
        val url = URI("$jenkinsUrl/job/${id.jenkinsJobName}/doDelete/api/json")
        val request = url.buildRequest {
            POST(BodyPublishers.noBody())
            basicAuth(jenkinsUsername, jenkinsPassword)
            crumb()
        }
        httpClient.exec(request) {}
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(SimpleJenkinsClient::class.java)

        private fun String.escapeXml(): String = this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "&apos;")

        /**
         * Checks whether a full Jenkins project rebuild is necessary after changes in the project config file.
         */
        fun needsProjectRebuild(newProject: Project, oldProject: Project): Boolean =
            newProject.build.buildArgs != oldProject.build.buildArgs ||
                    newProject.build.dockerFile != oldProject.build.dockerFile ||
                    // also this: if e.g. build memory increases from 1024 to 2048, we want a full rebuild: the old project might have failed to build because there was not enough memory
                    newProject.build.resources.memoryMb != oldProject.build.resources.memoryMb ||
                    newProject.gitRepo != oldProject.gitRepo ||
                    // if the project fails to build, new owner needs to know.
                    newProject.owner != oldProject.owner

        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    /**
     * Returns all jenkins jobs (=projects).
     */
    fun getJobsOverview(): List<JenkinsJob> {
        // API description: http://localhost:8080/api/
        // get all jobs: http://localhost:8080/api/json?pretty=true
        // very detailed info, NOT RECOMMENDED for production use: http://localhost:8080/api/json?pretty=true&depth=2
        // full URL: http://localhost:8080/api/json?tree=jobs[name,lastBuild[number,result,timestamp,duration,estimatedDuration]]
        val url = URI("$jenkinsUrl/api/json?tree=jobs[name,lastBuild[number,result,timestamp,duration,estimatedDuration]]")
        val request = url.buildRequest {
            basicAuth(jenkinsUsername, jenkinsPassword)
        }
        return httpClient.exec(request) {
            it.json<JenkinsJobs>(json).jobs
        }
    }

    /**
     * Returns the last 30 builds for given project [id]. The builds are sorted by [JenkinsBuild.number] ascending.
     * @throws java.io.FileNotFoundException if the project doesn't exist.
     */
    fun getLastBuilds(id: ProjectId): List<JenkinsBuild> {
        // general project info: http://localhost:8080/job/vaadin-boot-example-gradle/api/json?depth=2
        // http://localhost:8080/job/vaadin-boot-example-gradle/api/json?tree=builds[number,result,timestamp,duration,estimatedDuration]
        val url = URI("$jenkinsUrl/job/${id.jenkinsJobName}/api/json?tree=builds[number,result,timestamp,duration,estimatedDuration]")
        val request = url.buildRequest {
            basicAuth(jenkinsUsername, jenkinsPassword)
        }
        val builds: List<JenkinsBuild> = httpClient.exec(request) {
            it.json<JenkinsBuilds>(json).builds
        }
        return builds.sortedBy { it.number } .takeLast(30)
    }

    fun getBuildConsoleText(id: ProjectId, buildNumber: Int): String {
        // e.g. http://localhost:8080/job/vaadin-boot-example-gradle/27/consoleText
        val url = URI("$jenkinsUrl/job/${id.jenkinsJobName}/$buildNumber/logText/progressiveText")
        val request = url.buildRequest {
            basicAuth(jenkinsUsername, jenkinsPassword)
        }
        return httpClient.send(request, BodyHandlers.ofString())
            .checkOk()
            .body()
    }
}

@Serializable
internal data class JenkinsJobs(
    val jobs: List<JenkinsJob>
)

/**
 * A Jenkins job (=project); [name] equals to [ProjectId.id].
 */
@Serializable
internal data class JenkinsJob(
    val name: String,
    val lastBuild: JenkinsBuild?
)

@Serializable
internal data class JenkinsJobNames(
    val jobs: List<JenkinsJobName>
)

/**
 * A Jenkins job (=project); [name] equals to [ProjectId.id].
 */
@Serializable
internal data class JenkinsJobName(
    val name: String
)

@Serializable
internal data class JenkinsBuilds(
    val builds: List<JenkinsBuild>
)

/**
 * @property number the build number, starts at 1, second build is number 2, etc.
 * @property result the build result. jenkins passes in null when the project is still building.
 * @property timestamp when the project started to build. Millis since epoch.
 * @property duration build duration, in millis.
 * @property estimatedDuration build estimated duration, in millis.
 */
@Serializable
internal data class JenkinsBuild(
    val number: Int,
    val result: BuildResult = BuildResult.BUILDING,
    val timestamp: Long,
    val duration: Long,
    val estimatedDuration: Long
) {
    fun toBuild(): Build = Build(number, Duration.ofMillis(duration), Duration.ofMillis(estimatedDuration), Instant.ofEpochMilli(timestamp), result)
}

@Serializable
internal data class JenkinsCrumb(
    val crumb: String,
    val crumbRequestField: String
) {
    fun applyTo(r: HttpRequest.Builder) {
        r.header(crumbRequestField, crumb)
    }
}
