package org.openl.rules.repository.git;

import static org.junit.Assert.*;
import static org.openl.rules.repository.git.TestGitUtils.assertContains;
import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class LocalGitRepositoryTest {
    private File root;
    private GitRepository repo;

    @Before
    public void setUp() throws IOException, RRepositoryException {
        root = Files.createTempDirectory("openl").toFile();
        repo = createRepository(root);
    }

    @After
    public void tearDown() throws IOException {
        if (repo != null) {
            repo.close();
        }
        FileUtils.delete(root);
        if (root.exists()) {
            fail("Cannot delete folder " + root);
        }
    }

    @Test
    public void testReadEmpty() throws IOException {
        // Last version
        assertEquals(0, repo.list("").size());
        assertNull(repo.check("project1"));
        assertNull(repo.read("project1/file1"));
        assertEquals(0, repo.listFolders("project1/file1").size());

        // History
        assertEquals(0, repo.listHistory("project1").size());
        assertEquals(0, repo.listFiles("project1", "not-exist").size());
        assertNull(repo.checkHistory("project1", "not-exist"));
        assertNull(repo.readHistory("project1", "not-exist"));
    }

    @Test
    public void testSaveFile() throws IOException {
        String path = "rules/project1/folder/file4";
        String text = "File located in " + path;
        FileData result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("John Smith", result.getAuthor());
        assertEquals("Comment for rules/project1/folder/file4", result.getComment());
        assertEquals(text.length(), result.getSize());
        assertNotNull(result.getModifiedAt());

        assertEquals(text, IOUtils.toStringAndClose(repo.read("rules/project1/folder/file4").getStream()));
    }

    @Test
    public void testSaveFolder() throws IOException {
        List<FileItem> changes = Arrays.asList(
            new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
            new FileItem("rules/project1/file2", IOUtils.toInputStream("Modified")));

        FileData folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor("John Smith");
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes, ChangesetType.FULL);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());
    }

    @Test
    public void testBranches() throws IOException {
        try {
            repo.createBranch("project1", "project1/test1");
            fail("Must fail when create a branch on empty repository");
        } catch (IOException e) {
            assertEquals("Can't create a branch on the empty repository.", e.getMessage());
        }

        String text = "Some text";
        repo.save(createFileData("initial.txt", text), IOUtils.toInputStream(text));

        repo.createBranch("project1", "project1/test1");
        repo.createBranch("project1", "project1/test2");
        List<String> branches = repo.getBranches("project1");
        assertTrue(branches.contains(Constants.MASTER));
        assertTrue(branches.contains("project1/test1"));
        assertTrue(branches.contains("project1/test2"));
        assertEquals(3, branches.size());
    }

    private GitRepository createRepository(File local) throws RRepositoryException {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setGitSettingsPath(local.getParent() + "/git-settings");
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.initialize();

        return repo;
    }
}
