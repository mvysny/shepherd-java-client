package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import org.junit.jupiter.api.Test
import kotlin.test.expect

class UtilsTest : DynaTest({
    @Test fun exec() {
        val stdout = exec("ls", "-la")
        expect(true, stdout) { stdout.contains("build.gradle.kts") }
    }
})
