package com.github.mvysny.shepherd.api

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

class ShepherdClientTest {
    @Nested inner class TestProjectMemoryStats {
        @Test fun empty() {
            expect(ProjectMemoryStats(MemoryUsageStats(0, 2), MemoryUsageStats(0, 2))) {
                ProjectMemoryStats.calculateQuota(Config(2, 2, Resources.defaultRuntimeResources, Resources.defaultBuildResources), listOf())
            }
        }
        @Test fun `1 project`() {
            expect(ProjectMemoryStats(MemoryUsageStats(256, 1952), MemoryUsageStats(2304, 4000))) {
                ProjectMemoryStats.calculateQuota(Config(4000, 2, Resources.defaultRuntimeResources, Resources.defaultBuildResources), listOf(fakeProject))
            }
        }
        @Test fun `2 projects`() {
            expect(ProjectMemoryStats(MemoryUsageStats(512, 1904), MemoryUsageStats(4608, 6000))) {
                ProjectMemoryStats.calculateQuota(Config(6000, 2, Resources.defaultRuntimeResources, Resources.defaultBuildResources), listOf(fakeProject, fakeProject2))
            }
        }
        @Test fun `2 projects 1 builder`() {
            expect(ProjectMemoryStats(MemoryUsageStats(512, 3952), MemoryUsageStats(2560, 6000))) {
                ProjectMemoryStats.calculateQuota(Config(6000, 1, Resources.defaultRuntimeResources, Resources.defaultBuildResources), listOf(fakeProject, fakeProject2))
            }
        }
    }
    @Nested inner class TestHostMemoryUsageStats {
        @Test fun smoke() {
            println(HostMemoryUsageStats.getHostStats())
        }
    }
}
