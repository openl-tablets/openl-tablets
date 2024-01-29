package org.openl.rules.repository.folder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class CombinedFileChangesTest {
    private File root;
    private FileSystemRepository repo;

    @BeforeEach
    public void setUp() {
        root = new File("target/test-file-repository/");
        repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
    }

    @AfterEach
    public void tearDown() {
        FileUtils.deleteQuietly(root);
    }

    @Test
    public void testSaveMultipleFolders() throws IOException {
        FileData folder = new FileData();
        String folderName = "deployments/my-deployment1";
        folder.setName(folderName);

        final String file1 = "deployments/my-deployment1/project1/file1";
        final String file2 = "deployments/my-deployment1/project1/rules/file2";
        List<FileItem> project1Changes = Arrays.asList(
            createFileItem(file1, "hello1"),
            createFileItem(file2, "hello2")
        );

        final String file3 = "deployments/my-deployment1/project2/file1";
        final String file4 = "deployments/my-deployment1/project2/rules/file2";
        List<FileItem> project2Changes = Arrays.asList(
            createFileItem(file3, "hello3"),
            createFileItem(file4, "hello4")
        );

        CombinedFileChanges changes = new CombinedFileChanges(Arrays.asList(
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
        FileData file = new FileData();
        file.setName(fileName);
        return new FileItem(file, IOUtils.toInputStream(text));
    }

    private void assertRead(Repository repo, String name, String value) throws IOException {
        try (var result = repo.read(name)) {
            assertNotNull(result, "The file is not found.");
            FileData data = result.getData();
            assertNotNull(data, "The file descriptor is missing.");
            assertEquals(name, data.getName(), "Wrong file name");
            InputStream stream = result.getStream();
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(value, text, "Unexpected content in the file.");
        }
    }

}