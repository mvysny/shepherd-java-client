package com.github.mvysny.shepherd.api

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.containers.wait.strategy.WaitStrategy

class JenkinsContainer : GenericContainer<JenkinsContainer>("jenkins/jenkins:lts") {
    init {
        withExposedPorts(8080)
        // https://www.jenkins.io/doc/book/managing/system-properties/
        withEnv("JAVA_OPTS", "-Djenkins.install.runSetupWizard=false")
    }

    override fun getWaitStrategy(): WaitStrategy? {
        // wait while Jenkins is getting ready
        return Wait.forHttp("/")
    }
}
