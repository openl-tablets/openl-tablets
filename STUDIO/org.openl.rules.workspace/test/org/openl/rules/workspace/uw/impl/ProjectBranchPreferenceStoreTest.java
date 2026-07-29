package org.openl.rules.workspace.uw.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectBranchPreferenceStoreTest {

    @TempDir
    Path userDirectory;

    @Test
    void persistsPreferencesAcrossWorkspaceRecreation() {
        var store = ProjectBranchPreferenceStore.open(userDirectory);
        store.put("design", "Rates", "feature/rates");

        var reopened = ProjectBranchPreferenceStore.open(userDirectory);

        assertEquals("feature/rates", reopened.get("design", "rates").orElseThrow());
        reopened.remove("design", "Rates");
        assertTrue(ProjectBranchPreferenceStore.open(userDirectory).get("design", "Rates").isEmpty());
    }

    @Test
    void keepsRepositoryIdentitiesSeparate() {
        var store = ProjectBranchPreferenceStore.open(userDirectory);

        store.put("repo-a", "Same Name", "feature/a");
        store.put("repo-b", "Same Name", "feature/b");

        assertEquals("feature/a", store.get("repo-a", "same name").orElseThrow());
        assertEquals("feature/b", store.get("repo-b", "same name").orElseThrow());
    }
}
