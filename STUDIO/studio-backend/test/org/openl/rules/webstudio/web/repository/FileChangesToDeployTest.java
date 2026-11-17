package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * A project of a deployment that cannot be read from the design repository stops the deployment, instead of being
 * left out of it without a word.
 *
 * @author Yury Molchan
 */
class FileChangesToDeployTest {

    private final DesignTimeRepository designRepository = mock(DesignTimeRepository.class);
    private final Repository repository = mock(Repository.class);

    private FileChangesToDeploy changes() {
        when(designRepository.getRepository("design")).thenReturn(repository);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(true).build());
        var descriptor = ProjectDescriptorImpl.builder()
                .repositoryId("design")
                .projectName("P16638")
                .path("P16638")
                .projectVersion(new CommonVersionImpl("1"))
                .build();
        return new FileChangesToDeploy(List.of(descriptor), designRepository, "", "deployment/", "admin");
    }

    @Test
    void aProjectWithoutFilesStopsTheDeployment() throws Exception {
        var project = mock(AProject.class);
        when(project.getName()).thenReturn("P16638");
        when(designRepository.getProjectByPath(any(), any(), any(), any())).thenReturn(project);
        when(repository.listFiles(anyString(), any())).thenReturn(List.of());

        try (var changes = changes()) {
            var iterator = changes.iterator();
            var error = assertThrows(UnreadableProjectException.class, iterator::hasNext);

            assertEquals("P16638", error.getProjectName());
        }
    }

    @Test
    void aProjectThatCannotBeFoundStopsTheDeployment() throws Exception {
        when(designRepository.getProjectByPath(any(), any(), any(), any())).thenThrow(new IOException("not found"));

        try (var changes = changes()) {
            var iterator = changes.iterator();
            var error = assertThrows(UnreadableProjectException.class, iterator::hasNext);

            assertEquals("The project 'P16638' cannot be read from the design repository.", error.getMessage());
        }
    }
}
