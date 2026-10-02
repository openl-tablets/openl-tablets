package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.FileData;
import org.openl.studio.projects.model.files.FileNode;

class FileNodeMapperImplTest {

    private final FileNodeMapperImpl mapper = new FileNodeMapperImpl();

    @Test
    void mapsTheModificationTimeToUtc() {
        var node = (FileNode) mapper.map(file(new Date(1_721_000_000_123L)));

        assertEquals(ZonedDateTime.parse("2024-07-14T23:33:20.123Z"), node.getLastModified());
        assertEquals(42L, node.getSize());
    }

    @Test
    void leavesTheModificationTimeEmptyWhenTheRepositoryHasNone() {
        var node = (FileNode) mapper.map(file(null));

        assertNull(node.getLastModified());
    }

    private static AProjectArtefact file(Date modifiedAt) {
        var data = new FileData();
        data.setSize(42L);
        data.setModifiedAt(modifiedAt);
        var artefact = mock(AProjectArtefact.class);
        when(artefact.getInternalPath()).thenReturn("rules/Main.xlsx");
        when(artefact.getName()).thenReturn("Main.xlsx");
        when(artefact.getFileData()).thenReturn(data);
        return artefact;
    }
}
