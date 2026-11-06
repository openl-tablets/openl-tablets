package org.openl.itest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * Moves and deletes project folders of a design repository on disk, outside OpenL Studio, between the phases of
 * {@code test-resources-stable-project-id}: the ids the projects had keep serving the moved project and tell of the
 * deleted one while the server runs.
 *
 * <p>A project created afterwards in the folder of the deleted one starts with the access of its creator only, not
 * with the access the deleted project left behind.
 *
 * @author Yury Molchan
 */
class StableProjectIdTest {

    private static final String RESOURCES = "test-resources-stable-project-id/";
    private static final String MOVED = "EPBDS-16946-Moved";
    private static final String REMOVED = "EPBDS-16946-Removed";

    @Test
    void theIdsOfProjectsChangedOnDiskLeadToWhatBecameOfThem() throws Exception {
        try (var client = JettyServer.get().start()) {
            client.localEnv.put("ADMIN_AUTH_TOCKEN", "Basic YWRtaW46YWRtaW4=");
            // Each phase runs on its own, so the names every phase uses are given to all of them here.
            client.localEnv.put("MOVED_NAME", MOVED);
            client.localEnv.put("REMOVED_NAME", REMOVED);
            client.localEnv.put("USER", "user16938");
            client.test(RESOURCES + "010-setup");

            var repository = homeOfTheRunningServer().resolve("repositories/design-file");
            var archive = Files.createDirectories(repository.resolve("archive"));
            Files.move(repository.resolve(MOVED), archive.resolve(MOVED));
            deleteFolder(repository.resolve(REMOVED));

            client.test(RESOURCES + "020-folders-changed-on-disk");
            client.test(RESOURCES + "030-a-project-created-in-the-deleted-folder");
            client.test(RESOURCES + "999-tierdown");
        }
    }

    /**
     * The home of the server started last: every start resolves a home of its own, named after the moment it
     * started.
     */
    private static Path homeOfTheRunningServer() throws IOException {
        try (var homes = Files.list(Path.of("target"))) {
            return homes.filter(home -> home.getFileName().toString().startsWith("openl-test-"))
                    .max(Comparator.comparing(home -> home.getFileName().toString()))
                    .orElseThrow();
        }
    }

    private static void deleteFolder(Path folder) throws IOException {
        try (var tree = Files.walk(folder)) {
            for (var path : tree.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
