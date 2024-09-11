package com.github.mvysny.shepherd.api

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.expect

class FakeShepherdClientTest {
    @Nested inner class GetAllProjects {
        @Test fun smoke() {
            FakeShepherdClient().getAllProjects()
        }
        @Test fun `one fake project`() {
            expect(listOf(ProjectId("vaadin-boot-example-gradle"))) { FakeShepherdClient().withFakeProject().getAllProjects().map { it.project.id } }
        }
    }
    @Nested inner class GetAllProjectIDs {
        @Test fun smoke() {
            FakeShepherdClient().getAllProjectIDs()
        }
        @Test fun `one fake project`() {
            expect(listOf(ProjectId("vaadin-boot-example-gradle"))) { FakeShepherdClient().withFakeProject().getAllProjectIDs() }
        }
    }
    @Nested inner class getProjectInfo {
        @Test fun smoke() {
            FakeShepherdClient().withFakeProject().getProjectInfo(ProjectId("vaadin-boot-example-gradle"))
        }
    }
    @Nested inner class createProject {
        @Test fun smoke() {
            FakeShepherdClient().createProject(fakeProject2)
        }
        @Test fun `fails if new project would overflow memory`() {
            val cfg = Config(
                4500,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            val client = FakeShepherdClient(cfg)
            client.createProject(fakeProject)
            assertThrows<IllegalArgumentException> {
                client.createProject(fakeProject2)
            }
        }
        @Test fun `runtime memory limits`() {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(64, 1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            assertThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        @Test fun `runtime cpu limits`() {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 0.1f),
                maxProjectBuildResources = Resources(2500, 2f)
            )
            assertThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        @Test fun `build memory limits`() {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(64, 2f)
            )
            assertThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
        @Test fun `build cpu limits`() {
            val cfg = Config(
                5200,
                2,
                maxProjectRuntimeResources = Resources(512, 1f),
                maxProjectBuildResources = Resources(2500, 1f)
            )
            assertThrows<IllegalArgumentException> {
                FakeShepherdClient(cfg).createProject(fakeProject)
            }
        }
    }
}
