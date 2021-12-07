package org.openl.rules.repository.folder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() {
        root = new File("target/test-file-repository/");
        repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
    }

    @After
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
        FileItem result = repo.read(name);
        assertNotNull("The file is not found.", result);
        FileData data = result.getData();
        assertNotNull("The file descriptor is missing.", data);
        assertEquals("Wrong file name", name, data.getName());
        InputStream stream = result.getStream();
        String text = IOUtils.toStringAndClose(stream);
        assertEquals("Unexpected content in the file.", value, text);
    }

}