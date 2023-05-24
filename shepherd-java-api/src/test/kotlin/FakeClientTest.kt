package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList

class FakeClientTest : DynaTest({
    group("getAllProjects()") {
        test("smoke") {
            FakeClient.getAllProjects()
        }
        test("initially contains one fake project") {
            expectList(ProjectId("vaadin-boot-example-gradle")) { FakeClient.getAllProjects() }
        }
    }
    group("getProjectInfo()") {
        test("smoke") {
            FakeClient.getProjectInfo(ProjectId("vaadin-boot-example-gradle"))
        }
    }
})
