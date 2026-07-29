package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;

class MappedRepositoryTest {

    @TempDir
    Path root;

    /**
     * A project whose rules.xml has a blank name must be mapped under its folder name.
     *
     * An empty business name later resolves the local workspace path to "/", which escapes the
     * repository root on open. Such a descriptor cannot be produced through the UI, only on disk,
     * so the fallback is verified at the repository level. See {@code ProjectDescriptor#fillProjectName}.
     */
    @Test
    void blankRulesXmlNameFallsBackToFolderName() throws IOException {
        writeProject("no-name-project", "<project><name></name></project>");
        writeProject("absent-name-project", "<project></project>");
        writeProject("named-project", "<project><name>RealName</name></project>");

        var mapped = listMappedFolders("DESIGN/");

        assertEquals(3, mapped.size(), "All projects must be mapped: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/no-name-project:")),
                "Blank rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/absent-name-project:")),
                "Missing rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/RealName:")),
                "Named project must keep its rules.xml name, but was: " + mapped);
    }

    @Test
    void branchViewBuildsMappingFromSelectedBranch() throws Exception {
        var main = branchRepository("main", "rules/main-project", "MainProject");
        var feature = branchRepository("feature/rates", "features/rates-project", "RatesProject");
        when(main.forBranch("feature/rates")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            assertEquals(List.of("MainProject"), businessNames(mapped.listFolders("DESIGN/")));

            var featureMapped = mapped.forBranch("feature/rates");
            try {
                assertEquals(List.of("RatesProject"), businessNames(featureMapped.listFolders("DESIGN/")));
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void nonBaseBranchUsesRequestedMappedFolderForANewProject() throws Exception {
        var feature = branchRepository("feature", "existing/project", "Existing");
        var mapped = MappedRepository.create(feature, "DESIGN/");
        try {
            var project = fileData("DESIGN/NewProject");
            project.addAdditionalData(FileMappingData.forProject(
                    "DESIGN/NewProject", "custom/path", "NewProject"));
            var descriptor = new FileItem(
                    "DESIGN/NewProject/rules.xml",
                    new ByteArrayInputStream("<project><name>NewProject</name></project>"
                            .getBytes(StandardCharsets.UTF_8)));

            mapped.save(project, List.of(descriptor), ChangesetType.FULL);

            var folderCaptor = org.mockito.ArgumentCaptor.forClass(FileData.class);
            verify(feature).save(folderCaptor.capture(), any(), eq(ChangesetType.FULL));
            assertEquals("custom/path/NewProject", folderCaptor.getValue().getName());
        } finally {
            mapped.close();
        }
    }

    @Test
    void mappingScanDoesNotLoadDeletionOrAuditMetadata() throws Exception {
        var repository = mock(BranchRepository.class);
        var folder = mock(FileData.class);
        var descriptor = mock(FileData.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(true).build());
        when(folder.getName()).thenReturn("project");
        when(repository.listFolders("")).thenReturn(List.of(folder));
        when(repository.check("project/rules.xml")).thenReturn(descriptor);
        when(repository.read("project/rules.xml")).thenReturn(new FileItem(
                descriptor,
                new ByteArrayInputStream(
                        "<project><name>Project</name></project>".getBytes(StandardCharsets.UTF_8))));

        var mapped = MappedRepository.create(repository, "DESIGN/");
        try {
            verify(folder, never()).isDeleted();
            verify(folder, never()).getModifiedAt();
            verify(descriptor, never()).isDeleted();
            verify(descriptor, never()).getModifiedAt();
        } finally {
            mapped.close();
        }
    }

    @Test
    void identicalBranchTreesReuseMapping() throws Exception {
        var main = branchRepository("main", "rules/project", "Project", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project", "Project", "descriptor-1", "tree-1");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("Project"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).listFolders("");
                verify(feature, never()).read("rules/project/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void descriptorRevisionIsParsedOnceAcrossDifferentBranchTrees() throws Exception {
        var main = branchRepository("main", "rules/project", "Project", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project", "Project", "descriptor-1", "tree-2");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("Project"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).read("rules/project/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    @Test
    void blankDescriptorNameUsesEachBranchFolderWhenBlobIsReused() throws Exception {
        var main = branchRepository("main", "rules/project-one", "", "descriptor-1", "tree-1");
        var feature = branchRepository("feature", "rules/project-two", "", "descriptor-1", "tree-2");
        when(main.forBranch("feature")).thenReturn(feature);

        var mapped = (BranchRepository) MappedRepository.create(main, "DESIGN/");
        try {
            var featureMapped = mapped.forBranch("feature");
            try {
                assertEquals(List.of("project-two"), businessNames(featureMapped.listFolders("DESIGN/")));
                verify(feature, never()).read("rules/project-two/rules.xml");
            } finally {
                featureMapped.close();
            }
        } finally {
            mapped.close();
        }
    }

    private static BranchRepository branchRepository(String branch, String path, String projectName)
            throws IOException {
        return branchRepository(branch, path, projectName, null, null);
    }

    private static BranchRepository branchRepository(String branch,
                                                      String path,
                                                      String projectName,
                                                      @Nullable String descriptorRevision,
                                                      @Nullable String treeRevision) throws IOException {
        var repository = mock(BranchRepository.class);
        var folder = fileData(path);
        var descriptor = fileData(path + "/rules.xml");
        descriptor.setUniqueId(descriptorRevision);
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setFolders(true).setBranches(true).build());
        when(repository.getBranch()).thenReturn(branch);
        when(repository.getBaseBranch()).thenReturn("main");
        if (treeRevision != null) {
            when(repository.getBranchTreeRevisions(List.of(branch), ""))
                    .thenReturn(Map.of(branch, new BranchTreeRevision(branch + "-tip", treeRevision)));
        }
        when(repository.listFolders("")).thenReturn(List.of(folder));
        when(repository.check(path)).thenReturn(folder);
        when(repository.check(path + "/rules.xml")).thenReturn(descriptor);
        when(repository.read(path + "/rules.xml")).thenAnswer(invocation -> new FileItem(
                descriptor,
                new ByteArrayInputStream(
                        "<project><name>%s</name></project>".formatted(projectName)
                                .getBytes(StandardCharsets.UTF_8))));
        return repository;
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static List<String> businessNames(List<FileData> folders) {
        return folders.stream()
                .map(FileData::getName)
                .map(name -> name.substring("DESIGN/".length(), name.indexOf(':')))
                .toList();
    }

    private List<String> listMappedFolders(String baseFolder) throws IOException {
        var delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();

        Repository mapped = MappedRepository.create(delegate, baseFolder);
        try {
            return mapped.listFolders(baseFolder).stream().map(FileData::getName).toList();
        } finally {
            if (mapped instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }

    private void writeProject(String folder, String rulesXml) throws IOException {
        var projectFolder = root.resolve(folder);
        Files.createDirectories(projectFolder);
        Files.writeString(projectFolder.resolve("rules.xml"), rulesXml);
    }

}
