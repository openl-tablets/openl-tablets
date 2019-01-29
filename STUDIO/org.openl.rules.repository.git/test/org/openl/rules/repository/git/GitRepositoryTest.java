package org.openl.rules.repository.git;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class GitRepositoryTest {
    private static final String BRANCH = "test";
    private static final String FOLDER_IN_REPOSITORY = "rules/project1/";
    private static final String TAG_PREFIX = "Rules_";

    private File root;
    private GitRepository repo;
    private ChangesCounter changesCounter;

    @Before
    public void setUp() throws GitAPIException, IOException, RRepositoryException {
        root = Files.createTempDirectory(null).toFile();

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
            git.checkout().setName(Constants.MASTER).call();
            createNewFile(rulesFolder, "file1master", "root");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("Additional commit in master")
                    .setCommitter("user1", "user1@mail.to")
                    .call();
            addTag(git, commit, 4);
        }

        repo = createRepository(remote, local);

        changesCounter = new ChangesCounter();
        repo.setListener(changesCounter);
    }

    @After
    public void tearDown() {
        if (repo != null) {
            repo.close();
        }
        FileUtils.deleteQuietly(root);
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
        assertEquals("rules/project1", folderData.getName());
    }

    @Test
    public void listFiles() throws IOException {
        List<FileData> files = repo.listFiles("rules/project1/", "Rules_2");
        assertNotNull(files);
        assertEquals(2, files.size());
        assertContains(files, "rules/project1/file1");
        assertContains(files, "rules/project1/file2");

        FileData file1Rev2 = find(files, "rules/project1/file1");
        assertEquals("Rules_2", file1Rev2.getVersion());

        FileData file2Rev2 = find(files, "rules/project1/file2");
        assertEquals("Rules_2", file2Rev2.getVersion());
        assertEquals("user1", file2Rev2.getAuthor());
        assertEquals("Initial commit in test branch", file2Rev2.getComment());
        assertEquals("Expected file content: 'Hello!'",6, file2Rev2.getSize());

        files = repo.listFiles("rules/project1/", "Rules_3");
        assertNotNull(files);
        assertEquals(3, files.size());
        assertContains(files, "rules/project1/file1");
        assertContains(files, "rules/project1/file2");
        assertContains(files, "rules/project1/folder/file3");

        FileData file1Rev3 = find(files, "rules/project1/file1");
        assertEquals("Rules_2", file1Rev3.getVersion()); // The file wasn't modified in second commit

        FileData file2Rev3 = find(files, "rules/project1/file2");
        assertEquals("Rules_3", file2Rev3.getVersion());
        assertEquals("user2", file2Rev3.getAuthor());
        assertEquals("Second modification", file2Rev3.getComment());
        assertEquals("Expected file content: 'Hello World!'",12, file2Rev3.getSize());

        FileData file3Rev3 = find(files, "rules/project1/folder/file3");
        assertEquals("Rules_3", file3Rev3.getVersion());
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

        FileData project1 = repo.check("rules/project1");
        assertNotNull(project1);
        assertEquals("rules/project1", project1.getName());
        assertEquals("user2", project1.getAuthor());
        assertEquals("Second modification", project1.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, project1.getSize());
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
        File remote = new File(root, "remote");
        File temp = new File(root, "temp");
        try (GitRepository secondRepo = createRepository(remote, temp)) {
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
        folderData.setName("rules/project1");
        folderData.setAuthor("John Smith");
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());

        for (FileChange file : changes) {
            IOUtils.closeQuietly(file.getStream());
        }
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

        // Count actual changes in history
        String projectPath = "rules/project1";
        assertEquals(3, repo.listHistory(projectPath).size());

        // Archive the project
        FileData projectData = new FileData();
        projectData.setName(projectPath);
        projectData.setComment("Delete project1");
        projectData.setAuthor("John Smith");
        assertTrue("'project1' wasn't deleted", repo.delete(projectData));

        FileData deletedProject = repo.check(projectPath);
        assertTrue("'project1' isn't deleted", deletedProject.isDeleted());

        // Restore the project
        FileData toDelete = new FileData();
        toDelete.setName(projectPath);
        toDelete.setVersion(deletedProject.getVersion());
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertFalse("'project1' isn't restored", deletedProject.isDeleted());

        // Count actual changes in history
        assertEquals("Actual project changes must be 3. Technical commits for Archive/Restore shouldn't be counted",
                3,
                repo.listHistory(projectPath).size());

        // Erase the project
        toDelete.setName(projectPath);
        toDelete.setVersion(null);
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertNull("'project1' isn't erased", deletedProject);

        // Life after erase
        assertEquals(3, repo.listHistory(projectPath).size());
        // Create new version
        String text = "Reincarnation";
        repo.save(createFileData(projectPath, text), IOUtils.toInputStream(text));
        assertEquals(4, repo.listHistory(projectPath).size());
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

        List<FileData> project1History = repo.listHistory("rules/project1");
        assertEquals(2, project1History.size());
        assertEquals("Rules_2", project1History.get(0).getVersion());
        assertEquals("Rules_3", project1History.get(1).getVersion());
    }

    @Test
    public void checkHistory() throws IOException {
        assertEquals("Rules_2", repo.checkHistory("rules/project1/file2", "Rules_2").getVersion());
        assertEquals("Rules_3", repo.checkHistory("rules/project1/file2", "Rules_3").getVersion());
        assertNull(repo.checkHistory("rules/project1/file2", "Rules_1"));

        FileData v3 = repo.checkHistory("rules/project1", "Rules_3");
        assertEquals("Rules_3", v3.getVersion());
        assertEquals("user2", v3.getAuthor());

        FileData v2 = repo.checkHistory("rules/project1", "Rules_2");
        assertEquals("Rules_2", v2.getVersion());
        assertEquals("user1", v2.getAuthor());

        assertNull(repo.checkHistory("rules/project1", "Rules_1"));
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

        FileData destProject = new FileData();
        destProject.setName("rules/project2");
        destProject.setComment("Copy of project1");
        destProject.setAuthor("John Smith");
        FileData project2 = repo.copyHistory("rules/project1", destProject, "Rules_2");
        assertNotNull(project2);
        assertEquals("rules/project2", project2.getName());
        assertEquals("John Smith", project2.getAuthor());
        assertEquals("Copy of project1", project2.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, project2.getSize());
        assertEquals("Rules_6", project2.getVersion());
        List<FileData> project2Files = repo.list("rules/project2/");
        assertEquals(2, project2Files.size());
        assertContains(project2Files, "rules/project2/file1");
        assertContains(project2Files, "rules/project2/file2");
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
        try (Git git = Git.open(new File(root, "local"))) {
            Status status = git.status().call();
            assertTrue(status.getUncommittedChanges().isEmpty());
        }
    }

    @Test
    public void repoFolderExistsButEmpty() throws RRepositoryException, IOException {
        // Prepare the test: the folder with local repository name exists but it's empty
        repo.close();

        File remote = new File(root, "remote");
        File local = new File(root, "local");
        FileUtils.deleteQuietly(local);
        assertFalse("Can't delete repository. It shouldn't be locked.", local.exists());

        if (!local.mkdirs() && !local.exists()) {
            fail("Can't create the folder for test");
        }

        // Check that repo is cloned successfully
        try (GitRepository repository = createRepository(remote, local)) {
            assertEquals(5, repository.list("").size());
        }
        // Reuse cloned before repository. Must not fail.
        try (GitRepository repository = createRepository(remote, local)) {
            assertEquals(5, repository.list("").size());
        }
    }

    @Test
    public void neededBranchWasNotClonedBefore() throws RRepositoryException, IOException {
        // Prepare the test: clone master branch
        File remote = new File(root, "remote");
        File local = new File(root, "temp");
        try (GitRepository repository = createRepository(remote, local, Constants.MASTER)) {
            assertEquals(2, repository.list("").size());
        }

        // Check: second time initialize the repo. At this time use the branch "test". It must be pulled
        // successfully and repository must be switched to that branch.
        try (GitRepository repository = createRepository(remote, local)) {
            assertEquals(5, repository.list("").size());
        }
    }

    @Test
    public void twoUsersAddFileSimultaneously() throws RRepositoryException, IOException {
        // Prepare the test: clone master branch
        File remote = new File(root, "remote");
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        // First user starts to save it's changes
        try (GitRepository repository1 = createRepository(remote, local1)) {
            String text = "New file";

            // Second user is quicker than first
            FileData saved2;
            try (GitRepository repository2 = createRepository(remote, local2)) {
                saved2 = repository2.save(createFileData("rules/project-second/file2", text),
                        IOUtils.toInputStream(text));
            }

            // First user doesn't suspect that second user already committed his changes
            FileData saved1 = repository1.save(createFileData("rules/project-first/file1", text),
                    IOUtils.toInputStream(text));

            // Check that the changes of both users are persist and merged
            assertNotEquals("Versions of two changes must be different.", saved1.getVersion(), saved2.getVersion());
            assertEquals("5 files existed and 2 files must be added (must be 7 files in total).", 7, repository1.list("").size());
            assertEquals("Rules_6", saved1.getVersion());
            assertEquals("Rules_5", saved2.getVersion());
        }
    }

    private GitRepository createRepository(File remote, File local) throws RRepositoryException {
        return createRepository(remote, local, BRANCH);
    }

    private GitRepository createRepository(File remote, File local, String branch) throws RRepositoryException {
        GitRepository repo = new GitRepository();
        repo.setUri(remote.toURI().toString());
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setBranch(branch);
        repo.setTagPrefix(TAG_PREFIX);
        repo.initialize();

        return repo;
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

    private FileData find(List<FileData> files, String fileName) {
        for (FileData file : files) {
            if (fileName.equals(file.getName())) {
                return file;
            }
        }

        throw new IllegalArgumentException("File " + fileName + " not found");
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