package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.api.NoSuchProjectException
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.web.Bootstrap
import com.vaadin.flow.router.NotFoundException

/**
 * Checks that given [id] is a valid project ID, and the current user has access to the project.
 * @throws NotFoundException on any issue
 */
fun checkProjectId(id: String): Project {
    if (!ProjectId.isValid(id)) {
        throw NotFoundException()
    }
    val pid = ProjectId(id)
    val currentUser = UserLoginService.get().currentUser ?: throw NotFoundException()
    val project = try {
        Bootstrap.getClient().getProjectInfo(pid)
    } catch (_: NoSuchProjectException) {
        throw NotFoundException()
    }
    if (!currentUser.isAdmin && !project.canEdit(currentUser.email)) {
        throw NotFoundException()
    }
    return project
}
