package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

/**
 * The folder a project of the Maven repository is unpacked into is read as a repository, and never written.
 *
 * @author Yury Molchan
 */
class DownloadedProjectRepositoryTest {

    private static final String RULES_XML = "<project><name>Domain</name></project>";

    @TempDir
    Path downloads;

    @TempDir
    Path userDir;

    private DownloadedProjectRepository repository;
    private FileData rulesXml;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(downloads.resolve("domain-1.0.0"));
        Files.writeString(downloads.resolve("domain-1.0.0/rules.xml"), RULES_XML);
        repository = new DownloadedProjectRepository("maven:com.example:domain:1.0.0",
                downloads,
                MetainfoRegistry.open(userDir));
        rulesXml = new FileData();
        rulesXml.setName("domain-1.0.0/rules.xml");
    }

    @Test
    void isTheMavenRepositoryOfTheProjectItHolds() {
        assertEquals("maven:com.example:domain:1.0.0", repository.getId());
        assertEquals("Maven", repository.getName());
        assertEquals("maven", DownloadedProjectRepository.TYPE);
        assertTrue(repository.isReadOnly());
        assertEquals(downloads, repository.getRoot());
    }

    @Test
    void readsTheFilesOfTheProject() throws IOException {
        assertEquals(List.of("domain-1.0.0/rules.xml"),
                repository.list("domain-1.0.0/").stream().map(FileData::getName).toList());
        try (var item = repository.read("domain-1.0.0/rules.xml")) {
            assertEquals(RULES_XML, new String(item.getStream().readAllBytes()));
        }
    }

    @Test
    void refusesEveryWriteWhoeverAsks() {
        var folder = new FileData();
        folder.setName("domain-1.0.0");
        var item = new FileItem(rulesXml, new ByteArrayInputStream(new byte[0]));

        assertRefused(() -> repository.save(rulesXml, new ByteArrayInputStream(new byte[0])));
        assertRefused(() -> repository.save(List.of(item)));
        assertRefused(() -> repository.save(folder, List.of(item), ChangesetType.FULL));
        assertRefused(() -> repository.delete(rulesXml));
        assertRefused(() -> repository.delete(List.of(rulesXml)));
        assertRefused(() -> repository.deleteHistory(folder));
        assertRefused(() -> repository.copyHistory("domain-1.0.0", folder, null));
    }

    @Test
    void leavesTheFilesAsTheyWereDownloaded() throws IOException {
        assertRefused(() -> repository.delete(rulesXml));

        assertEquals(RULES_XML, Files.readString(downloads.resolve("domain-1.0.0/rules.xml")));
    }

    private static void assertRefused(Write write) {
        var refusal = assertThrows(AccessDeniedException.class, write::run);
        assertTrue(refusal.getMessage().contains("read-only"), refusal.getMessage());
    }

    /** A write into the repository. */
    @FunctionalInterface
    private interface Write {
        void run() throws IOException;
    }
}
