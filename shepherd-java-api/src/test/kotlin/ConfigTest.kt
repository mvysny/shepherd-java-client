package com.github.mvysny.shepherd.api

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.expect

class ConfigTest {
    @Test fun load() {
        val temp = File.createTempFile("testconfig", "json")
        temp.writeText("""{
        "memoryQuotaMb": 14102,
        "concurrentJenkinsBuilders": 2,
        "maxProjectRuntimeResources": {
                "memoryMb": 512,
                "cpu": 1
        },
        "maxProjectBuildResources": {
                "memoryMb": 2500,
                "cpu": 2
        },
        "jenkins": {
          "url": "http://localhost:8080/jenkins",
          "username": "admin",
          "password": "secretpassword"
        }
}
""")
        expect("secretpassword") {
            Config.load(temp.toPath()).jenkins.password
        }
        expect("http://localhost:8080/jenkins") {
            Config.load(temp.toPath()).jenkins.url
        }
        temp.delete()
    }
}
