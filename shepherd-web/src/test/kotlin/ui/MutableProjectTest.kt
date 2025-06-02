package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import kotlin.test.Test
import kotlin.test.expect

class MutableProjectTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @Test fun smoke() {
        fakeProject.toMutable()
    }

    @Test fun allInformationCopiedCorrectly() {
        expect(fakeProject) { fakeProject.toMutable().toProject(FakeShepherdClient()) }
    }

    @Test fun testGitUrls() {
        listOf(
            "https://github.com/mvysny/shepherd-java-client",
            "git@github.com:mvysny/shepherd-java-client.git",
            "ssh://user@host.xz:port/path/to/repo.git/",
            "ssh://user@host.xz/path/to/repo.git/",
            "ssh://host.xz:port/path/to/repo.git/",
            "ssh://host.xz/path/to/repo.git/",
            "ssh://user@host.xz/path/to/repo.git/",
            "ssh://host.xz/path/to/repo.git/",
            "ssh://user@host.xz/~user/path/to/repo.git/",
            "ssh://host.xz/~user/path/to/repo.git/",
            "ssh://user@host.xz/~/path/to/repo.git",
            "ssh://host.xz/~/path/to/repo.git",
            "user@host.xz:/path/to/repo.git/",
            "host.xz:/path/to/repo.git/",
            "user@host.xz:~user/path/to/repo.git/",
            "host.xz:~user/path/to/repo.git/",
            "user@host.xz:path/to/repo.git",
            "host.xz:path/to/repo.git",
            "rsync://host.xz/path/to/repo.git/",
            "git://host.xz/path/to/repo.git/",
            "git://host.xz/~user/path/to/repo.git/",
            "http://host.xz/path/to/repo.git/",
            "https://host.xz/path/to/repo.git/",
        ).forEach {
            val m = fakeProject.toMutable()
            m.gitRepoURL = it
            m.validate()
        }
    }
}
