package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectKeyTest {

    @Test
    void nullRepositoryId_throws() {
        assertThrows(NullPointerException.class, () -> new ProjectKey(null, "path"));
    }

    @Test
    void nullRepositoryPath_throws() {
        assertThrows(NullPointerException.class, () -> new ProjectKey("repo", null));
    }

    @Test
    void bothNull_throws() {
        assertThrows(NullPointerException.class, () -> new ProjectKey(null, null));
    }

    @Test
    void getters_returnConstructorValues() {
        var key = new ProjectKey("repo-1", "path/to/project");
        assertEquals("repo-1", key.repositoryId());
        assertEquals("path/to/project", key.repositoryPath());
    }

    @Test
    void equals_sameValues() {
        var a = new ProjectKey("repo", "path");
        var b = new ProjectKey("repo", "path");
        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    void equals_differentRepositoryId() {
        var a = new ProjectKey("repo-1", "path");
        var b = new ProjectKey("repo-2", "path");
        assertNotEquals(a, b);
    }

    @Test
    void equals_differentRepositoryPath() {
        var a = new ProjectKey("repo", "path-1");
        var b = new ProjectKey("repo", "path-2");
        assertNotEquals(a, b);
    }

    @Test
    void equals_caseSensitive() {
        var a = new ProjectKey("repo", "path");
        var b = new ProjectKey("REPO", "PATH");
        assertNotEquals(a, b);
    }

    @Test
    void hashCode_consistentWithEquals() {
        var a = new ProjectKey("repo", "path");
        var b = new ProjectKey("repo", "path");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashCode_stableAcrossInvocations() {
        var key = new ProjectKey("repo", "path");
        assertEquals(key.hashCode(), key.hashCode());
    }
}
