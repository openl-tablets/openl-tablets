package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.RulesDeploy;

class WebStudioRulesDeployTest {

    private final WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);
    private final RulesProject project = mock(RulesProject.class);

    @BeforeEach
    void setUp() {
        doReturn(project).when(studio).getCurrentProject();
    }

    @Test
    void readsTheDeployConfigurationOfTheProject() throws ProjectException {
        var resource = mock(AProjectResource.class);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream(
                "<rules-deploy><serviceName>hello</serviceName></rules-deploy>".getBytes(StandardCharsets.UTF_8)));
        when(project.hasArtefact(RulesDeploy.FILE_NAME)).thenReturn(true);
        when(project.getArtefact(RulesDeploy.FILE_NAME)).thenReturn(resource);

        assertEquals("hello", studio.getCurrentProjectRulesDeploy().getServiceName());
    }

    @Test
    void hasNoDeployConfigurationWithoutItsFile() {
        assertNull(studio.getCurrentProjectRulesDeploy());
    }

    @Test
    void hasNoDeployConfigurationWithoutAnOpenedProject() {
        doReturn(null).when(studio).getCurrentProject();

        assertNull(studio.getCurrentProjectRulesDeploy());
    }

    @Test
    void hasNoDeployConfigurationWhenItsFileHasGone() throws ProjectException {
        when(project.hasArtefact(RulesDeploy.FILE_NAME)).thenReturn(true);
        when(project.getArtefact(RulesDeploy.FILE_NAME)).thenThrow(new ProjectException("Gone"));

        assertNull(studio.getCurrentProjectRulesDeploy());
    }

    @Test
    void hasNoDeployConfigurationInAFolder() throws ProjectException {
        when(project.hasArtefact(RulesDeploy.FILE_NAME)).thenReturn(true);
        var folder = mock(AProjectFolder.class);
        when(project.getArtefact(RulesDeploy.FILE_NAME)).thenReturn(folder);

        assertNull(studio.getCurrentProjectRulesDeploy());
    }
}
