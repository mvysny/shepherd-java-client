package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.expectThrows
import org.junit.jupiter.api.Test
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
    additionalServices = setOf(Service(ServiceType.Postgres))
)
private val serializedJson2 = """{"id":"jdbi-orm-vaadin-crud-demo","description":"JDBI-ORM example project","gitRepo":{"url":"https://github.com/mvysny/jdbi-orm-vaadin-crud-demo","branch":"master","credentialsID":"c4d257ce-0048-11ee-a0b5-ffedf9ffccf4"},"owner":{"name":"Martin Vysny","email":"mavi@vaadin.com"},"runtime":{"resources":{"memoryMb":256,"cpu":1.0},"envVars":{"JDBC_URL":"jdbc:postgresql://postgres-service:5432/postgres","JDBC_USERNAME":"postgres","JDBC_PASSWORD":"mysecretpassword"}},"build":{"resources":{"memoryMb":2048,"cpu":2.0},"buildArgs":{"offlinekey":"q3984askdjalkd9823"},"dockerFile":"vherd.Dockerfile"},"publication":{"publishOnMainDomain":false,"additionalDomains":["demo.jdbiorm.eu"]},"additionalServices":[{"type":"Postgres"}]}"""


class ProjectIdTest {
    @Test fun `validation pass`() {
        ProjectId("vaadin-boot-example-gradle")
        ProjectId("manolo-26-2")
    }
    @Test fun `validation fails`() {
        expectThrows<IllegalArgumentException>("The ID must contain") {
            ProjectId("-foo")
        }
        expectThrows<IllegalArgumentException>("The ID must contain") {
            ProjectId("")
        }
        expectThrows<IllegalArgumentException>("The ID must contain") {
            ProjectId("bar-")
        }
        expectThrows<IllegalArgumentException>("The ID must contain") {
            ProjectId("Bar")
        }
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
}
