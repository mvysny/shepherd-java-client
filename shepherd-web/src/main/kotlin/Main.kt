package com.github.mvysny.shepherd.web

import com.github.mvysny.vaadinboot.VaadinBoot

var devMode = false

fun main(args: Array<String>) {
    if (args.getOrNull(0) == "dummy") {
        // perfect for development
        println("!!!!!! RUNNING IN DEVELOPMENT MODE !!!!!")
        devMode = true
        Services.newFake()
    }
    VaadinBoot().run()
}
