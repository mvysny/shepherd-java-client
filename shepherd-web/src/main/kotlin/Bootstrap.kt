package com.github.mvysny.shepherd.web

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.server.PWA
import jakarta.servlet.ServletContextListener
import jakarta.servlet.annotation.WebListener

@WebListener
class Bootstrap : ServletContextListener

@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
class AppShell : AppShellConfigurator