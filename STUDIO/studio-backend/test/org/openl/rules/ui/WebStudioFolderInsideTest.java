package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Renaming a project moves its folder to the new project name, so a name that leads outside the repository
 * folder must be refused.
 */
class WebStudioFolderInsideTest {

    @TempDir
    File root;

    @ParameterizedTest
    @ValueSource(strings = {"rules/Project", "Project", "rules/../Project"})
    void resolvesPathsInsideTheRoot(String path) throws IOException {
        assertEquals(new File(root, path).getCanonicalFile(), WebStudio.folderInside(root, path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../Project", "rules/../../Project", "", "."})
    void refusesPathsOutsideTheRoot(String path) throws IOException {
        assertNull(WebStudio.folderInside(root, path));
    }
}
