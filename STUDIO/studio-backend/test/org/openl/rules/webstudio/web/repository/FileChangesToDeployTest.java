package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
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
        return changes(DeploymentExtras.NONE);
    }

    private FileChangesToDeploy changes(DeploymentExtras extras) {
        when(designRepository.getRepository("design")).thenReturn(repository);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(true).build());
        var descriptor = ProjectDescriptorImpl.builder()
                .repositoryId("design")
                .projectName("P16638")
                .path("P16638")
                .projectVersion(new CommonVersionImpl("1"))
                .build();
        return new FileChangesToDeploy(List.of(descriptor), designRepository, "", "deployment/", "admin", extras);
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
    void writesTheLibrariesAndTheDownloadedProjectsAfterTheFilesOfTheProjects(@TempDir Path temp) throws Exception {
        var project = mock(AProject.class);
        when(project.getName()).thenReturn("P16638");
        when(designRepository.getProjectByPath(any(), any(), any(), any())).thenReturn(project);
        when(repository.listFiles("P16638/", "1")).thenReturn(List.of(file("P16638/rules.xml"),
                file("P16638/lib/kept.jar")));
        when(repository.readHistory(anyString(), any())).thenAnswer(
                call -> new FileItem(call.<String>getArgument(0), new ByteArrayInputStream(new byte[0])));
        var kept = Files.writeString(temp.resolve("kept.jar"), "downloaded");
        var added = Files.writeString(temp.resolve("added.jar"), "downloaded");
        var domain = Files.createDirectories(temp.resolve("domain-1.0.0-1/rules"));
        Files.writeString(domain.resolveSibling("rules.xml"), "<project/>");
        Files.writeString(domain.resolve("Main.xlsx"), "");
        var extras = new DeploymentExtras(Map.of("P16638", List.of(kept, added)),
                Map.of("Domain", domain.getParent()));

        var written = new ArrayList<String>();
        try (var changes = changes(extras)) {
            for (var item : changes) {
                written.add(item.getData().getName());
            }
        }

        assertEquals(List.of("deployment/P16638/META-INF/MANIFEST.MF",
                "deployment/P16638/rules.xml",
                "deployment/P16638/lib/kept.jar",
                "deployment/P16638/lib/added.jar",
                "deployment/Domain/rules.xml",
                "deployment/Domain/rules/Main.xlsx"), written);
    }

    private static FileData file(String name) {
        var file = new FileData();
        file.setName(name);
        file.setVersion("1");
        return file;
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
