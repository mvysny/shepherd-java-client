package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.KFormLayout
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bigDecimalField
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.checkBox
import com.github.mvysny.karibudsl.v10.emailField
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.integerField
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.karibudsl.v23.multiSelectComboBox
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ServiceType
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.host
import com.github.mvysny.shepherd.web.security.checkProjectId
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.github.mvysny.shepherd.web.ui.components.Form
import com.github.mvysny.shepherd.web.ui.components.namedVarSetField
import com.github.mvysny.shepherd.web.ui.components.simpleStringSetField
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.function.SerializablePredicate
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route("project/edit", layout = MainLayout::class)
@PageTitle("Edit Project")
@PermitAll
class EditProjectRoute : KComposite(), HasUrlParameter<String> {
    private lateinit var captionComponent: H2
    private lateinit var form: ProjectForm
    private lateinit var project: MutableProject
    private val layout = ui {
        verticalLayout {
            captionComponent = h2()
            form = projectForm(false)
            button("Save & Apply") {
                onClick { save() }
            }
        }
    }

    override fun setParameter(event: BeforeEvent, parameter: String) {
        val project = checkProjectId(parameter)
        captionComponent.text = project.id.id
        this.project = project.toMutable()
        form.binder.readBean(this.project)
    }

    private fun save() {
        if (!form.writeIfValid(project)) {
            return
        }

        Bootstrap.getClient().updateProject(project.toProject())
        navigateTo<ProjectListRoute>()
    }
}

/**
 * Edits [MutableProject] in unbuffered mode. Unbuffered because of nested mutable sets.
 */
class ProjectForm(val creatingNew: Boolean) : KFormLayout(), Form<MutableProject> {
    override val binder: Binder<MutableProject> = beanValidationBinder()

    private val config = Bootstrap.getClient().getConfig()

    init {
        val isAdmin = getCurrentUser().isAdmin

        textField("The project ID, must be unique. The project will be published and running at https://$host/PROJECT_ID") {
            isEnabled = creatingNew // can't change project ID
            bind(binder).bind(MutableProject::id)
        }
        textField("Description: Any additional vital information about the project") {
            bind(binder).bind(MutableProject::description)
        }
        textField("WebPage: the project home page. May be empty, in such case GitRepo URL is considered the home page.") {
            bind(binder).bind(MutableProject::webpage)
        }
        h3("Git Repository") {
            colspan = 2
        }
        textField("GIT Repository URL: the git repository from where the project comes from, e.g. https://github.com/mvysny/vaadin-boot-example-gradle") {
            bind(binder).bind(MutableProject::gitRepoURL)
        }
        textField("GIT Repository branch: usually `master` or `main`") {
            bind(binder).bind(MutableProject::gitRepoBranch)
        }
        textField("GIT Repository Credentials ID") {
            isVisible = isAdmin
            bind(binder).bind(MutableProject::gitRepoCredentialsID)
        }
        h3("Owner") {
            colspan = 2
        }
        textField("Project Owner Name. Only Shepherd admin can create projects for someone else.") {
            isEnabled = isAdmin
            bind(binder).bind(MutableProject::projectOwnerName)
        }
        emailField("How to reach the project owner in case the project needs to be modified/misbehaves. Jenkins will send notification emails about the failed builds here.") {
            isEnabled = isAdmin
            bind(binder).bind(MutableProject::projectOwnerEmail)
        }
        h3("Runtime") {
            colspan = 2
        }
        integerField("How much memory the project needs for running, in MB. Please try to keep the memory requirements as low as possible, " +
                "so that we can host as many projects as possible. 256MB is a good default, but Spring Boot project may require 350MB or more. " +
                "WARNING: This is a HARD limit for the app. If JVM asks for more, it will be hard-killed by the Linux OOM-killer, " +
                "without any warning or any log message (only host OS dmesg will log this). Make sure to have your Dockerfile run Java with the -Xmx???m VM argument; that way the app will crash with OutOfMemoryException which should be visible in the logs. " +
                "The -Xmx value should be a bit lower value than the hard limit, to give a bit of room for JVM itself.") {
            bind(binder)
                .withValidator(SerializablePredicate { it == null || it <= config.maxProjectRuntimeResources.memoryMb }, "Can not be larger than ${config.maxProjectRuntimeResources.memoryMb}")
                .bind(MutableProject::runtimeMemoryMb)
        }
        bigDecimalField("Max CPU cores to use. 1 means 1 CPU core to be used.") {
            isEnabled = isAdmin
            bind(binder)
                .withValidator(SerializablePredicate { it == null || it <= config.maxProjectRuntimeResources.cpu.toBigDecimal() }, "Can not be larger than ${config.maxProjectRuntimeResources.cpu}")
                .bind(MutableProject::runtimeCpu)
        }
        namedVarSetField("Runtime environment variables, e.g. `SPRING_DATASOURCE_URL` to `jdbc:postgresql://liukuri-postgres:5432/postgres`") {
            bind(binder).bind(MutableProject::envVars)
        }
        namedVarSetField("Optional build args, passed as `--build-arg name=\"value\"` to `docker build`. You can e.g. pass Vaadin Offline Key here.") {
            bind(binder).bind(MutableProject::buildArgs)
        }
        textField("If not null, we build off this dockerfile instead of the default `Dockerfile`") {
            bind(binder).bind(MutableProject::buildDockerFile)
        }
        h3("Publishing") {
            colspan = 2
        }
        checkBox("Publish the project on the main domain as well, at `$host/PROJECT_ID`.") {
            bind(binder).bind(MutableProject::publishOnMainDomain)
        }
        simpleStringSetField("Additional domains to publish to project at. Must not contain the main domain $host. E.g. `yourproject.com`. You need to configure your domain DNS record to point to $host IP address first!") {
            hint = "Enter your domain and press the PLUS button"
            bind(binder).bind(MutableProject::publishAdditionalDomains)
        }
        checkBox(
            "Use https for additional domains. Only affects additional domains: if the project is published on the main domain ($host) then it always uses https. " +
                    "If checked, the project is exposed on port 443 as https; https certificate is obtained automatically from Let's Encrypt. Make sure to configure your domain DNS record to point to $host IP address first! " +
                    "If unchecked, the project is exposed on port 80 as plain http. This is " +
                    "useful e.g. when CloudFlare unwraps https for us. Ignored if there are no additional domains."
        ) {
            bind(binder).bind(MutableProject::publishAdditionalDomainsHttps)
        }
        integerField("Max request body size, in megabytes, defaults to 1m. Increase if you intend your project to accept large file uploads.") {
            bind(binder).bind(MutableProject::ingressMaxBodySizeMb)
        }
        integerField("Proxy Read Timeout, in seconds, defaults to 60s. Increase to 6 minutes or more if you use Vaadin Push, otherwise the connection will be dropped out. Alternatively, set this to 3 minutes and set Vaadin heartbeat frequency to 2 minutes.") {
            bind(binder).bind(MutableProject::ingressProxyReadTimeoutSeconds)
        }
        h3("Additional Services") {
            colspan = 2
        }
        multiSelectComboBox<ServiceType>("Additional services accessible by your project. If you enable PostgreSQL, then use the following values to access the database: JDBC URI: `jdbc:postgresql://postgres-service:5432/postgres`, username: `postgres`, password: `mysecretpassword`.") {
            setItems(ServiceType.entries)
            bind(binder).bind(MutableProject::additionalServices)
        }
    }

    override fun additionalValidation(bean: MutableProject) {
        bean.validate()
        if (creatingNew) {
            if (Bootstrap.getClient().existsProject(ProjectId(bean.id!!))) {
                throw RuntimeException("${bean.id}: project already exists")
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).projectForm(creatingNew: Boolean, block: ProjectForm.() -> Unit = {}) = init(ProjectForm(creatingNew), block)
