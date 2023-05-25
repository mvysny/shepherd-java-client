package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList

class FakeShepherdClientTest : DynaTest({
    group("getAllProjects()") {
        test("smoke") {
            FakeShepherdClient.getAllProjects()
        }
        test("initially contains one fake project") {
            expectList(ProjectId("vaadin-boot-example-gradle")) { FakeShepherdClient.getAllProjects() }
        }
    }
    group("getProjectInfo()") {
        test("smoke") {
            FakeShepherdClient.getProjectInfo(ProjectId("vaadin-boot-example-gradle"))
        }
    }
})
