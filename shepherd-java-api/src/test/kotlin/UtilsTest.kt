package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class UtilsTest : DynaTest({
    test("exec") {
        val stdout = exec("ls", "-la")
        expect(true, stdout) { stdout.contains("build.gradle.kts") }
    }
})
