package org.openl.rules.repository.git;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;

public class GitRepositoryTest {
    private static final String BRANCH = "test";
    private static final String FOLDER_IN_REPOSITORY = "rules/project1/";
    private static final String TAG_PREFIX = "Rules_";
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private GitRepository repo;
    private ChangesCounter changesCounter;

    @Before
    public void setUp() throws GitAPIException, IOException, RRepositoryException {
        File root = tempFolder.getRoot();
        File remote = new File(root, "remote");
        File local = new File(root, "local");

        // Initialize remote repository
        try (Git git = Git.init().setDirectory(remote).call()) {
            Repository repository = git.getRepository();

            File parent = repository.getDirectory().getParentFile();
            File rulesFolder = new File(parent, FOLDER_IN_REPOSITORY);

            // create initial commit in master
            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit()
                    .setMessage("Initial")
                    .setCommitter("user1", "user1@mail.to")
                    .call();
            addTag(git, commit, 1);

            // create first commit in test branch
            git.branchCreate().setName(BRANCH).call();
            git.checkout().setName(BRANCH).call();

            createNewFile(parent, "file-in-test", "root");
            createNewFile(rulesFolder, "file1", "Hi!");
            File file2 = createNewFile(rulesFolder, "file2", "Hello!");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("Initial commit in test branch")
                    .setCommitter("user1", "user1@mail.to")
                    .call();
            addTag(git, commit, 2);

            // create second commit
            writeText(file2, "Hello World!");
            createNewFile(new File(rulesFolder, "folder"), "file3", "In folder");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setAll(true)
                    .setMessage("Second modification")
                    .setCommitter("user2", "user2@gmail.to")
                    .call();
            addTag(git, commit, 3);

            // create commit in master
            git.checkout().setName("master").call();
            createNewFile(rulesFolder, "file1master", "root");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("Additional commit in master")
                    .setCommitter("user1", "user1@mail.to")
                    .call();
            addTag(git, commit, 4);
        }

        repo = new GitRepository();
        repo.setUri(remote.toURI().toString());
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setBranch(BRANCH);
        repo.setTagPrefix(TAG_PREFIX);
        repo.initialize();

        changesCounter = new ChangesCounter();
        repo.setListener(changesCounter);
    }

    @After
    public void tearDown() {
        repo.close();
    }

    @Test
    public void list() throws IOException {
        assertEquals(5, repo.list("").size());

        List<FileData> files = repo.list("rules/project1/");
        assertNotNull(files);
        assertEquals(3, files.size());

        FileData file1 = getFileData(files, "rules/project1/file1");
        assertNotNull(file1);
        assertEquals("user1", file1.getAuthor());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        FileData file2 = getFileData(files, "rules/project1/file2");
        assertNotNull(file2);
        assertEquals("user2", file2.getAuthor());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        FileData file3 = getFileData(files, "rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("user2", file3.getAuthor());
        assertEquals("Second modification", file3.getComment());
        assertEquals(9, file3.getSize());
    }

    @Test
    public void listFolders() throws IOException {
        assertEquals(1, repo.listFolders("").size());

        List<FileData> folders = repo.listFolders("rules/");
        assertNotNull(folders);
        assertEquals(1, folders.size());

        FileData folderData = folders.get(0);
        assertEquals("rules/project1/", folderData.getName());
    }

    @Test
    public void check() throws IOException {
        FileData file1 = repo.check("rules/project1/file1");
        assertNotNull(file1);
        assertEquals("user1", file1.getAuthor());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        FileData file2 = repo.check("rules/project1/file2");
        assertNotNull(file2);
        assertEquals("user2", file2.getAuthor());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        FileData file3 = repo.check("rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("user2", file3.getAuthor());
        assertEquals("Second modification", file3.getComment());
        assertEquals(9, file3.getSize());
    }

    @Test
    public void read() throws IOException {
        assertEquals("Hi!", IOUtils.toStringAndClose(repo.read("rules/project1/file1").getStream()));
        assertEquals("Hello World!", IOUtils.toStringAndClose(repo.read("rules/project1/file2").getStream()));
        assertEquals("In folder", IOUtils.toStringAndClose(repo.read("rules/project1/folder/file3").getStream()));

        assertEquals(0, changesCounter.getChanges());
    }

    @Test
    public void save() throws IOException, RRepositoryException {
        // Create a new file
        String path = "rules/project1/folder/file4";
        String text = "File located in " + path;
        FileData result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("John Smith", result.getAuthor());
        assertEquals("Comment for rules/project1/folder/file4", result.getComment());
        assertEquals(text.length(), result.getSize());
        assertEquals("Rules_5", result.getVersion());
        assertNotNull(result.getModifiedAt());

        assertEquals(text, IOUtils.toStringAndClose(repo.read("rules/project1/folder/file4").getStream()));

        // Modify existing file
        text = "Modified";
        result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));
        assertNotNull(result);
        assertEquals(text.length(), result.getSize());
        assertEquals("Rules_6", result.getVersion());
        assertEquals(text, IOUtils.toStringAndClose(repo.read("rules/project1/folder/file4").getStream()));

        assertEquals(2, changesCounter.getChanges());

        // Clone remote repository to temp folder and check that changes we made before exist there
        File root = tempFolder.getRoot();
        File remote = new File(root, "remote");
        File temp = new File(root, "temp");
        try (GitRepository secondRepo = new GitRepository()) {
            secondRepo.setUri(remote.toURI().toString());
            secondRepo.setLocalRepositoryPath(temp.getAbsolutePath());
            secondRepo.setBranch(BRANCH);
            secondRepo.setTagPrefix(TAG_PREFIX);
            secondRepo.initialize();
            assertEquals(text, IOUtils.toStringAndClose(secondRepo.read("rules/project1/folder/file4").getStream()));
        }

        // Check that creating new folders works correctly
        path = "rules/project1/new-folder/file5";
        text = "File located in " + path;
        assertNotNull(repo.save(createFileData(path, text), IOUtils.toInputStream(text)));
    }

    @Test
    public void saveFolder() throws IOException {
        List<FileChange> changes = Arrays.asList(
                new FileChange("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                new FileChange("rules/project1/file2", IOUtils.toInputStream("Modified"))
        );

        FileData folderData = new FileData();
        folderData.setName("rules/project1/");
        folderData.setAuthor("John Smith");
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());
    }

    @Test
    public void delete() throws IOException {
        FileData fileData = new FileData();
        fileData.setName("rules/project1/file2");
        fileData.setComment("Delete file 2");
        fileData.setAuthor("John Smith");
        boolean deleted = repo.delete(fileData);
        assertTrue("'file2' wasn't deleted", deleted);

        assertNull("'file2' still exists", repo.check("rules/project1/file2"));
    }

    @Test
    public void copy() throws IOException {
        FileData dest = new FileData();
        dest.setName("rules/project1/file2-copy");
        dest.setComment("Copy file 2");
        dest.setAuthor("John Smith");
        FileData copy = repo.copy("rules/project1/file2", dest);
        assertNotNull(copy);
        assertEquals("rules/project1/file2-copy", copy.getName());
        assertEquals("John Smith", copy.getAuthor());
        assertEquals("Copy file 2", copy.getComment());
        assertEquals(12, copy.getSize());

        assertNotNull(repo.check("rules/project1/file2"));
        assertNotNull(repo.check("rules/project1/file2-copy"));
    }

    @Test
    public void rename() throws IOException {
        FileData dest = new FileData();
        dest.setName("rules/project1/file2-copy");
        dest.setComment("Copy file 2");
        dest.setAuthor("John Smith");
        FileData renamed = repo.rename("rules/project1/file2", dest);
        assertNotNull(renamed);
        assertEquals("rules/project1/file2-copy", renamed.getName());
        assertEquals("John Smith", renamed.getAuthor());
        assertEquals("Copy file 2", renamed.getComment());
        assertEquals(12, renamed.getSize());

        assertNull("'file2' wasn't deleted", repo.check("rules/project1/file2"));
        assertNotNull("'file2-copy' doesn't exist", repo.check("rules/project1/file2-copy"));
    }

    @Test
    public void listHistory() throws IOException {
        List<FileData> file2History = repo.listHistory("rules/project1/file2");
        assertEquals(2, file2History.size());
        assertEquals("Rules_2", file2History.get(0).getVersion());
        assertEquals("Rules_3", file2History.get(1).getVersion());
    }

    @Test
    public void checkHistory() throws IOException {
        assertEquals("Rules_2", repo.checkHistory("rules/project1/file2", "Rules_2").getVersion());
        assertEquals("Rules_3", repo.checkHistory("rules/project1/file2", "Rules_3").getVersion());
        assertNull(repo.checkHistory("rules/project1/file2", "Rules_1"));
    }

    @Test
    public void readHistory() throws IOException {
        assertEquals("Hello!", IOUtils.toStringAndClose(repo.readHistory("rules/project1/file2", "Rules_2").getStream()));
        assertEquals("Hello World!", IOUtils.toStringAndClose(repo.readHistory("rules/project1/file2", "Rules_3").getStream()));
        assertNull(repo.readHistory("rules/project1/file2", "Rules_1"));
    }

    @Test
    public void copyHistory() throws IOException {
        FileData dest = new FileData();
        dest.setName("rules/project1/file2-copy");
        dest.setComment("Copy file 2");
        dest.setAuthor("John Smith");

        FileData copy = repo.copyHistory("rules/project1/file2", dest, "Rules_2");
        assertNotNull(copy);
        assertEquals("rules/project1/file2-copy", copy.getName());
        assertEquals("John Smith", copy.getAuthor());
        assertEquals("Copy file 2", copy.getComment());
        assertEquals(6, copy.getSize());
        assertEquals("Rules_5", copy.getVersion());
        assertEquals("Hello!", IOUtils.toStringAndClose(repo.read("rules/project1/file2-copy").getStream()));
    }

    @Test
    public void changesShouldBeRolledBackOnError() throws Exception {
        try {
            FileData data = new FileData();
            data.setName("rules/project1/file2");
            data.setAuthor(null);
            data.setComment(null);
            repo.save(data, IOUtils.toInputStream("error"));
            fail("Exception should be thrown");
        } catch (IOException e) {
            assertEquals("Name of PersonIdent must not be null.", e.getCause().getMessage());
        }

        // Check that there are no uncommitted changes after error
        try (Git git = Git.open(new File(tempFolder.getRoot(), "local"))) {
            Status status = git.status().call();
            assertTrue(status.getUncommittedChanges().isEmpty());
        }
    }

    private FileData createFileData(String path, String text) {
        FileData fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment("Comment for " + path);
        fileData.setAuthor("John Smith");
        return fileData;
    }

    private FileData getFileData(List<FileData> files, String fileName) {
        for (FileData fileData : files) {
            if (fileName.equals(fileData.getName())) {
                return fileData;
            }
        }
        return null;
    }

    private File createNewFile(File parent, String fileName, String text) throws IOException {
        if (!parent.mkdirs() && !parent.exists()) {
            throw new IOException("Could not create folder " + parent);
        }
        File file = new File(parent, fileName);
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + file);
        }
        writeText(file, text);
        return file;
    }

    private void writeText(File file, String text) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.displayName())) {
            writer.append(text);
        }
    }

    private void addTag(Git git, RevCommit commit, int version) throws GitAPIException {
        git.tag().setObjectId(commit).setName(TAG_PREFIX + version).call();
    }

    private void assertContains(List<FileData> files, String fileName) {
        boolean contains = false;
        for (FileData file : files) {
            if (fileName.equals(file.getName())) {
                contains = true;
                break;
            }
        }

        assertTrue("Files list doesn't contain the file '" + fileName + "'", contains);
    }

    private static class ChangesCounter implements Listener {
        private int changes = 0;

        @Override
        public void onChange() {
            changes++;
        }

        public int getChanges() {
            return changes;
        }
    }
}