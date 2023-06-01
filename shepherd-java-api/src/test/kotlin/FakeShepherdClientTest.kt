package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows

class FakeShepherdClientTest : DynaTest({
    group("getAllProjects()") {
        test("smoke") {
            FakeShepherdClient().getAllProjects()
        }
        test("one fake project") {
            expectList(ProjectId("vaadin-boot-example-gradle")) { FakeShepherdClient().withFakeProject().getAllProjects().map { it.project.id } }
        }
    }
    group("getAllProjectIDs()") {
        test("smoke") {
            FakeShepherdClient().getAllProjectIDs()
        }
        test("one fake project") {
            expectList(ProjectId("vaadin-boot-example-gradle")) { FakeShepherdClient().withFakeProject().getAllProjectIDs() }
        }
    }
    group("getProjectInfo()") {
        test("smoke") {
            FakeShepherdClient().withFakeProject().getProjectInfo(ProjectId("vaadin-boot-example-gradle"))
        }
    }
    group("createProject()") {
        test("smoke") {
            FakeShepherdClient().createProject(fakeProject2)
        }
        test("fails if new project would overflow memory") {
            val cfg = Config(
                4500,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            val client = FakeShepherdClient(cfg)
            client.createProject(fakeProject)
            expectThrows<IllegalArgumentException> {
                client.createProject(fakeProject2)
            }
        }
        test("runtime memory limits") {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(64, 1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            expectThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        test("runtime cpu limits") {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 0.1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            expectThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        test("build memory limits") {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(64, 2f)
            )
            expectThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        test("build cpu limits") {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(2500, 1f)
            )
            expectThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
    }
})
