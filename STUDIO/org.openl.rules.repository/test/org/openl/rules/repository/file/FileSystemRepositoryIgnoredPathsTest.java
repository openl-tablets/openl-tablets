package org.openl.rules.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

/**
 * A repository on the file system keeps the ignored paths out of reach by itself, whoever uses it, and matches a path
 * a request names as the file system resolves it.
 */
class FileSystemRepositoryIgnoredPathsTest {

    @TempDir
    Path root;

    private LocalRepository repository;

    @BeforeEach
    void init() throws IOException {
        write("p/rules.xml", "rules");
        write("p/Rules.xlsx", "workbook");
        write("p/pom.xml", "version 1");
        write("p/target/Rules.zip", "build");
        write(".git/config", "[core]");
        repository = new LocalRepository();
        repository.setRoot(root);
        repository.setIgnoredPaths("**/pom.xml, **/target/**");
        repository.initialize();
    }

    private void write(String path, String content) throws IOException {
        var file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private String contentOf(String path) throws IOException {
        return Files.readString(root.resolve(path));
    }

    private static FileItem item(String name, String content) {
        return new FileItem(name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static List<String> namesOf(List<FileData> files) {
        return files.stream().map(FileData::getName).sorted().toList();
    }

    @Test
    void neitherListsNorReadsIgnoredFiles() throws IOException {
        assertEquals(List.of("p/Rules.xlsx", "p/rules.xml"), namesOf(repository.list("")));
        assertEquals(List.of(), repository.list("p/target/"));
        assertEquals(List.of("p"), namesOf(repository.listFolders("")));
        assertEquals(List.of(), namesOf(repository.listFolders("p/")));
        assertNull(repository.check("p/pom.xml"));
        assertNull(repository.read(".git/config"));
        assertEquals(List.of(), repository.listHistory("p/pom.xml"));
    }

    @Test
    void neitherWritesNorDeletesIgnoredFiles() throws IOException {
        assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData(".git/hooks/pre-commit"), new ByteArrayInputStream(new byte[0])));
        assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData("p/target"), List.of(), ChangesetType.DIFF));
        assertThrows(AccessDeniedException.class,
                () -> repository.copyHistory("p/rules.xml", fileData("p/pom.xml"), null));
        assertThrows(FileNotFoundException.class,
                () -> repository.copyHistory("p/pom.xml", fileData("p/copy.xml"), null));
        assertEquals(List.of("p/new.txt"),
                namesOf(repository.save(List.of(item("p/new.txt", "new"), item("p/pom.xml", "version 2")))));
        assertFalse(repository.delete(fileData("p/pom.xml")));
        assertFalse(repository.delete(List.of(fileData(".git/config"))));

        assertFalse(Files.exists(root.resolve(".git/hooks/pre-commit")));
        assertEquals("version 1", contentOf("p/pom.xml"));
        assertEquals("[core]", contentOf(".git/config"));
    }

    @Test
    void aFullSaveKeepsTheIgnoredFilesOfTheFolder() throws IOException {
        repository.save(fileData("p"),
                List.of(item("p/rules.xml", "new rules"), item("p/pom.xml", "version 2")),
                ChangesetType.FULL);

        assertEquals("new rules", contentOf("p/rules.xml"));
        assertFalse(Files.exists(root.resolve("p/Rules.xlsx")));
        assertEquals("version 1", contentOf("p/pom.xml"));
        assertEquals("build", contentOf("p/target/Rules.zip"));
    }

    @Test
    void aChangeOfAnIgnoredFileLeavesTheVersionOfTheFolder() throws IOException {
        var version = repository.check("p").getVersion();

        write("p/pom.xml", "version 2");
        write("p/target/Rules.zip", "build 2");

        assertEquals(version, repository.check("p").getVersion());

        write("p/rules.xml", "new rules");

        assertNotEquals(version, repository.check("p").getVersion());
    }

    @Test
    void tellsTheIgnoredFilesOfAProjectFolder() {
        assertTrue(repository.ignores(root.resolve("p/target")));
        assertTrue(repository.ignores(root.resolve("p/target/classes/Rules.xlsx")));
        assertTrue(repository.ignores(root.resolve("p/../p/pom.xml")));
        assertFalse(repository.ignores(root.resolve("p/rules/Rules.xlsx")));
        assertFalse(repository.ignores(root.resolveSibling("target")), "a path outside the repository");
    }

    /** A link is followed: the path is matched as the file system resolves it. */
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Creating a link needs a privilege on Windows")
    void aLinkToAnIgnoredFolderDoesNotGetAroundIt() throws IOException {
        Files.createSymbolicLink(root.resolve("p/config"), root.resolve(".git"));

        assertNull(repository.read("p/config/config"));
        assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData("p/config/hooks/pre-commit"), new ByteArrayInputStream(new byte[0])));
        assertFalse(Files.exists(root.resolve(".git/hooks/pre-commit")));
        assertTrue(Files.exists(root.resolve(".git/config")));
    }
}
