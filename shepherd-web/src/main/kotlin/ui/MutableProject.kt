package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.shepherd.api.BuildSpec
import com.github.mvysny.shepherd.api.GitRepo
import com.github.mvysny.shepherd.api.IngressConfig
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ProjectOwner
import com.github.mvysny.shepherd.api.ProjectRuntime
import com.github.mvysny.shepherd.api.Publication
import com.github.mvysny.shepherd.api.Resources
import com.github.mvysny.shepherd.api.Service
import com.github.mvysny.shepherd.api.ServiceType
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.host
import com.github.mvysny.shepherd.web.jsr303Validate
import com.github.mvysny.shepherd.web.security.User
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.URL
import org.hibernate.validator.constraints.UUID
import java.math.BigDecimal

/**
 * An editable version of [Project].
 * @property id The project ID, must be unique. The project will be published and running at https://$host/PROJECT_ID
 * @property description any additional vital information about the project.
 * @property webpage WebPage: the project home page. May be empty, in such case GitRepo URL is considered the home page
 * @property gitRepoURL the git repository from where the project comes from, e.g. `https://github.com/mvysny/vaadin-boot-example-gradle`
 * @property gitRepoBranch usually `master` or `main`
 * @property gitRepoCredentialsID todo document
 * @property projectOwnerName The project owner name.
 * @property projectOwnerEmail How to reach the project owner in case the project needs to be modified/misbehaves.
 * Jenkins will send notification emails about the failed builds here.
 * @property runtimeMemoryMb how much memory the project needs for running, in MB. Please try to keep the memory requirements as low as possible, so that we can host as many projects as possible. 256MB is a good default.
 * @property runtimeCpu max CPU cores to use. 1 means 1 CPU core to be used.
 * @property envVars runtime environment variables, e.g. `SPRING_DATASOURCE_URL` to `jdbc:postgresql://liukuri-postgres:5432/postgres`
 * @property buildArgs optional build args, passed as `--build-arg name="value"` to `docker build`. You can e.g. pass Vaadin Offline Key here.
 * @property buildDockerFile if not null, we build off this dockerfile instead of the default `Dockerfile`.
 * @property publishOnMainDomain if true (the default), the project will be published on the main domain as well.
 * Say the main domain is `v-herd.eu`, then the project will be accessible at `v-herd.eu/PROJECT_ID`.
 * @property publishAdditionalDomainsHttps only affects additional domains; if the project is published on the main domain then it always uses https.
 * Defaults to true. If false, the project is published on additional domains via plain http.
 * Useful e.g. when CloudFlare unwraps https for us. Ignored if there are no additional domains.
 * @property publishAdditionalDomains additional domains to publish to project at. Must not contain the main domain.
 * E.g. `yourproject.com`. You need to configure your domain DNS record to point to v-herd.eu IP address first!
 * @property ingressMaxBodySizeMb Max request body size, in megabytes, defaults to 1m.
 * Increase if you intend to upload large files.
 * @property ingressProxyReadTimeoutSeconds Proxy Read Timeout, in seconds, defaults to 60s.
 * Increase to 6 minutes or more if you use Vaadin Push, otherwise the connection will be dropped out. Alternatively, set this to 3 minutes and set
 * Vaadin heartbeat frequency to 2 minutes.
 * @property additionalServices additional services, only accessible by your project. If you enable PostgreSQL, then use the following values to access the database:
 * JDBC URI: `jdbc:postgresql://postgres-service:5432/postgres`, username: `postgres`, password: `mysecretpassword`.
 */
data class MutableProject(
    @field:NotNull
    @field:NotBlank
    @field:Pattern(regexp = ProjectId.idValidatorPattern, message = "The ID must contain at most 54 characters, it must contain only lowercase alphanumeric characters or '-', it must start and end with an alphanumeric character")
    var id: String?,
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    var description: String?,
    @field:URL
    @field:Length(max = 255)
    var webpage: String?,
    @field:NotNull
    @field:NotBlank
    @field:URL
    @field:Length(max = 255)
    var gitRepoURL: String?,
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    var gitRepoBranch: String?,
    @field:Length(max = 255)
    @field:UUID
    var gitRepoCredentialsID: String?,
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    var projectOwnerName: String?,
    @field:NotNull
    @field:NotBlank
    @field:Email
    @field:Length(max = 255)
    var projectOwnerEmail: String?,
    @field:NotNull
    @field:Min(64)
    var runtimeMemoryMb: Int,
    @field:NotNull
    @field:DecimalMin("0.1")
    var runtimeCpu: BigDecimal,
    @field:NotNull
    var envVars: Set<NamedVar>,
    var buildResources: Resources,
    @field:NotNull
    var buildArgs: Set<NamedVar>,
    @field:Length(max = 255)
    var buildDockerFile: String?,

    var publishOnMainDomain: Boolean,
    var publishAdditionalDomainsHttps: Boolean,
    @field:NotNull
    var publishAdditionalDomains: MutableSet<String>,
    @field:NotNull
    @field:Min(1)
    @field:Max(10)
    var ingressMaxBodySizeMb: Int,
    @field:NotNull
    @field:Min(1)
    @field:Max(600)
    var ingressProxyReadTimeoutSeconds: Int,

    @field:NotNull
    var additionalServices: MutableSet<ServiceType>
) {
    companion object {
        fun newEmpty(owner: User) = MutableProject(
            id = null,
            description = null,
            webpage = null,
            gitRepoURL = null,
            gitRepoBranch = "main",
            gitRepoCredentialsID = null,
            projectOwnerName = owner.name,
            projectOwnerEmail = owner.email,
            runtimeMemoryMb = Resources.defaultRuntimeResources.memoryMb,
            runtimeCpu = Resources.defaultRuntimeResources.cpu.toBigDecimal(),
            envVars = setOf(),
            buildResources = Resources.defaultBuildResources,
            buildArgs = setOf(),
            buildDockerFile = null,
            publishOnMainDomain = true,
            publishAdditionalDomainsHttps = true,
            publishAdditionalDomains = mutableSetOf(),
            ingressMaxBodySizeMb = 1,
            ingressProxyReadTimeoutSeconds = 60,
            additionalServices = mutableSetOf()
        )
    }

    /**
     * Validates values in this bean.
     * @throws ValidationException if validation fails.
     */
    fun validate() {
        jsr303Validate(this)
        if (publishAdditionalDomains.contains(host)) {
            throw ValidationException("Additional domains must not contain $host")
        }
        if (publishAdditionalDomains.any { it.endsWith(".$host") }) {
            throw ValidationException("Additional domains must not contain *.$host")
        }
    }

    fun toProject(): Project {
        validate()
        return Project(
            id = ProjectId(id!!),
            description = description!!,
            webpage = webpage,
            gitRepo = GitRepo(
                gitRepoURL!!,
                gitRepoBranch!!,
                gitRepoCredentialsID
            ),
            owner = ProjectOwner(projectOwnerName!!, projectOwnerEmail!!),
            runtime = ProjectRuntime(
                resources = Resources(
                    runtimeMemoryMb,
                    runtimeCpu.toFloat()
                ), envVars = envVars.associate { it.name to it.value }),
            build = BuildSpec(
                buildResources,
                buildArgs.associate { it.name to it.value },
                dockerFile = buildDockerFile
            ),
            publication = Publication(
                publishOnMainDomain = publishOnMainDomain,
                https = publishAdditionalDomainsHttps,
                additionalDomains = publishAdditionalDomains.toSet(),
                ingressConfig = IngressConfig(ingressMaxBodySizeMb, ingressProxyReadTimeoutSeconds)
            ),
            additionalServices = additionalServices.map { Service(it) } .toSet()
        )
    }
}

data class NamedVar(
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    val name: String = "",
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    val value: String = ""
)

fun Project.toMutable(): MutableProject = MutableProject(
    id = id.id,
    description = description,
    webpage = webpage,
    gitRepoURL = gitRepo.url,
    gitRepoBranch = gitRepo.branch,
    gitRepoCredentialsID = gitRepo.credentialsID,
    projectOwnerName = owner.name,
    projectOwnerEmail = owner.email,
    runtimeMemoryMb = runtime.resources.memoryMb,
    runtimeCpu = runtime.resources.cpu.toBigDecimal(),
    envVars = runtime.envVars.map { NamedVar(it.key, it.value) } .toSet(),
    buildResources = build.resources,
    buildArgs = build.buildArgs.map { NamedVar(it.key, it.value) } .toSet(),
    buildDockerFile = build.dockerFile,
    publishOnMainDomain = publication.publishOnMainDomain,
    publishAdditionalDomainsHttps = publication.https,
    publishAdditionalDomains = publication.additionalDomains.toMutableSet(),
    ingressMaxBodySizeMb = publication.ingressConfig.maxBodySizeMb,
    ingressProxyReadTimeoutSeconds = publication.ingressConfig.proxyReadTimeoutSeconds,
    additionalServices = additionalServices.map { it.type } .toMutableSet()
)
