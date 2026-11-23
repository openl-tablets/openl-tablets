package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.repository.api.FileData;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FolderNode;

class FileNodeMapperImplTest {

    private final FileNodeMapperImpl mapper = new FileNodeMapperImpl(
            ProjectFiles.of("pom.xml", "rules.xml, docs/**"));

    @Test
    void mapsTheModificationTimeToUtc() {
        var node = (FileNode) mapper.map(file("rules/Main.xlsx", new Date(1_721_000_000_123L)), project());

        assertEquals(ZonedDateTime.parse("2024-07-14T23:33:20.123Z"), node.getLastModified());
        assertEquals(42L, node.getSize());
        assertFalse(node.isHidden());
    }

    @Test
    void leavesTheModificationTimeEmptyWhenTheRepositoryHasNone() {
        var node = (FileNode) mapper.map(file("rules/Main.xlsx", null), project());

        assertNull(node.getLastModified());
    }

    @Test
    void hidesTheHiddenAndTheProtectedFilesOfAProject() {
        assertTrue(mapper.map(file("rules.xml", null), project()).isHidden());
        assertTrue(mapper.map(file("POM.xml", null), project()).isHidden());
        assertFalse(mapper.map(file("rules/pom.xml", null), project()).isHidden());

        var folder = mock(AProjectArtefact.class);
        when(folder.isFolder()).thenReturn(true);
        when(folder.getInternalPath()).thenReturn("docs");
        when(folder.getName()).thenReturn("docs");
        var node = (FolderNode) mapper.map(folder, project());
        assertTrue(node.isHidden());
        assertTrue(node.withChildren(List.of()).isHidden());
    }

    @Test
    void hidesNothingOutsideAProject() {
        assertFalse(mapper.map(file("rules.xml", null), mock(FileRoot.class)).isHidden());
    }

    private static FileRoot project() {
        var root = mock(FileRoot.class);
        when(root.isProject()).thenReturn(true);
        return root;
    }

    private static AProjectArtefact file(String path, Date modifiedAt) {
        var data = new FileData();
        data.setSize(42L);
        data.setModifiedAt(modifiedAt);
        var artefact = mock(AProjectArtefact.class);
        when(artefact.getInternalPath()).thenReturn(path);
        when(artefact.getName()).thenReturn(path.substring(path.lastIndexOf('/') + 1));
        when(artefact.getFileData()).thenReturn(data);
        return artefact;
    }
}
