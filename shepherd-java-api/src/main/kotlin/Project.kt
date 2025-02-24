@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URI
import java.nio.file.Path
import java.util.UUID

/**
 * The Project ID must:
 *
 * * contain at most 54 characters
 * * contain only lowercase alphanumeric characters or '-'
 * * start with an alphanumeric character
 * * end with an alphanumeric character
 */
@Serializable(with = ProjectIdAsStringSerializer::class)
public data class ProjectId(public val id: String) : Comparable<ProjectId> {
    init {
        require(isValid(id)) { "The ID must contain at most 54 characters, it must contain only lowercase alphanumeric characters or '-', it must start and end with an alphanumeric character" }
    }
    public companion object {
        public const val idValidatorPattern: String = "[a-z0-9][a-z0-9\\-]{0,52}[a-z0-9]"
        private val idValidator = idValidatorPattern.toRegex()
        public fun isValid(id: String): Boolean = idValidator.matches(id)
    }

    override fun compareTo(other: ProjectId): Int = id.compareTo(other.id)
}

private object ProjectIdAsStringSerializer : KSerializer<ProjectId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ProjectId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ProjectId) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): ProjectId = ProjectId(decoder.decodeString())
}

/**
 * Information about the project owner, how to reach him in case the project needs to be modified/misbehaves.
 * Jenkins may send notification emails about the failed builds to [email].
 * @property name e.g. `Martin Vysny`
 * @property email e.g. `mavi@vaadin.com`
 */
@Serializable
public data class ProjectOwner(
    val name: String,
    val email: String
) {
    override fun toString(): String = "$name <$email>"
}

/**
 * Resources the app needs, memory and CPU.
 * @property memoryMb max memory in megabytes
 * @property cpu max CPU cores to use. 1 means 1 CPU core to be used.
 */
@Serializable
public data class Resources(
    val memoryMb: Int,
    val cpu: Float
) {
    init {
        // 64 Mb is not arbitrary - this corresponds to the requests/memory in the Kubernetes
        // config yaml file generated by SimpleKubernetesClient
        require(memoryMb >= 64) { "Give the process at least 64 Mb: $memoryMb" }
        require(cpu > 0) { "cpu: must be greater than 0 but got $cpu" }
    }
    public companion object {
        @JvmStatic
        public val defaultRuntimeResources: Resources = Resources(memoryMb = 256, cpu = 1f)
        @JvmStatic
        public val defaultBuildResources: Resources = Resources(memoryMb = 2048, cpu = 2f)
    }

    override fun toString(): String = "Memory: $memoryMb MB; CPU: $cpu cores"
}

/**
 * How to build the project.
 * @property resources how many resources to allocate for the build. Passed via `BUILD_MEMORY` and `CPU_QUOTA` env variables to `shepherd-build`.
 * @property buildArgs optional build args, passed as `--build-arg name="value"` to `docker build` via the `BUILD_ARGS` env variable passed to `shepherd-build`.
 * @property dockerFile if not null, we build off this dockerfile instead of the default `Dockerfile`. Passed via `DOCKERFILE` env variable to `shepherd-build`.
 */
@Serializable
public data class BuildSpec @JvmOverloads constructor(
    val resources: Resources,
    val buildArgs: Map<String, String> = mapOf(),
    val dockerFile: String? = null
)

/**
 * A git repository.
 * @property url the git repository from where the project comes from, e.g. `https://github.com/mvysny/vaadin-boot-example-gradle`
 * @property branch usually `master` or `main`
 * @property credentialsID a Jenkins credentials UUID to be used when accessing remote private repository. See the README for more details.
 */
@Serializable
public data class GitRepo(
    val url: String,
    val branch: String,
    val credentialsID: String? = null
) {
    init {
        require(!url.containsWhitespaces()) { "url '$url' must not contain whitespaces" }
        validateGitUrl(url)
        require(!branch.containsWhitespaces()) { "branch '$branch' must not contain whitespaces" }
        require(credentialsID == null || !credentialsID.containsWhitespaces()) { "credentialsID '$credentialsID' must not contain whitespaces" }
        if (credentialsID != null) {
            UUID.fromString(credentialsID) // make sure it's a UUID
        }
    }

    public companion object {
        // https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS
        private val scpLikeSyntaxRegex = "([A-Za-z][A-Za-z0-9+.-]*@)?[A-Za-z][A-Za-z0-9+.-]*:[A-Za-z0-9+.\\-/~]*".toRegex()

        public fun validateGitUrl(url: String) {
            require(url.contains(':'))  { "url '$url' must contain colon (can not be a local path)" }
            if (url.contains("://")) {
                URI.create(url) // don't convert to URL otherwise this would fail: unknown protocol: ssh
            } else {
                require(scpLikeSyntaxRegex.matches(url)) { "url '$url' doesn't have valid scp-like syntax" }
            }
        }
    }
}

/**
 * Runtime project config.
 * @property resources what resources the project needs for running, memory and CPU. Please try to keep the memory requirements as low as possible, so that we can host as many projects as possible.
 * @property envVars environment variables, e.g. `SPRING_DATASOURCE_URL` to `jdbc:postgresql://liukuri-postgres:5432/postgres`
 */
@Serializable
public data class ProjectRuntime @JvmOverloads constructor(
    val resources: Resources,
    val envVars: Map<String, String> = mapOf()
) {
    init {
        envVars.keys.forEach { key ->
            require(!key.containsWhitespaces()) { "key $key: must not contain whitespaces" }
        }
    }
}

/**
 * @property id the project ID, must be unique.
 * @property description any additional vital information about the project.
 * @property webpage the project home page. If null, use [GitRepo.url]. Call [Project.resolveWebpage] to do this automatically.
 * Useful to have if [GitRepo.url] points to a private Git repo which is not browseable by the browser.
 * @property gitRepo the git repository from where the project comes from
 * @property owner the project owner: a contact person responsible for the project.
 * @property additionalAdmins if not null, lists e-mails of additional project admins. These users are also allowed
 * to fully manage the project, and are also notified of build failures.
 * @property runtime what resources the project needs for running
 * @property build build info
 * @property publication how to publish project over http/https
 * @property additionalServices any additional services the project needs, e.g. additional databases and such.
 */
@Serializable
public data class Project(
    val id: ProjectId,
    val description: String,
    val webpage: String? = null,
    val gitRepo: GitRepo,
    val owner: ProjectOwner,
    val runtime: ProjectRuntime,
    val build: BuildSpec,
    val publication: Publication = Publication(),
    val additionalServices: Set<Service> = setOf(),
    var additionalAdmins: Set<String>? = null,
) {
    /**
     * Returns URLs on which this project runs (can be browsed to). E.g. for `vaadin-boot-example-gradle`
     * on the `v-herd.eu` [host], this returns `https://v-herd.eu/vaadin-boot-example-gradle`.
     */
    public fun getPublishedURLs(host: String): List<String> =
        listOf("https://$host/${id.id}") + publication.additionalDomains.map { "https://$it" }

    public fun resolveWebpage(): String = webpage ?: gitRepo.url

    public companion object {
        /**
         * Loads [Project] from given JSON [file].
         * @throws java.io.FileNotFoundException Fails if the file doesn't exist.
         */
        @JvmStatic
        public fun loadFromFile(file: Path): Project = JsonUtils.fromFile(file)

        @JvmStatic
        public fun fromJson(json: String): Project = Json.decodeFromString(json)
    }

    /**
     * Saves this project as a JSON to given [file]. Pretty-prints the JSON by default;
     * override via the [prettyPrint] parameter.
     */
    @JvmOverloads
    public fun saveToFile(file: Path, prettyPrint: Boolean = true) {
        JsonUtils.saveToFile<Project>(this, file, prettyPrint)
    }

    @JvmOverloads
    public fun toJson(prettyPrint: Boolean = false): String = JsonUtils.toJson(this, prettyPrint)

    /**
     * Checks if given user can manage this project: if he can change the project settings, view the project logs
     * and even delete the project.
     */
    public fun canEdit(userEmail: String): Boolean =
        owner.email == userEmail || (additionalAdmins != null && additionalAdmins!!.contains(userEmail))

    public val allAdmins: Set<String> get() = when {
        additionalAdmins == null -> setOf(owner.email)
        else -> setOf(owner.email) + additionalAdmins!!
    }

    /**
     * Lists e-mails of people that needs to be contacted if the project fails to build.
     */
    public val emailNotificationSendTo: Set<String> get() = allAdmins
}

@Serializable
public enum class ServiceType {
    /**
     * A PostgreSQL database. Use the following values to access the database:
     * * JDBC URI: `jdbc:postgresql://postgres-service:5432/postgres`
     * * username: `postgres`
     * * password: `mysecretpassword`.
     * The database is only accessible by your project; no other project has access to the database.
     */
    Postgres
}

@Serializable
public data class Service(
    val type: ServiceType
)

/**
 * The project publication over http/https.
 * @property publishOnMainDomain if true (the default), the project will be published on the main domain as well.
 * Say the main domain is `v-herd.eu`, then the project will be accessible at `v-herd.eu/PROJECT_ID`.
 * @property https only affects [additionalDomains]; if the project is published on the main domain then it always uses https.
 * Defaults to true. If false, the project is published on [additionalDomains] via plain http.
 * Useful e.g. when CloudFlare unwraps https for us.
 * @property additionalDomains additional domains to publish to project at. Must not contain the main domain.
 * E.g. `yourproject.com`.
 * @property ingressConfig additional NGINX Ingress configuration.
 */
@Serializable
public data class Publication(
    val publishOnMainDomain: Boolean = true,
    val https: Boolean = true,
    val additionalDomains: Set<String> = setOf(),
    val ingressConfig: IngressConfig = IngressConfig()
)

/**
 * NGINX Ingress additional configuration.
 * @property maxBodySizeMb in megabytes, defaults to 1m. [client_max_body_size](http://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size).
 * Max request body size, increase if you intend to upload large files.
 * @property proxyReadTimeoutSeconds in seconds, defaults to 60s. [proxy_read_timeout](http://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_read_timeout).
 * Increase to 6 minutes or more if you use Vaadin Push, otherwise the connection will be dropped out. Alternatively, set this to 3 minutes and set
 * Vaadin heartbeat frequency to 2 minutes.
 */
@Serializable
public data class IngressConfig(
    val maxBodySizeMb: Int = 1,
    val proxyReadTimeoutSeconds: Int = 60,
) {
    init {
        require(maxBodySizeMb >= 1) { "maxBodySize: must be 1 or greater but was $maxBodySizeMb" }
        require(proxyReadTimeoutSeconds >= 1) { "proxyReadTimeout: must be 1 or greater but was $proxyReadTimeoutSeconds" }
    }
}
