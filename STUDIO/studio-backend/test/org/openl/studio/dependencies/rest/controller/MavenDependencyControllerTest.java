package org.openl.studio.dependencies.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.dependencies.model.MavenProjectSummary;
import org.openl.studio.dependencies.service.MavenProjectViewer;

class MavenDependencyControllerTest {

    private final MavenProjectViewer viewer = mock(MavenProjectViewer.class);
    private final MavenDependencyController controller = new MavenDependencyController(viewer);

    @Test
    void listsTheProjectsTheWorkspaceDependsOn() {
        var domain = new MavenProjectSummary("com.example", "domain", "1.0.0", "Domain");
        when(viewer.projects()).thenReturn(List.of(domain));

        assertEquals(List.of(domain), controller.getProjects());
    }
}
