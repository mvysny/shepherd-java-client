package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class ShepherdClientTest : DynaTest({
    group("Stats") {
        test("empty") {
            expect(Stats(2, 2, 0, 0)) {
                Stats.calculate(Config(2, 2), listOf())
            }
        }
        test("1 project") {
            expect(Stats(2, 2, 256, 2304)) {
                Stats.calculate(Config(2, 2), listOf(fakeProject))
            }
        }
        test("2 projects") {
            expect(Stats(2, 2, 512, 4608)) {
                Stats.calculate(Config(2, 2), listOf(fakeProject, fakeProject2))
            }
        }
        test("2 projects 1 builder") {
            expect(Stats(2,1,  512, 2560)) {
                Stats.calculate(Config(2, 1), listOf(fakeProject, fakeProject2))
            }
        }
    }
})
