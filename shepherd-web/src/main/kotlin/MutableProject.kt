package com.github.mvysny.shepherd.web

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
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.URL

/**
 * An editable version of [Project].
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
    @field:URL
    @field:Length(max = 255)
    var gitRepoBranch: String?,
    @field:Length(max = 255)
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
    @field:Max(512)
    var runtimeMemoryMb: Int,
    @field:NotNull
    @field:Min(1)
    @field:Max(1)
    var runtimeCpu: Float,
    @field:NotNull
    var envVars: MutableList<NamedVar>,
    var buildResources: Resources,
    @field:NotNull
    var buildArgs: MutableList<NamedVar>,
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
        fun newEmpty() = MutableProject(
            id = null,
            description = null,
            webpage = null,
            gitRepoURL = null,
            gitRepoBranch = "main",
            gitRepoCredentialsID = null,
            projectOwnerName = null,
            projectOwnerEmail = null,
            runtimeMemoryMb = Resources.defaultRuntimeResources.memoryMb,
            runtimeCpu = Resources.defaultRuntimeResources.cpu,
            envVars = mutableListOf(),
            buildResources = Resources.defaultBuildResources,
            buildArgs = mutableListOf(),
            buildDockerFile = null,
            publishOnMainDomain = true,
            publishAdditionalDomainsHttps = true,
            publishAdditionalDomains = mutableSetOf(),
            ingressMaxBodySizeMb = 1,
            ingressProxyReadTimeoutSeconds = 60,
            additionalServices = mutableSetOf()
        )
    }

    fun toProject(): Project {
        jsr303Validate(this)
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
                    runtimeCpu
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
    var name: String = "",
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    var value: String = ""
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
    runtimeCpu = runtime.resources.cpu,
    envVars = runtime.envVars.map { NamedVar(it.key, it.value) } .toMutableList(),
    buildResources = build.resources,
    buildArgs = build.buildArgs.map { NamedVar(it.key, it.value) } .toMutableList(),
    buildDockerFile = build.dockerFile,
    publishOnMainDomain = publication.publishOnMainDomain,
    publishAdditionalDomainsHttps = publication.https,
    publishAdditionalDomains = publication.additionalDomains.toMutableSet(),
    ingressMaxBodySizeMb = publication.ingressConfig.maxBodySizeMb,
    ingressProxyReadTimeoutSeconds = publication.ingressConfig.proxyReadTimeoutSeconds,
    additionalServices = additionalServices.map { it.type } .toMutableSet()
)

private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

fun jsr303Validate(obj: Any) {
    validator.validate(obj)
}
