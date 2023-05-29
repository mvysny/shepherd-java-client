package com.github.mvysny.shepherd.api

import com.offbytwo.jenkins.JenkinsServer
import com.offbytwo.jenkins.client.JenkinsHttpClient
import com.offbytwo.jenkins.client.JenkinsHttpConnection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

public class SimpleJenkinsClient @JvmOverloads constructor(
    private val jenkinsClient: JenkinsHttpConnection = JenkinsHttpClient(URI("http://localhost:8080"), "admin", "admin")
) : Closeable {
    private val jenkins: JenkinsServer = JenkinsServer(jenkinsClient)
    private val ProjectId.jenkinsJobName: String get() = id
    private val Project.jenkinsJobName: String get() = id.jenkinsJobName

    /**
     * Starts a build manually.
     */
    public fun build(id: ProjectId) {
        jenkins.getJob(id.jenkinsJobName).build(true)
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

    private fun hasJob(id: ProjectId) = jenkins.jobs.keys.contains(id.jenkinsJobName)

    /**
     * Creates a new Jenkins job for given project, or updates existing job if it already exists.
     */
    public fun createJob(project: Project) {
        val xml = getJobXml(project)
        if (!hasJob(project.id)) {
            // crumbFlag=true is necessary: https://serverfault.com/questions/990224/jenkins-server-throws-403-while-accessing-rest-api-or-using-jenkins-java-client/1131973
            jenkins.createJob(project.jenkinsJobName, xml, true)
        } else {
            log.warn("Jenkins job for ${project.id.id} already exists, updating existing instead")
            updateJob(project)
        }
    }

    /**
     * Updates Jenkins job. Fails if the job doesn't exist.
     */
    public fun updateJob(project: Project) {
        val xml = getJobXml(project)
        jenkins.updateJob(project.jenkinsJobName, xml, true)
    }

    /**
     * Deletes Jenkins job for given project. Does nothing if the job doesn't exist - logs
     * a warning log instead.
     */
    public fun deleteJobIfExists(id: ProjectId) {
        jenkins.deleteJob(id.jenkinsJobName, true)
    }

    public companion object {
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
        public fun needsProjectRebuild(newProject: Project, oldProject: Project): Boolean =
            newProject.build.buildArgs != oldProject.build.buildArgs ||
                    newProject.build.dockerFile != oldProject.build.dockerFile
    }

    override fun close() {
        jenkins.close()
    }

    public fun getJobsOverview(): List<JenkinsJob> {
        val result = jenkinsClient.get("?tree=jobs[name,lastBuild[result,timestamp]]")
        return Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<JenkinsJobs>(result).jobs
    }
}

@Serializable
internal data class JenkinsJobs(
    val jobs: List<JenkinsJob>
)

@Serializable
public data class JenkinsJob(
    val name: String,
    val lastBuild: JenkinsBuild?
)

/**
 * @property result the build result. jenkins passes in null when the project is still building.
 * @property timestamp when the project started to build.
 */
@Serializable
public data class JenkinsBuild(
    val result: BuildResult = BuildResult.BUILDING,
    val timestamp: Long
) {
    /**
     * true if the build is still ongoing, false if the build has finished building.
     */
    val ongoing: Boolean get() = result == BuildResult.BUILDING

    val buildStarted: ZonedDateTime get() = Instant.ofEpochMilli(timestamp).atZone(
        ZoneId.systemDefault())
}
