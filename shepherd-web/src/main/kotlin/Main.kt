package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.vaadinboot.VaadinBoot

fun main() {
    Bootstrap.client = FakeShepherdClient().withFakeProject()
    VaadinBoot().run()
}
