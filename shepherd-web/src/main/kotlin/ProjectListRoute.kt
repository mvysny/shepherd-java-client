package com.github.mvysny.shepherd.web

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h1
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.router.Route

@Route("")
class ProjectListRoute : KComposite() {
    private val layout = ui {
        verticalLayout {
            h1("Welcome!")
        }
    }
}
