package org.openl.rules.workspace.lw.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

class LocalWorkspaceImplTest {

    @TempDir
    Path userDir;

    private MetainfoRegistry registry;
    private LocalWorkspaceImpl workspace;

    @BeforeEach
    void init() {
        registry = MetainfoRegistry.open(userDir);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");
        workspace = new LocalWorkspaceImpl("jdoe", userDir.toFile(), designTimeRepository, registry, false);
    }

    @Test
    void aSnapshotKeepsTheProjectsOfItsMoment() throws IOException {
        var before = workspace.snapshot();

        // Another request opens a copy and refreshes the workspace.
        seedCopy("P1", "rules/P1");
        workspace.refresh();
        var after = workspace.snapshot();

        assertTrue(before.projects().isEmpty(), "A snapshot does not follow a later refresh.");
        var copy = after.forFolder("design", "rules/P1");
        assertNotNull(copy, "A new snapshot finds the copy by its folder.");
        assertSame(copy, after.forName("design", "p1"), "A name is found whatever its case.");
        assertSame(copy, after.projects().iterator().next());
    }

    @Test
    void aSnapshotListsTheProjectsByName() throws IOException {
        seedCopy("Second", "rules/second");
        seedCopy("first", "rules/first");
        workspace.refresh();

        var names = workspace.snapshot().projects().stream().map(AProject::getName).toList();

        assertEquals(List.of("first", "Second"), names, "The order stays the same from one start to the next.");
    }

    private void seedCopy(String name, String folder) throws IOException {
        Files.createDirectories(userDir.resolve(name));
        registry.save(name,
                new ProjectMetainfo("design", folder, "main", "rev-1", "jdoe", 1751980000000L, 7L, null, Map.of()));
    }
}
