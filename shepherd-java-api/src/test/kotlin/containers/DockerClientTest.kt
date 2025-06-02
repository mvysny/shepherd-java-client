package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.ResourcesUsage
import org.junit.jupiter.api.Test
import kotlin.test.expect

class DockerClientTest {
    @Test fun parseContainerStats() {
        expect(ResourcesUsage(128, 0.16f)) {
            DockerClient.parseContainerStats("0.16% 128.1MiB / 256MiB")
        }
        expect(ResourcesUsage(259, 0.20f)) {
            DockerClient.parseContainerStats("0.20% 259MiB / 7.549GiB")
        }
        expect(ResourcesUsage(2048, 0.20f)) {
            DockerClient.parseContainerStats("0.20% 2.048GiB / 7.549GiB")
        }
    }
}