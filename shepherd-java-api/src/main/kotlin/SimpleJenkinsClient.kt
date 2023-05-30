package com.github.mvysny.shepherd.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.time.Instant

internal class SimpleJenkinsClient @JvmOverloads constructor(
    private val jenkinsUrl: String = "http://localhost:8080",
    private val jenkinsUsername: String = "admin",
    private val jenkinsPassword: String = "admin"
) : Closeable {
//    private val jenkinsClient: JenkinsHttpConnection = JenkinsHttpClient(URI(jenkinsUrl), jenkinsUsername, jenkinsPassword)
//    private val jenkins: JenkinsServer = JenkinsServer(jenkinsClient)
    private val ProjectId.jenkinsJobName: String get() = id
    private val Project.jenkinsJobName: String get() = id.jenkinsJobName

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        cookieJar(BasicCookieJar()) // Jenkins Crumbs require that the session cookie is preserved between requests
        addInterceptor(BasicAuthInterceptor(jenkinsUsername, jenkinsPassword))
    } .build()

    /**
     * Starts a build manually.
     */
    fun build(id: ProjectId) {
        val crumb = getCrumb()

        val url = "$jenkinsUrl/job/${id.jenkinsJobName}/build/api/json".buildUrl()
        val request = url.buildRequest {
            post(EMPTY_REQUEST)
            crumb.applyTo(this)
        }
        okHttpClient.exec(request) {}
    }

    /**
     * Retrieves a new crumb from Jenkins. This prevents CSRF.
     */
    private fun getCrumb(): JenkinsCrumb {
        val url = "$jenkinsUrl/crumbIssuer/api/json".buildUrl()
        return okHttpClient.exec(url.buildRequest()) {
            it.json<JenkinsCrumb>(json)
        }
    }

    private fun getJobXml(project: Project): String {
        val emailNotificationSendTo = setOf("mavi@vaadin.com", project.owner.email).joinToString(" ")
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

        // you can get the job XML from e.g. http://localhost:8080/job/beverage-buddy-vok/config.xml
        val xml = """
<?xml version='1.1' encoding='UTF-8'?>
<project>
  <actions/>
  <description>${project.description.escapeXml()}. Web page: ${project.gitRepo.url.escapeXml()}. Owner: ${project.owner.toString().escapeXml()}</description>
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
        <url>${project.gitRepo.url.escapeXml()}</url>
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
/opt/shepherd/shepherd-build ${project.id.id.escapeXml()}</command>
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
        val url = "$jenkinsUrl/api/json?tree=jobs[name]".buildUrl()
        return okHttpClient.exec(url.buildRequest()) { response ->
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
            // crumbFlag=true is necessary: https://serverfault.com/questions/990224/jenkins-server-throws-403-while-accessing-rest-api-or-using-jenkins-java-client/1131973
//            jenkins.createJob(project.jenkinsJobName, xml, true)
            val crumb = getCrumb()
            val url = "$jenkinsUrl/createItem/api/json".buildUrl {
                addEncodedQueryParameter("name", project.jenkinsJobName)
            }
            val request = url.buildRequest {
                post(xml.toRequestBody("text/xml; charset=utf-8".toMediaType()))
                crumb.applyTo(this)
            }
            okHttpClient.exec(request) {}
        } else {
            log.warn("Jenkins job for ${project.id.id} already exists, updating existing instead")
            updateJob(project)
        }
    }

    /**
     * Updates Jenkins job. Fails if the job doesn't exist.
     */
    fun updateJob(project: Project) {
        val xml = getJobXml(project)
//        jenkins.updateJob(project.jenkinsJobName, xml, true)
        val crumb = getCrumb()
        val url = "$jenkinsUrl/job/${project.jenkinsJobName}/config.xml/api/json".buildUrl()
        val request = url.buildRequest {
            post(xml.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            crumb.applyTo(this)
        }
        okHttpClient.exec(request) {}
    }

    /**
     * Deletes Jenkins job for given project. Does nothing if the job doesn't exist - logs
     * a warning log instead.
     */
    fun deleteJobIfExists(id: ProjectId) {
        if (!hasJob(id)) {
            log.warn("Jenkins job ${id.jenkinsJobName} doesn't exist, not deleting Jenkins job")
            return
        }

        val crumb = getCrumb()
        val url = "$jenkinsUrl/job/${id.jenkinsJobName}/doDelete/api/json".buildUrl()
        val request = url.buildRequest {
            post(EMPTY_REQUEST)
            crumb.applyTo(this)
        }
        okHttpClient.exec(request) {}
//        jenkins.deleteJob(id.jenkinsJobName, true)
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
                    newProject.build.dockerFile != oldProject.build.dockerFile

        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    override fun close() {
//        jenkins.close()
        okHttpClient.destroy()
    }

    /**
     * Returns all jenkins jobs (=projects).
     */
    fun getJobsOverview(): List<JenkinsJob> {
        // API description: http://localhost:8080/api/
        // get all jobs: http://localhost:8080/api/json?pretty=true
        // very detailed info, NOT RECOMMENDED for production use: http://localhost:8080/api/json?pretty=true&depth=2
        // full URL: http://localhost:8080/api/json?tree=jobs[name,lastBuild[number,result,timestamp,duration,estimatedDuration]]
        val url = "$jenkinsUrl/api/json?tree=jobs[name,lastBuild[number,result,timestamp,duration,estimatedDuration]]".buildUrl()
        return okHttpClient.exec(url.buildRequest()) {
            it.json<JenkinsJobs>(json).jobs
        }
    }

    /**
     * Returns the last 10 builds for given project [id].
     */
    fun getLastBuilds(id: ProjectId): List<JenkinsBuild> {
        // general project info: http://localhost:8080/job/vaadin-boot-example-gradle/api/json?depth=2
        // http://localhost:8080/job/vaadin-boot-example-gradle/api/json?tree=builds[number,result,timestamp,duration,estimatedDuration]
        val url = "$jenkinsUrl/job/${id.jenkinsJobName}/api/json?tree=builds[number,result,timestamp,duration,estimatedDuration]".buildUrl()
        return okHttpClient.exec(url.buildRequest()) {
            it.json<JenkinsBuilds>(json).builds
        }
    }

    fun getBuildConsoleText(id: ProjectId, buildNumber: Int): String {
        // e.g. http://localhost:8080/job/vaadin-boot-example-gradle/27/consoleText
        val url = "$jenkinsUrl/job/${id.jenkinsJobName}/$buildNumber/logText/progressiveText".buildUrl()
        return okHttpClient.exec(url.buildRequest()) { it.string() }
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
    fun applyTo(r: Request.Builder) {
        r.addHeader(crumbRequestField, crumb)
    }
}
