package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Where a project lands inside a non-flat repository. Every way of creating a project — an archive, files,
 * a template, a copy or a publish from the workspace — puts it together the same way.
 */
class FileMappingDataTest {

    @Test
    void projectInTheRootHasNoFolder() {
        assertEquals("Rules", FileMappingData.internalPath(null, "Rules"));
        assertEquals("Rules", FileMappingData.internalPath("  ", "Rules"));
    }

    @Test
    void folderIsSeparatedFromTheProjectName() {
        assertEquals("folder/Rules", FileMappingData.internalPath("folder", "Rules"));
        assertEquals("folder/Rules", FileMappingData.internalPath("folder/", "Rules"));
    }

    @Test
    void folderIsNormalizedBeforeTheProjectNameIsAppended() {
        assertEquals("folder/Rules", FileMappingData.internalPath("/folder", "Rules"));
        assertEquals("folder/nested/Rules", FileMappingData.internalPath("\\folder\\nested", "Rules"));
    }

    @Test
    void mappedProjectCarriesBothItsPaths() {
        var data = FileMappingData.forProject("DESIGN/Rules", "folder", "Rules");

        assertEquals("DESIGN/Rules", data.getExternalPath());
        assertEquals("folder/Rules", data.getInternalPath());
    }
}
