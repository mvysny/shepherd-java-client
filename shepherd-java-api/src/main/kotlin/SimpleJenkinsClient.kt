package com.github.mvysny.shepherd.api

import com.offbytwo.jenkins.JenkinsServer
import java.net.URI

public class SimpleJenkinsClient(
    private val jenkins: JenkinsServer = JenkinsServer(URI("http://localhost:8080"), "admin", "admin")
) {
    private val ProjectId.jenkinsJobName: String get() = id

    /**
     * Starts a build manually.
     */
    public fun build(id: ProjectId) {
        jenkins.getJob(id.jenkinsJobName).build()
    }

    private fun getJobXml(project: Project): String {
        val emailNotificationSendTo = setOf("mavi@vaadin.com", project.owner.email).joinToString(" ")
        val envVars = mutableListOf(
            "export BUILD_MEMORY=${project.build.resources.memoryMb}m",
            "export CPU_QUOTA=${(project.build.resources.cpu.toDouble() * 100000).toInt()}"
        )
        if (project.build.buildArgs.isNotEmpty()) {
            val buildArgs: String =
                project.build.buildArgs.entries.joinToString(" ") { (k, v) -> """--build-arg $k="$v"""" }
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
  <description>${project.description}. Web page: ${project.gitRepo}. Owner: ${project.owner}</description>
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
        <url>${project.gitRepo.url}</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/${project.gitRepo.branch}</name>
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
      <command>${envVars.joinToString("\n")}
/opt/shepherd/shepherd-build ${project.id}</command>
      <configuredLocalRules/>
    </hudson.tasks.Shell>
  </builders>
  <publishers>
    <hudson.tasks.Mailer plugin="mailer">
      <recipients>${emailNotificationSendTo}</recipients>
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

    /**
     * Creates a new Jenkins job for given project.
     */
    public fun createJob(project: Project) {
        val xml = getJobXml(project)
        jenkins.createJob(project.id.id, xml)
    }

    /**
     * Updates Jenkins job.
     */
    public fun updateJob(project: Project) {
        val xml = getJobXml(project)
        jenkins.updateJob(project.id.id, xml)
    }

    /**
     * Deletes Jenkins job for given project.
     */
    public fun deleteJob(id: ProjectId) {
        jenkins.deleteJob(id.id)
    }
}
