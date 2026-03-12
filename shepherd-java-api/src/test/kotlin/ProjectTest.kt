package com.github.mvysny.shepherd.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.expect

val fakeProject = Project(
    id = ProjectId("vaadin-boot-example-gradle"),
    description = "Example project for Vaadin Boot built via Gradle",
    gitRepo = GitRepo("https://github.com/mvysny/vaadin-boot-example-gradle", "master"),
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtime = ProjectRuntime(Resources.defaultRuntimeResources),
    build = BuildSpec(resources = Resources.defaultBuildResources)
)
private val serializedJson = """{"id":"vaadin-boot-example-gradle","description":"Example project for Vaadin Boot built via Gradle","gitRepo":{"url":"https://github.com/mvysny/vaadin-boot-example-gradle","branch":"master"},"owner":{"name":"Martin Vysny","email":"mavi@vaadin.com"},"runtime":{"resources":{"memoryMb":256,"cpu":1.0}},"build":{"resources":{"memoryMb":2048,"cpu":2.0}}}"""

val fakeProject2 = Project(
    id = ProjectId("jdbi-orm-vaadin-crud-demo"),
    description = "JDBI-ORM example project",
    gitRepo = GitRepo("https://github.com/mvysny/jdbi-orm-vaadin-crud-demo", "master", credentialsID = "c4d257ce-0048-11ee-a0b5-ffedf9ffccf4"),
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtime = ProjectRuntime(
        Resources.defaultRuntimeResources,
        envVars = mapOf(
            "JDBC_URL" to "jdbc:postgresql://postgres-service:5432/postgres",
            "JDBC_USERNAME" to "postgres",
            "JDBC_PASSWORD" to "mysecretpassword"
        )
    ),
    build = BuildSpec(
        resources = Resources.defaultBuildResources,
        buildArgs = mapOf(
            "offlinekey" to "q3984askdjalkd9823"
        ),
        dockerFile = "vherd.Dockerfile"
    ),
    publication = Publication(
        publishOnMainDomain = false,
        additionalDomains = setOf("demo.jdbiorm.eu")
    ),
    additionalServices = setOf(Service(ServiceType.Postgres)),
    additionalAdmins = setOf("someone.else@foo.eu", "foo@bar.baz")
)
private val serializedJson2 = """{"id":"jdbi-orm-vaadin-crud-demo","description":"JDBI-ORM example project","gitRepo":{"url":"https://github.com/mvysny/jdbi-orm-vaadin-crud-demo","branch":"master","credentialsID":"c4d257ce-0048-11ee-a0b5-ffedf9ffccf4"},"owner":{"name":"Martin Vysny","email":"mavi@vaadin.com"},"runtime":{"resources":{"memoryMb":256,"cpu":1.0},"envVars":{"JDBC_URL":"jdbc:postgresql://postgres-service:5432/postgres","JDBC_USERNAME":"postgres","JDBC_PASSWORD":"mysecretpassword"}},"build":{"resources":{"memoryMb":2048,"cpu":2.0},"buildArgs":{"offlinekey":"q3984askdjalkd9823"},"dockerFile":"vherd.Dockerfile"},"publication":{"publishOnMainDomain":false,"additionalDomains":["demo.jdbiorm.eu"]},"additionalServices":[{"type":"Postgres"}],"additionalAdmins":["someone.else@foo.eu","foo@bar.baz"]}"""


class ProjectIdTest {
    @Test fun `validation pass`() {
        ProjectId("vaadin-boot-example-gradle")
        ProjectId("manolo-26-2")
    }
    @Test fun `validation fails`() {
        var ex = assertThrows<IllegalArgumentException> {
            ProjectId("-foo")
        }
        assertTrue(ex.message!!.contains("The ID must contain"), ex.message)

        ex = assertThrows<IllegalArgumentException> {
            ProjectId("")
        }
        expect(true, ex.message) { ex.message!!.contains("The ID must contain") }

        ex = assertThrows<IllegalArgumentException> {
            ProjectId("bar-")
        }
        expect(true, ex.message) { ex.message!!.contains("The ID must contain") }

        ex = assertThrows<IllegalArgumentException> {
            ProjectId("Bar")
        }
        expect(true, ex.message) { ex.message!!.contains("The ID must contain") }
    }
}

class ProjectTest {
    @Test fun `json serialization`() {
        expect(serializedJson) { fakeProject.toJson() }
        expect(serializedJson2) { fakeProject2.toJson() }
    }
    @Test fun `json deserialization`() {
        expect(fakeProject) { Project.fromJson(serializedJson) }
        expect(fakeProject2) { Project.fromJson(serializedJson2) }
    }
    @Test fun basicProperties() {
        expect(setOf("mavi@vaadin.com")) { fakeProject.allAdmins }
        expect(setOf("mavi@vaadin.com")) { fakeProject.emailNotificationSendTo }
        expect(true) { fakeProject.canEdit("mavi@vaadin.com") }
        expect(false) { fakeProject.canEdit("foo@bar.baz") }

        expect(setOf("mavi@vaadin.com", "someone.else@foo.eu", "foo@bar.baz")) { fakeProject2.allAdmins }
        expect(setOf("mavi@vaadin.com", "someone.else@foo.eu", "foo@bar.baz")) { fakeProject2.emailNotificationSendTo }
        expect(true) { fakeProject2.canEdit("mavi@vaadin.com") }
        expect(true) { fakeProject2.canEdit("foo@bar.baz") }
    }
}

class BuildSpecTest {
    @Test fun `buildContext validation - valid contexts`() {
        BuildSpec(Resources.defaultBuildResources, buildContext = null)
        BuildSpec(Resources.defaultBuildResources, buildContext = "demo")
        BuildSpec(Resources.defaultBuildResources, buildContext = "services/api")
        BuildSpec(Resources.defaultBuildResources, buildContext = "path/to/subdir")
    }

    @Test fun `buildContext validation - rejects whitespaces`() {
        val ex = assertThrows<IllegalArgumentException> {
            BuildSpec(Resources.defaultBuildResources, buildContext = "path with spaces")
        }
        assertTrue(ex.message!!.contains("whitespaces"), ex.message)
    }

    @Test fun `buildContext validation - rejects directory traversal`() {
        val ex = assertThrows<IllegalArgumentException> {
            BuildSpec(Resources.defaultBuildResources, buildContext = "../parent")
        }
        assertTrue(ex.message!!.contains(".."), ex.message)

        val ex2 = assertThrows<IllegalArgumentException> {
            BuildSpec(Resources.defaultBuildResources, buildContext = "foo/../bar")
        }
        assertTrue(ex2.message!!.contains(".."), ex2.message)
    }

    @Test fun `buildContext validation - rejects absolute paths`() {
        val ex = assertThrows<IllegalArgumentException> {
            BuildSpec(Resources.defaultBuildResources, buildContext = "/absolute/path")
        }
        assertTrue(ex.message!!.contains("relative"), ex.message)
    }

    @Test fun `buildContext serialization`() {
        val spec = BuildSpec(Resources.defaultBuildResources, buildContext = "demo")
        val json = JsonUtils.toJson(spec, prettyPrint = false)
        assertTrue(json.contains("\"buildContext\":\"demo\""), json)

        val deserialized = JsonUtils.fromJson<BuildSpec>(json)
        expect("demo") { deserialized.buildContext }
    }
}

class GitRepoTest {
    @Test
    fun validation() {
        // valid repos
        GitRepo(
            "https://github.com/mvysny/shepherd-java-client",
            "master",
            null
        )
        GitRepo(
            "git@github.com:mvysny/shepherd-java-client.git",
            "master",
            "dbe7586a-86f2-11ef-ad36-bb31ef1eb1e7"
        )

        // https://stackoverflow.com/q/31801271/377320
        listOf(
            "https://github.com/mvysny/shepherd-java-client",
            "git@github.com:mvysny/shepherd-java-client.git",
            "ssh://user@host.xz:port/path/to/repo.git/",
            "ssh://user@host.xz/path/to/repo.git/",
            "ssh://host.xz:port/path/to/repo.git/",
            "ssh://host.xz/path/to/repo.git/",
            "ssh://user@host.xz/path/to/repo.git/",
            "ssh://host.xz/path/to/repo.git/",
            "ssh://user@host.xz/~user/path/to/repo.git/",
            "ssh://host.xz/~user/path/to/repo.git/",
            "ssh://user@host.xz/~/path/to/repo.git",
            "ssh://host.xz/~/path/to/repo.git",
            "user@host.xz:/path/to/repo.git/",
            "host.xz:/path/to/repo.git/",
            "user@host.xz:~user/path/to/repo.git/",
            "host.xz:~user/path/to/repo.git/",
            "user@host.xz:path/to/repo.git",
            "host.xz:path/to/repo.git",
            "rsync://host.xz/path/to/repo.git/",
            "git://host.xz/path/to/repo.git/",
            "git://host.xz/~user/path/to/repo.git/",
            "git@github.com:onuridrisoglu/mtk-ballpark.git",
            "http://host.xz/path/to/repo.git/",
            "https://host.xz/path/to/repo.git/",
        ).forEach { GitRepo(it, "master") }

        // invalid repos
        assertThrows<IllegalArgumentException> {
            GitRepo("foo", "master")
        }
        assertThrows<IllegalArgumentException> {
            GitRepo(
                "foo git@github.com:mvysny/shepherd-java-client.git",
                "master"
            )
        }
        assertThrows<IllegalArgumentException> {
            GitRepo(
                "git@github.com:mvysny/shepherd-java-client.git",
                "master",
                "abc"
            )
        }
    }
}
