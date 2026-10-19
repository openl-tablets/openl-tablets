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
 * @author Yury Molchan
 */
class StableProjectIdTest {

    private static final String RESOURCES = "test-resources-stable-project-id/";

    @Test
    void theIdsOfProjectsChangedOnDiskLeadToWhatBecameOfThem() throws Exception {
        try (var client = JettyServer.get().start()) {
            client.localEnv.put("ADMIN_AUTH_TOCKEN", "Basic YWRtaW46YWRtaW4=");
            client.test(RESOURCES + "010-setup");

            var repository = homeOfTheRunningServer().resolve("repositories/design-file");
            var archive = Files.createDirectories(repository.resolve("archive"));
            Files.move(repository.resolve("EPBDS-16946-Moved"), archive.resolve("EPBDS-16946-Moved"));
            deleteFolder(repository.resolve("EPBDS-16946-Removed"));

            client.test(RESOURCES + "020-folders-changed-on-disk");
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
