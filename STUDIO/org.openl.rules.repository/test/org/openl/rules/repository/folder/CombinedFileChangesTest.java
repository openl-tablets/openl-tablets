package org.openl.rules.repository.folder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.IOUtils;

class CombinedFileChangesTest {

    @TempDir
    private Path root;
    @AutoClose
    private FileSystemRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
    }

    @Test
    void testSaveMultipleFolders() throws IOException {
        var folder = new FileData();
        var folderName = "deployments/my-deployment1";
        folder.setName(folderName);

        final var file1 = "deployments/my-deployment1/project1/file1";
        final var file2 = "deployments/my-deployment1/project1/rules/file2";
        var project1Changes = Arrays.asList(
                createFileItem(file1, "hello1"),
                createFileItem(file2, "hello2")
        );

        final var file3 = "deployments/my-deployment1/project2/file1";
        final var file4 = "deployments/my-deployment1/project2/rules/file2";
        var project2Changes = Arrays.asList(
                createFileItem(file3, "hello3"),
                createFileItem(file4, "hello4")
        );

        var changes = new CombinedFileChanges(Arrays.asList(
                project1Changes,
                project2Changes
        ));

        repo.save(folder, changes, ChangesetType.FULL);

        assertRead(repo, file1, "hello1");
        assertRead(repo, file2, "hello2");
        assertRead(repo, file3, "hello3");
        assertRead(repo, file4, "hello4");
    }

    private FileItem createFileItem(String fileName, String text) {
        var file = new FileData();
        file.setName(fileName);
        return new FileItem(file, IOUtils.toInputStream(text));
    }

    private void assertRead(Repository repo, String name, String value) throws IOException {
        try (var result = repo.read(name)) {
            assertNotNull(result, "The file is not found.");
            var data = result.getData();
            assertNotNull(data, "The file descriptor is missing.");
            assertEquals(name, data.getName(), "Wrong file name");
            var stream = result.getStream();
            var text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(value, text, "Unexpected content in the file.");
        }
    }

}
