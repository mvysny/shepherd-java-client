package com.github.mvysny.shepherd.api;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class ProjectJavaAPITest {
    @Test
    public void testAPI() {
        final ProjectId id = ProjectTestKt.getFakeProject().getId();
        System.out.println(id);
        final String pid = id.getId();
        System.out.println(pid);

        var project = new Project(new ProjectId("foo"), "Foo", "http://bar",
                new GitRepo("git://foo", "main", null),
                new ProjectOwner("Foo", "foo@bar.baz"),
                new ProjectRuntime(Resources.getDefaultRuntimeResources()),
                new BuildSpec(Resources.getDefaultRuntimeResources()),
                new Publication(), new HashSet<>());
        System.out.println(project);
    }
}
