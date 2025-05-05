package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.fakeProject
import org.junit.jupiter.api.Test
import kotlin.test.expect

class TraefikDockerRuntimeContainerSystemTest {
    @Test fun calculateDockerRunCommand() {
        val c = TraefikDockerRuntimeContainerSystem("v-herd.eu")
        expect(listOf("docker", "run", "-d", "-t", "--name", "shepherd_vaadin-boot-example-gradle", "--restart", "always",
            "--network", "vaadin-boot-example-gradle.shepherd", "-m", "256m", "--cpus", "1.0",
            "--label", "traefik.http.routers.shepherd_vaadin-boot-example-gradle.entrypoints=http",
            "--label", "traefik.http.routers.shepherd_vaadin-boot-example-gradle.rule=Host(`vaadin-boot-example-gradle.v-herd.eu`)",
            "shepherd/vaadin-boot-example-gradle:latest")) { c.calculateDockerRunCommand(fakeProject) }
    }
}