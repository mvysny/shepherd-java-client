package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.KFormLayout
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.checkBox
import com.github.mvysny.karibudsl.v10.emailField
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.h4
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
import com.github.mvysny.shepherd.web.showErrorNotification
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.validation.ValidationException
import java.io.FileNotFoundException

@Route("edit", layout = MainLayout::class)
@PageTitle("Edit Project")
class EditProjectRoute : KComposite(), HasUrlParameter<String> {
    private lateinit var captionComponent: H2
    private val form: ProjectForm = ProjectForm(false)
    private lateinit var project: MutableProject
    private val layout = ui {
        verticalLayout {
            captionComponent = h2()
            add(form)
            button("Save & Apply") {
                onClick { save() }
            }
        }
    }

    override fun setParameter(event: BeforeEvent, parameter: String) {
        val project = try {
            Bootstrap.getClient().getProjectInfo(ProjectId(parameter))
        } catch (_: FileNotFoundException) {
            throw NotFoundException()
        }
        captionComponent.text = project.id.id
        this.project = project.toMutable()
        form.binder.bean = this.project
    }

    private fun save() {
        if (!form.binder.validate().isOk) {
            showErrorNotification("There are errors in the form")
            return
        }
        try {
            project.validate()
        } catch (e: ValidationException) {
            showErrorNotification("Error: " + e.message)
            return
        }

        Bootstrap.getClient().updateProject(project.toProject())
        navigateTo<ProjectListRoute>()
    }
}

class ProjectForm(val creatingNew: Boolean) : KFormLayout() {
    val binder: Binder<MutableProject> = beanValidationBinder()
    private val gitRepoCredentialsIDField: TextField
    private val projectOwnerNameField: TextField
    private val projectOwnerEmailField: EmailField

    init {
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
        h4("Git Repository") {
            colspan = 2
        }
        textField("GIT Repository URL: the git repository from where the project comes from, e.g. https://github.com/mvysny/vaadin-boot-example-gradle") {
            bind(binder).bind(MutableProject::gitRepoURL)
        }
        textField("GIT Repository URL: the git repository from where the project comes from, e.g. https://github.com/mvysny/vaadin-boot-example-gradle") {
            bind(binder).bind(MutableProject::gitRepoURL)
        }
        textField("GIT Repository branch: usually `master` or `main`") {
            bind(binder).bind(MutableProject::gitRepoBranch)
        }
        gitRepoCredentialsIDField = textField("GIT Repository Credentials ID") {
            bind(binder).bind(MutableProject::gitRepoCredentialsID)
        }
        h4("Owner") {
            colspan = 2
        }
        projectOwnerNameField = textField("Project Owner Name") {
            bind(binder).bind(MutableProject::projectOwnerName)
        }
        projectOwnerEmailField =
            emailField("How to reach the project owner in case the project needs to be modified/misbehaves. Jenkins will send notification emails about the failed builds here.") {
                bind(binder).bind(MutableProject::projectOwnerEmail)
            }
        h4("Runtime") {
            colspan = 2
        }
        integerField("how much memory the project needs for running, in MB. Please try to keep the memory requirements as low as possible, so that we can host as many projects as possible. 256MB is a good default.") {
            bind(binder).bind(MutableProject::runtimeMemoryMb)
        }
        // todo runtimeCpu
        // todo env vars
        // todo buildArgs
        textField("if not null, we build off this dockerfile instead of the default `Dockerfile`") {
            bind(binder).bind(MutableProject::buildDockerFile)
        }
        h4("Publishing") {
            colspan = 2
        }
        checkBox("if true (the default), the project will be published on the main domain as well, at `$host/PROJECT_ID`.") {
            bind(binder).bind(MutableProject::publishOnMainDomain)
        }
        checkBox(
            "only affects additional domains; if the project is published on the main domain then it always uses https. " +
                    "Defaults to true. If false, the project is published on additional domains via plain http. " +
                    "Useful e.g. when CloudFlare unwraps https for us. Ignored if there are no additional domains."
        ) {
            bind(binder).bind(MutableProject::publishAdditionalDomainsHttps)
        }
        // todo additional domains
        integerField("Max request body size, in megabytes, defaults to 1m. Increase if you intend to upload large files.") {
            bind(binder).bind(MutableProject::ingressMaxBodySizeMb)
        }
        integerField("Proxy Read Timeout, in seconds, defaults to 60s. Increase to 6 minutes or more if you use Vaadin Push, otherwise the connection will be dropped out. Alternatively, set this to 3 minutes and set Vaadin heartbeat frequency to 2 minutes.") {
            bind(binder).bind(MutableProject::ingressProxyReadTimeoutSeconds)
        }
        h4("Additional Services") {
            colspan = 2
        }
        multiSelectComboBox<ServiceType>("additional services, only accessible by your project. If you enable PostgreSQL, then use the following values to access the database: JDBC URI: `jdbc:postgresql://postgres-service:5432/postgres`, username: `postgres`, password: `mysecretpassword`.") {
            setItems(ServiceType.entries)
            bind(binder).bind(MutableProject::additionalServices)
        }
    }

    fun disableFieldsForRegularUser() {
        gitRepoCredentialsIDField.isVisible = false
        projectOwnerNameField.isEnabled = false
        projectOwnerEmailField.isEnabled = false
    }
}
