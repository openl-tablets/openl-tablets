package org.openl.rules.workspace.lw.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.InPlaceRepository;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

/**
 * In single-user mode the projects of a design repository on the file system are edited where they are: the workspace
 * lists them from the folder of the repository, with no copy of their own.
 */
class LocalWorkspaceInPlaceTest {

    private static final String RULES = "DESIGN/rules/";

    @TempDir
    Path userDir;

    @TempDir
    Path designRoot;

    private MetainfoRegistry registry;
    private DesignTimeRepository designTimeRepository;

    @BeforeEach
    void init() throws IOException {
        Files.createDirectories(designRoot.resolve("bank-rating/rules"));
        Files.writeString(designRoot.resolve("bank-rating/rules.xml"), "<project><name>Bank Rating</name></project>");
        var files = new org.openl.rules.repository.file.LocalRepository();
        files.setRoot(designRoot);
        files.setId("design");
        files.initialize();
        var design = MappedRepository.create(files, RULES);
        registry = MetainfoRegistry.open(userDir);
        designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRulesLocation()).thenReturn(RULES);
        when(designTimeRepository.getRepository("design")).thenReturn(design);
        var projects = design.listFolders(RULES).stream().map(data -> new AProject(design, data)).toList();
        doReturn(projects).when(designTimeRepository).getProjects();
    }

    @Test
    void aProjectOfAFileRepositoryIsListedFromItsFolder() {
        var project = workspace(true).snapshot().forFolder("design", "bank-rating");

        assertNotNull(project);
        var repository = assertInstanceOf(InPlaceRepository.class, project.getRepository());
        assertEquals(designRoot, repository.getRoot(), "the project is read where the design repository keeps it");
        assertEquals("bank-rating", project.getFolderPath());
        assertEquals("design", repository.getId());
        assertFalse(Files.exists(userDir.resolve("bank-rating")), "nothing is copied into the workspace");
    }

    @Test
    void theProjectsAreCopiedOutsideSingleUserMode() {
        assertTrue(workspace(false).snapshot().projects().isEmpty());
    }

    @Test
    void aRepositoryOtherThanOneOnTheFileSystemIsCopied() {
        var remote = mock(Repository.class);
        when(designTimeRepository.getRepository("design")).thenReturn(remote);

        assertTrue(workspace(true).snapshot().projects().isEmpty());
    }

    @Test
    void theRepositoryOfTheWorkspaceStaysTheWorkspace() {
        var repository = workspace(true).getRepository("design");

        assertFalse(repository instanceof InPlaceRepository);
        assertEquals(userDir, repository.getRoot(), "a copy opened later still goes to the workspace");
    }

    @Test
    void aCopyWithChangesNotSavedStaysTheCopy() throws IOException {
        copy("Bank Rating");
        registry.markDirty("Bank Rating");

        var project = workspace(true).snapshot().forFolder("design", "bank-rating");

        assertNotNull(project);
        assertFalse(project.getRepository() instanceof InPlaceRepository, "its changes are still to be saved");
        assertEquals(userDir, ((LocalRepository) project.getRepository()).getRoot());
    }

    @Test
    void aCopyWithoutChangesGivesWayToTheFolder() throws IOException {
        copy("Bank Rating");

        var project = workspace(true).snapshot().forFolder("design", "bank-rating");

        assertNotNull(project);
        assertInstanceOf(InPlaceRepository.class, project.getRepository());
        assertNull(workspace(true).snapshot().forFolder("design", "Bank Rating"));
    }

    private LocalWorkspaceImpl workspace(boolean inPlace) {
        return new LocalWorkspaceImpl("jdoe", userDir.toFile(), designTimeRepository, registry, inPlace,
                IgnoredPaths.of(null));
    }

    /** A copy of the project opened into the workspace before. */
    private void copy(String name) throws IOException {
        Files.createDirectories(userDir.resolve(name));
        registry.save(name,
                new ProjectMetainfo("design", "bank-rating", null, "rev-1", "jdoe", 1751980000000L, 7L, null, Map.of()));
    }
}
