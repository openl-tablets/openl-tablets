package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * A dependency manager compiled for one project serves another only with the very projects that one needs: two
 * projects of a name, as two versions of one downloaded from the Maven repository, are different projects.
 */
class ProjectModelReuseTest {

    private final ProjectDescriptor uses = project("UsesDomain", "/workspace/UsesDomain");
    private final ProjectDescriptor custom = project("CustomDomain", "/workspace/CustomDomain");
    private final ProjectDescriptor portal = project("Portal", "/workspace/Portal");
    private final ProjectDescriptor domain1 = project("Domain", "/maven/domain/1.0.0/domain-1.0.0-1");
    private final ProjectDescriptor domain2 = project("Domain", "/maven/domain/2.0.0/domain-2.0.0-1");

    @Test
    void reusesAManagerLoadingTheProjectWithWhatItNeeds() {
        assertEquals(ProjectModel.Reuse.REUSE,
                ProjectModel.reuseOf(List.of(portal, uses, domain1, custom), List.of(uses, domain1), uses));
    }

    @Test
    void replacesAManagerLoadingAnotherProjectOfANameTheProjectNeeds() {
        // Portal compiled CustomDomain against Domain 1.0.0; CustomDomain alone needs Domain 2.0.0
        assertEquals(ProjectModel.Reuse.REPLACE,
                ProjectModel.reuseOf(List.of(portal, uses, domain1, custom), List.of(custom, domain2), custom));
    }

    @Test
    void expandsAManagerWhoseProjectsAllBelongToTheWorkspace() {
        assertEquals(ProjectModel.Reuse.EXPAND,
                ProjectModel.reuseOf(List.of(uses, domain1), List.of(portal, uses, domain1, custom), portal));
    }

    @Test
    void replacesAManagerLoadingProjectsTheWorkspaceDoesNotNeed() {
        assertEquals(ProjectModel.Reuse.REPLACE,
                ProjectModel.reuseOf(List.of(custom, domain2), List.of(uses, domain1), uses));
    }

    private static ProjectDescriptor project(String name, String folder) {
        var project = new ProjectDescriptor();
        project.setName(name);
        project.setProjectFolder(Path.of(folder));
        return project;
    }
}
