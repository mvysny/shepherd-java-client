package com.github.mvysny.shepherd.api

import org.junit.jupiter.api.Test
import kotlin.test.expect

class UtilsTest {
    @Test fun exec() {
        val stdout = exec("ls", "-la")
        expect(true, stdout) { stdout.contains("build.gradle.kts") }
    }
}
