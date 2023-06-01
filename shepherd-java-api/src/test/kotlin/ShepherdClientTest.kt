package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class ShepherdClientTest : DynaTest({
    group("ProjectMemoryStats") {
        test("empty") {
            expect(ProjectMemoryStats(MemoryUsageStats(0, 2), MemoryUsageStats(0, 2))) {
                ProjectMemoryStats.calculateQuota(Config(2, 2), listOf())
            }
        }
        test("1 project") {
            expect(ProjectMemoryStats(MemoryUsageStats(256, 1952), MemoryUsageStats(2304, 4000))) {
                ProjectMemoryStats.calculateQuota(Config(4000, 2), listOf(fakeProject))
            }
        }
        test("2 projects") {
            expect(ProjectMemoryStats(MemoryUsageStats(512, 1904), MemoryUsageStats(4608, 6000))) {
                ProjectMemoryStats.calculateQuota(Config(6000, 2), listOf(fakeProject, fakeProject2))
            }
        }
        test("2 projects 1 builder") {
            expect(ProjectMemoryStats(MemoryUsageStats(512, 3952), MemoryUsageStats(2560, 6000))) {
                ProjectMemoryStats.calculateQuota(Config(6000, 1), listOf(fakeProject, fakeProject2))
            }
        }
    }
    group("HostMemoryUsageStats") {
        test("smoke") {
            println(HostMemoryUsageStats.getHostStats())
        }
    }
})
