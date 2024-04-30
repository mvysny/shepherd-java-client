package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import java.io.File
import kotlin.test.expect

class ConfigTest : DynaTest({
    test("load") {
        val temp = File.createTempFile("testconfig", "json")
        Config.location = temp.toPath()
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
            Config.load().jenkins.password
        }
        expect("http://localhost:8080/jenkins") {
            Config.load().jenkins.url
        }
        temp.delete()
    }
})
