package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import kotlin.test.expect

val fakeProject = Project(
    id = ProjectId("vaadin-boot-example-gradle"),
    description = "vaadin-boot-example-gradle",
    gitRepo = GitRepo("https://github.com/mvysny/vaadin-boot-example-gradle", "master"),
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtime = ProjectRuntime(Resources.defaultRuntimeResources),
    build = Build(resources = Resources.defaultBuildResources)
)
private val serializedJson = """{"id":"vaadin-boot-example-gradle","description":"vaadin-boot-example-gradle","gitRepo":{"url":"https://github.com/mvysny/vaadin-boot-example-gradle","branch":"master"},"owner":{"name":"Martin Vysny","email":"mavi@vaadin.com"},"runtime":{"resources":{"memoryMb":256,"cpu":1.0}},"build":{"resources":{"memoryMb":2048,"cpu":2.0}}}"""

class ProjectIdTest : DynaTest({
    test("validation pass") {
        ProjectId("vaadin-boot-example-gradle")
        ProjectId("manolo-26-2")
    }
    test("validation fails") {
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
})

class ProjectTest : DynaTest({
    test("json serialization") {
        expect(serializedJson) { fakeProject.toJson() }
    }
    test("json deserialization") {
        expect(fakeProject) { Project.fromJson(serializedJson) }
    }
})
