package org.openl.studio.deployment.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.deployment.service.DeploymentService;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.model.ProjectIdModel;

class DeploymentsControllerTest {

    @Test
    void get_deployment_reports_not_found_when_no_deployment_matches() {
        var service = mock(DeploymentService.class);
        when(service.getDeployments(any())).thenReturn(List.of());
        var controller = new DeploymentsController(service, mock(ProjectIdentityConverter.class));
        var id = ProjectIdModel.builder().repository("prod").projectName("Missing").build().encode();

        assertThrows(NotFoundException.class, () -> controller.getDeployment(id));
    }

    @Test
    void get_deployments_can_filter_by_deployed_project() {
        var service = mock(DeploymentService.class);
        var deployments = List.of(
                deployment("prod", "Service One", "Alpha", "Beta"),
                deployment("prod", "Service Two", "Beta"));
        when(service.getDeployments(any())).thenReturn(deployments);
        var controller = new DeploymentsController(service, mock(ProjectIdentityConverter.class));

        var result = controller.getDeployments("prod", "Alpha");

        assertEquals(1, result.size());
        var deployment = result.getFirst();
        assertEquals("Service One", deployment.name);
        assertEquals(1, deployment.items.size());
        assertEquals("Alpha", deployment.items.getFirst().name);
    }

    @Test
    void get_deployment_omits_item_audit_fields_when_project_metadata_fails() {
        var service = mock(DeploymentService.class);
        var project = mock(AProject.class);
        when(project.getName()).thenReturn("Alpha");
        when(project.getFileData()).thenThrow(new IllegalStateException("metadata failed"));
        var deployments = List.of(deployment("prod", "Service One", project));
        when(service.getDeployments(any())).thenReturn(deployments);
        var controller = new DeploymentsController(service, mock(ProjectIdentityConverter.class));
        var id = ProjectIdModel.builder().repository("prod").projectName("Service One").build().encode();

        var result = controller.getDeployment(id);

        assertEquals("Service One", result.name);
        assertEquals(1, result.items.size());
        var item = result.items.getFirst();
        assertEquals("Alpha", item.name);
        assertNull(item.modifiedBy);
        assertNull(item.modifiedAt);
        assertNull(item.revision);
    }

    private static Deployment deployment(String repositoryId, String deploymentName, String... projectNames) {
        return deployment(repositoryId, deploymentName, List.of(projectNames).stream()
                .map(DeploymentsControllerTest::project)
                .toList());
    }

    private static Deployment deployment(String repositoryId, String deploymentName, IProject... projects) {
        return deployment(repositoryId, deploymentName, List.of(projects));
    }

    private static Deployment deployment(String repositoryId, String deploymentName, List<IProject> projects) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);

        var deployment = mock(Deployment.class);
        when(deployment.getRepository()).thenReturn(repository);
        when(deployment.getDeploymentName()).thenReturn(deploymentName);
        when(deployment.getName()).thenReturn(deploymentName);
        when(deployment.getProjects()).thenReturn(projects);
        return deployment;
    }

    private static IProject project(String name) {
        var project = mock(IProject.class);
        when(project.getName()).thenReturn(name);
        return project;
    }
}
