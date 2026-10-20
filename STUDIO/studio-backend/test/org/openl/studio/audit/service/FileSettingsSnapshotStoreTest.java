package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSettingsSnapshotStoreTest {

    @TempDir
    Path home;

    @Test
    void hasNoSnapshotBeforeTheFirstOne() throws Exception {
        assertEquals(Optional.empty(), new FileSettingsSnapshotStore(home.resolve("new").toString()).load());
    }

    @Test
    void keepsTheSnapshotInTheHomeFolderAndReplacesItWhole() throws Exception {
        var store = new FileSettingsSnapshotStore(home.resolve("new").toString());

        store.save(Map.of("user.mode", "pbkdf2-sha256$1$a$b", "repository.design.password", "pbkdf2-sha256$1$c$d"));
        store.save(Map.of("user.mode", "pbkdf2-sha256$1$e$f"));

        assertEquals(Optional.of(Map.of("user.mode", "pbkdf2-sha256$1$e$f")), store.load());
        assertTrue(Files.exists(home.resolve("new").resolve(FileSettingsSnapshotStore.FILE_NAME)));
        try (var files = Files.list(home.resolve("new"))) {
            assertEquals(1, files.count(), "No temporary file is left behind");
        }
    }
}
