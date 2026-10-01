package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;

class WebStudioProjectsTest {

    private static final String REPOSITORY_ID = "local";

    @TempDir
    private Path root;
    private Environment previousEnvironment;

    @BeforeEach
    void setUp() {
        // A new session reads its settings.
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
    }

    @AfterEach
    void restoreSettings() {
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void resolvesTheProjectsOfTheLocalWorkspaceAndLeavesOutTheOnesThatFail() throws IOException {
        Files.createDirectories(root.resolve("Rates"));
        Files.writeString(root.resolve("Rates").resolve(ProjectDescriptor.FILE_NAME),
                "<project><name>Rates</name></project>");

        var repository = mock(LocalRepository.class);
        when(repository.getRoot()).thenReturn(root);
        when(repository.getId()).thenReturn(REPOSITORY_ID);
        var rates = mock(AProject.class);
        when(rates.getRepository()).thenReturn(repository);
        when(rates.getFolderPath()).thenReturn("Rates");
        var broken = mock(AProject.class);
        when(broken.getRepository()).thenThrow(new IllegalStateException("The repository is gone"));

        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getRepository(REPOSITORY_ID)).thenReturn(repository);
        doReturn(List.of(broken, rates)).when(localWorkspace).getProjects();
        var userWorkspace = mock(UserWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        var session = mock(RulesUserSession.class);
        when(session.getUserWorkspace()).thenReturn(userWorkspace);

        var studio = new WebStudio(session, null, null, null, null, null, null);

        var projects = studio.getProjects().get(REPOSITORY_ID);
        assertEquals(List.of("Rates"), projects.stream().map(ProjectDescriptor::getName).toList());
    }
}
