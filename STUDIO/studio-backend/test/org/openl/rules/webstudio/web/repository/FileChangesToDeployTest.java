package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * The files a deployment carries. A project of a deployment that cannot be read from the design repository stops the
 * deployment, instead of being left out of it without a word.
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
        return new FileChangesToDeploy(List.of(descriptor), designRepository, "", "deployment/", "admin",
                ProjectFiles.of("pom.xml", "rules.xml"));
    }

    /** A project whose every file is left out deploys its manifest, and the deployment goes on with the next one. */
    @Test
    void aProjectOfProtectedFilesOnlyDeploysItsManifest() throws Exception {
        var project = mock(AProject.class);
        when(project.getName()).thenReturn("P16638");
        var projectData = new FileData();
        projectData.setModifiedAt(new Date());
        when(project.getFileData()).thenReturn(projectData);
        when(designRepository.getProjectByPath(any(), any(), any(), any())).thenReturn(project);
        var pom = new FileData();
        pom.setName("P16638/pom.xml");
        when(repository.listFiles("P16638/", "1")).thenReturn(List.of(pom));

        var deployed = new ArrayList<String>();
        try (var changes = changes()) {
            changes.forEach(item -> deployed.add(item.getData().getName()));
        }

        assertEquals(List.of("deployment/P16638/META-INF/MANIFEST.MF"), deployed);
    }

    @Test
    void leavesTheProtectedFilesAndTheOldManifestOut() throws Exception {
        var project = mock(AProject.class);
        when(project.getName()).thenReturn("P16638");
        var projectData = new FileData();
        projectData.setModifiedAt(new Date());
        when(project.getFileData()).thenReturn(projectData);
        when(designRepository.getProjectByPath(any(), any(), any(), any())).thenReturn(project);
        var files = new ArrayList<FileData>();
        for (var name : List.of("rules.xml", "pom.xml", "META-INF/MANIFEST.MF", "rules/Main.xlsx", "rules/pom.xml")) {
            var file = new FileData();
            file.setName("P16638/" + name);
            files.add(file);
        }
        // The listing a repository answers may not be changed.
        when(repository.listFiles("P16638/", "1")).thenReturn(List.copyOf(files));
        when(repository.readHistory(anyString(), any())).thenAnswer(call -> new FileItem(call.<String>getArgument(0),
                new ByteArrayInputStream(new byte[0])));

        var deployed = new ArrayList<String>();
        try (var changes = changes()) {
            changes.forEach(item -> deployed.add(item.getData().getName()));
        }

        assertEquals(List.of("deployment/P16638/META-INF/MANIFEST.MF", "deployment/P16638/rules.xml",
                "deployment/P16638/rules/Main.xlsx", "deployment/P16638/rules/pom.xml"), deployed);
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
