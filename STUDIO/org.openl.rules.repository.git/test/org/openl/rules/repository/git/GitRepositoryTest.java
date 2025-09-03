package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.openl.rules.repository.git.TestGitUtils.assertContains;
import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createNewFile;
import static org.openl.rules.repository.git.TestGitUtils.writeText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.dataformat.yaml.YamlMapperFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.repository.git.branch.BranchesData;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class GitRepositoryTest {
    private static final String BRANCH = "test";
    private static final String FOLDER_IN_REPOSITORY = "rules/project1";
    private static final String TAG_PREFIX = "Rules_";

    @TempDir
    private static File template;
    @TempDir(cleanup = CleanupMode.NEVER)
    private File root;
    private File remote;
    private File local;
    @AutoClose
    private GitRepository repo;
    private ChangesCounter changesCounter;

    @BeforeAll
    public static void initTest() throws GitAPIException, IOException {
        // Initialize remote repository
        try (Git git = Git.init().setDirectory(template).call()) {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            File parent = repository.getDirectory().getParentFile();
            File rulesFolder = new File(parent, FOLDER_IN_REPOSITORY);

            // create initial commit in master
            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setMessage("Initial").setCommitter("User 1", "user1@email.to").call();
            addTag(git, commit, 1);

            // create first commit in test branch
            git.branchCreate().setName(BRANCH).call();
            git.checkout().setName(BRANCH).call();

            createNewFile(parent, "file-in-test", "root");
            createNewFile(rulesFolder, "file1", "Hi.");
            File file2 = createNewFile(rulesFolder, "file2", "Hello.");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("Initial commit in test branch")
                    .setCommitter("User 1", "user1@email.to")
                    .call();
            addTag(git, commit, 2);

            // create second commit
            writeText(file2, "Hello World.");
            createNewFile(new File(rulesFolder, "folder"), "file3", "In folder");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setAll(true)
                    .setMessage("Second modification")
                    .setCommitter("User 2", "user2@email.to")
                    .call();
            addTag(git, commit, 3);

            // create commit in master
            git.checkout().setName(Constants.MASTER).call();
            createNewFile(rulesFolder, "file1master", "root");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("Additional commit in master")
                    .setCommitter("User 1", "user1@email.to")
                    .call();
            addTag(git, commit, 4);
        }
    }

    @BeforeEach
    public void setUp() throws IOException {

        remote = new File(root, "remote");
        local = new File(root, "local");

        FileUtils.copy(template, remote);
        repo = createRepository(remote, local);

        changesCounter = new ChangesCounter();
        repo.setListener(changesCounter);
    }

    @Test
    public void list() throws IOException {
        assertEquals(5, repo.list("").size());

        List<FileData> files = repo.list("rules/project1/");
        assertNotNull(files);
        assertEquals(3, files.size());

        FileData file1 = getFileData(files, "rules/project1/file1");
        assertNotNull(file1);
        assertEquals("User 1", file1.getAuthor().getDisplayName());
        assertEquals("user1@email.to", file1.getAuthor().getEmail());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        FileData file2 = getFileData(files, "rules/project1/file2");
        assertNotNull(file2);
        assertEquals("User 2", file2.getAuthor().getDisplayName());
        assertEquals("user2@email.to", file2.getAuthor().getEmail());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        FileData file3 = getFileData(files, "rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("User 2", file3.getAuthor().getDisplayName());
        assertEquals("user2@email.to", file3.getAuthor().getEmail());
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
        assertEquals("User 1", file2Rev2.getAuthor().getDisplayName());
        assertEquals("user1@email.to", file2Rev2.getAuthor().getEmail());
        assertEquals("Initial commit in test branch", file2Rev2.getComment());
        assertEquals(6, file2Rev2.getSize(), "Expected file content: 'Hello!'");

        files = repo.listFiles("rules/project1/", "Rules_3");
        assertNotNull(files);
        assertEquals(3, files.size());
        assertContains(files, "rules/project1/file1");
        assertContains(files, "rules/project1/file2");
        assertContains(files, "rules/project1/folder/file3");

        // Each file has last modified project version, to performance improve
        // FileData file1Rev3 = find(files, "rules/project1/file1");
        // assertEquals("Rules_2", file1Rev3.getVersion()); // The file has not been modified in second commit

        FileData file2Rev3 = find(files, "rules/project1/file2");
        assertEquals("Rules_3", file2Rev3.getVersion());
        assertEquals("User 2", file2Rev3.getAuthor().getDisplayName());
        assertEquals("user2@email.to", file2Rev3.getAuthor().getEmail());
        assertEquals("Second modification", file2Rev3.getComment());
        assertEquals(12, file2Rev3.getSize(), "Expected file content: 'Hello World!'");

        FileData file3Rev3 = find(files, "rules/project1/folder/file3");
        assertEquals("Rules_3", file3Rev3.getVersion());
    }

    @Test
    public void check() throws IOException {
        FileData file1 = repo.check("rules/project1/file1");
        assertNotNull(file1);
        assertEquals("User 1", file1.getAuthor().getDisplayName());
        assertEquals("user1@email.to", file1.getAuthor().getEmail());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        FileData file2 = repo.check("rules/project1/file2");
        assertNotNull(file2);
        assertEquals("User 2", file2.getAuthor().getDisplayName());
        assertEquals("user2@email.to", file2.getAuthor().getEmail());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        FileData file3 = repo.check("rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("User 2", file3.getAuthor().getDisplayName());
        assertEquals("user2@email.to", file3.getAuthor().getEmail());
        assertEquals("Second modification", file3.getComment());
        assertEquals(9, file3.getSize());

        FileData project1 = repo.check("rules/project1");
        assertNotNull(project1);
        assertEquals("rules/project1", project1.getName());
        assertEquals("User 2", project1.getAuthor().getDisplayName());
        assertEquals("user2@email.to", project1.getAuthor().getEmail());
        assertEquals("Second modification", project1.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, project1.getSize());
    }

    @Test
    public void read() throws IOException {
        assertEquals("Hi.", readText(repo.read("rules/project1/file1")));
        assertEquals("Hello World.", readText(repo.read("rules/project1/file2")));
        assertEquals("In folder", readText(repo.read("rules/project1/folder/file3")));

        assertEquals(0, changesCounter.getChanges());
    }

    @Test
    public void save() throws IOException {
        // Create a new file
        String path = "rules/project1/folder/file4";
        String text = "File located in " + path;
        FileData result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("John Smith", result.getAuthor().getDisplayName());
        assertEquals("jsmith@email", result.getAuthor().getEmail());
        assertEquals("Comment for rules/project1/folder/file4", result.getComment());
        assertEquals(text.length(), result.getSize());
        assertEquals("Rules_5", result.getVersion());
        assertNotNull(result.getModifiedAt());

        assertEquals(text, readText(repo.read("rules/project1/folder/file4")));

        // Modify existing file
        text = "Modified";
        result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));
        assertNotNull(result);
        assertEquals(text.length(), result.getSize());
        assertEquals("Rules_6", result.getVersion());
        assertEquals(text, readText(repo.read("rules/project1/folder/file4")));

        assertEquals(2, changesCounter.getChanges());

        // Clone remote repository to temp folder and check that changes we made before exist there
        File temp = new File(root, "temp");
        try (GitRepository secondRepo = createRepository(remote, temp)) {
            assertEquals(text, readText(secondRepo.read("rules/project1/folder/file4")));
        }

        // Check that creating new folders works correctly
        path = "rules/project1/new-folder/file5";
        text = "File located in " + path;
        assertNotNull(repo.save(createFileData(path, text), IOUtils.toInputStream(text)));
    }

    @Test
    public void saveFolder() throws IOException {
        List<FileItem> changes = Arrays.asList(
                new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                new FileItem("rules/project1/file2", IOUtils.toInputStream("Modified")));

        FileData folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes, ChangesetType.FULL);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());

        // Save second time without changes. Mustn't fail.
        changes.get(0).getStream().reset();
        changes.get(1).getStream().reset();
        assertNotNull(repo.save(folderData, changes, ChangesetType.FULL));

        for (FileItem file : changes) {
            IOUtils.closeQuietly(file.getStream());
        }
    }

    @Test
    public void delete() throws IOException {
        FileData fileData = new FileData();
        fileData.setName("rules/project1/file2");
        fileData.setComment("Delete file 2");
        fileData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        boolean deleted = repo.delete(fileData);
        assertTrue(deleted, "'file2' has not been deleted");

        assertNull(repo.check("rules/project1/file2"), "'file2' still exists");

        // Count actual changes in history
        String projectPath = "rules/project1";
        assertEquals(3, repo.listHistory(projectPath).size());

        // Archive the project
        FileData projectData = new FileData();
        projectData.setName(projectPath);
        projectData.setComment("Delete project1");
        projectData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        assertTrue(repo.delete(projectData), "'project1' has not been deleted");

        FileData deletedProject = repo.check(projectPath);
        assertTrue(deletedProject.isDeleted(), "'project1' is not deleted");

        // Restore the project
        FileData toDelete = new FileData();
        toDelete.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        toDelete.setName(projectPath);
        toDelete.setVersion(deletedProject.getVersion());
        toDelete.setComment("Delete project1.");
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertFalse(deletedProject.isDeleted(), "'project1' is not restored");
        assertEquals("Delete project1.", deletedProject.getComment());

        // Count actual changes in history
        assertEquals(5, repo.listHistory(projectPath).size(), "Actual project changes must be 5.");
        assertEquals(5, repo.listHistory(projectPath, null, false, Page.unpaged()).size(), "Actual project changes must be 5.");
        Page page = Page.ofSize(2);
        assertEquals(2, repo.listHistory(projectPath, null, false, page).size(), "Actual project changes must be 2.");
        assertEquals(2, repo.listHistory(projectPath, null, false, page.withPage(1)).size(), "Actual project changes must be 2.");
        assertEquals(1, repo.listHistory(projectPath, null, false, page.withPage(2)).size(), "Actual project changes must be 1.");
        assertEquals(0, repo.listHistory(projectPath, null, false, page.withPage(3)).size(), "Actual project changes must be 0.");

        // Erase the project
        toDelete.setName(projectPath);
        toDelete.setVersion(null);
        toDelete.setComment("Erase project1");
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertNull(deletedProject, "'project1' is not erased");

        // Life after erase
        List<FileData> versionsAfterErase = repo.listHistory(projectPath);
        assertEquals(6, versionsAfterErase.size());
        FileData erasedData = versionsAfterErase.get(versionsAfterErase.size() - 1);
        assertTrue(erasedData.isDeleted());
        assertEquals(0, repo.listFiles(projectPath, erasedData.getVersion()).size());

        // Create new version
        String text = "Reincarnation";
        repo.save(createFileData(projectPath + "/folder/reincarnate", text), IOUtils.toInputStream(text));
        assertEquals(7, repo.listHistory(projectPath).size());

        // manually add the file with name ".archived". It shouldn't prevent to delete the project
        repo.save(createFileData(projectPath + "/" + GitRepository.DELETED_MARKER_FILE, ""), IOUtils.toInputStream(""));
        assertTrue(repo.delete(projectData), "'project1' has not been deleted");
        assertTrue(repo.check(projectPath).isDeleted(), "'project1' is not deleted");
    }

    @Test
    @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
    public void deleteAndSwitchBranches() throws IOException, GitAPIException {
        repo.createBranch(FOLDER_IN_REPOSITORY, "test1");
        GitRepository repo2 = repo.forBranch("test1");

        final String name = FOLDER_IN_REPOSITORY;

        // Archive the project in main branch
        FileData fileData = new FileData();
        fileData.setName(name);
        fileData.setComment("Delete project1");
        fileData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        boolean deleted = repo.delete(fileData);
        assertTrue(deleted, "'file2' has not been deleted");

        // Check that the project is archived in main branch
        assertEquals(BRANCH, repo.getBranch());
        final FileData archived = repo.check(name);
        assertTrue(archived.isDeleted());

        // Check that the project is archived in secondary branch too
        assertTrue(repo2.check(name).isDeleted(),
                "In repository with flat folder structure deleted status should be gotten from main branch");

        // Undelete the project
        assertTrue(repo.deleteHistory(archived));
        FileData undeleted = repo.check(name);

        // Check that the project is undeleted in main branch
        assertFalse(undeleted.isDeleted());

        // Check that the project is undeleted in secondary branch too
        assertFalse(repo2.check(name).isDeleted(),
                "In repository with flat folder structure deleted status should be gotten from main branch");

        // Check that old archived version is still deleted.
        assertTrue(repo.checkHistory(name, archived.getVersion()).isDeleted());

        // Check that isDeleted() is not broken for files: their status shouldn't be get from main branch.
        String filePath = "rules/project1/folder/file-new";
        String text = "text";
        FileData created = repo2.save(createFileData(filePath, text), IOUtils.toInputStream(text));
        assertFalse(created.isDeleted());
        assertFalse(repo2.check(filePath).isDeleted());
        assertFalse(repo2.checkHistory(filePath, created.getVersion()).isDeleted());

        // Delete the project outside of OpenL
        deleteProjectOutsideOfOpenL(repo2);
        // Recreate a project
        assertNotNull(repo2.save(createFileData(filePath, text), IOUtils.toInputStream(text)));
        // Check that the commit with project erasing can be read. There should be no deadlock.
        List<FileData> history = repo2.listHistory(name);
        assertTrue(history.size() > 2, "Not enough history records");
        FileData erasedData = history.get(history.size() - 2);
        assertTrue(erasedData.isDeleted());
    }

    private void deleteProjectOutsideOfOpenL(GitRepository repo) throws IOException, GitAPIException {
        try (Git git = repo.getClosableGit()) {
            git.checkout().setName(repo.getBranch()).setForced(true).call();
            git.rm().addFilepattern(FOLDER_IN_REPOSITORY).call();
            git.commit().setMessage("External erase").setCommitter("User 1", "user1@email.to").call();
        }
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

        assertEquals(1, repo.listHistory("rules/project1/folder").size());
    }

    @Test
    public void checkHistory() throws IOException {
        assertEquals("Rules_2", repo.checkHistory("rules/project1/file2", "Rules_2").getVersion());
        assertEquals("Rules_3", repo.checkHistory("rules/project1/file2", "Rules_3").getVersion());
        assertNull(repo.checkHistory("rules/project1/file2", "Rules_1"));

        FileData v3 = repo.checkHistory("rules/project1", "Rules_3");
        assertEquals("Rules_3", v3.getVersion());
        assertEquals("User 2", v3.getAuthor().getDisplayName());
        assertEquals("user2@email.to", v3.getAuthor().getEmail());

        FileData v2 = repo.checkHistory("rules/project1", "Rules_2");
        assertEquals("Rules_2", v2.getVersion());
        assertEquals("User 1", v2.getAuthor().getDisplayName());
        assertEquals("user1@email.to", v2.getAuthor().getEmail());

        assertNull(repo.checkHistory("rules/project1", "Rules_1"));
    }

    @Test
    public void readHistory() throws IOException {
        assertEquals("Hello.",
                readText(repo.readHistory("rules/project1/file2", "Rules_2")));
        assertEquals("Hello World.",
                readText(repo.readHistory("rules/project1/file2", "Rules_3")));
        assertNull(repo.readHistory("rules/project1/file2", "Rules_1"));
    }

    @Test
    public void copyHistory() throws IOException {
        FileData dest = new FileData();
        dest.setName("rules/project1/file2-copy");
        dest.setComment("Copy file 2");
        dest.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        FileData copy = repo.copyHistory("rules/project1/file2", dest, "Rules_2");
        assertNotNull(copy);
        assertEquals("rules/project1/file2-copy", copy.getName());
        assertEquals("John Smith", copy.getAuthor().getDisplayName());
        assertEquals("jsmith@email", copy.getAuthor().getEmail());
        assertEquals("Copy file 2", copy.getComment());
        assertEquals(6, copy.getSize());
        assertEquals("Rules_5", copy.getVersion());
        assertEquals("Hello.", readText(repo.read("rules/project1/file2-copy")));

        FileData destProject = new FileData();
        destProject.setName("rules/project2");
        destProject.setComment("Copy of project1");
        destProject.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        FileData project2 = repo.copyHistory("rules/project1", destProject, "Rules_2");
        assertNotNull(project2);
        assertEquals("rules/project2", project2.getName());
        assertEquals("John Smith", project2.getAuthor().getDisplayName());
        assertEquals("jsmith@email", project2.getAuthor().getEmail());
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
            data.setAuthor(new UserInfo(null));
            data.setComment(null);
            repo.save(data, IOUtils.toInputStream("error"));
            fail("Exception should be thrown");
        } catch (IOException e) {
            assertEquals("Name of PersonIdent must not be null.", e.getCause().getMessage());
        }

        // Check that there are no uncommitted changes after error
        try (Git git = Git.open(local)) {
            Status status = git.status().call();
            assertTrue(status.getUncommittedChanges().isEmpty());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void repoFolderExistsButEmpty() throws IOException {
        // Prepare the test: the folder with local repository name exists but it's empty
        repo.close();

        FileUtils.deleteQuietly(local);
        assertFalse(local.exists(), "Cannot delete repository. It shouldn't be locked.");

        if (!local.mkdirs() && !local.exists()) {
            fail("Cannot create the folder for test");
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
    public void neededBranchWasNotClonedBefore() throws IOException {
        // Prepare the test: clone master branch
        File local = new File(root, "temp");
        try (GitRepository repository = createRepository(remote, local, Constants.MASTER)) {
            assertEquals(2, repository.list("").size());
        }

        // Check: second time initialize the repo. At this time use the branch "test". It must be pulled
        // successfully and repository must be switched to that branch.
        try (GitRepository repository = createRepository(remote, local)) {
            assertEquals(5, repository.list("").size());

            // Check that changes are saved to correct branch.
            String text = "New file";
            FileItem change1 = new FileItem("rules/project-second/new/file1", IOUtils.toInputStream(text));
            FileItem change2 = new FileItem("rules/project-second/new/file2", IOUtils.toInputStream(text));
            FileData newProjectData = createFileData("rules/project-second/new", text);
            repository.save(newProjectData, Arrays.asList(change1, change2), ChangesetType.FULL);
            assertEquals(7, repository.list("").size());
        }
    }

    @Test
    public void twoUsersAddFileSimultaneously() throws IOException {
        // Prepare the test: clone master branch
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

            // First user does not suspect that second user already committed his changes
            FileData saved1 = repository1.save(createFileData("rules/project-first/file1", text),
                    IOUtils.toInputStream(text));

            // Check that the changes of both users are persist and merged
            assertNotEquals(saved1.getVersion(), saved2.getVersion(), "Versions of two changes must be different.");
            assertEquals(7,
                    repository1.list("").size(),
                    "5 files existed and 2 files must be added (must be 7 files in total).");
            assertEquals("Rules_6", saved1.getVersion());
            assertEquals("Rules_5", saved2.getVersion());
            assertEquals(repository1.check(saved1.getName()).getVersion(), "Rules_6");
            assertEquals("Rules_6", repository1.listHistory(saved1.getName()).get(0).getVersion());

            // Just ensure that last commit in the whole repository is merge commit
            assertEquals("Merge branch 'test' into test", repository1.check("rules").getComment());
        }
    }

    @Test
    public void mergeConflictInFile() throws IOException {
        // Prepare the test: clone master branch
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final String filePath = "rules/project1/file2";

        try (GitRepository repository1 = createRepository(remote, local1);
             GitRepository repository2 = createRepository(remote, local2)) {
            try {
                baseCommit = repository1.check(filePath).getVersion();
                // First user commit
                String text1 = "foo\nbar";
                FileData save1 = repository1.save(createFileData(filePath, text1), IOUtils.toInputStream(text1));
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                String text2 = "foo\nbaz";
                repository2.save(createFileData(filePath, text2), IOUtils.toInputStream(text2));

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                Collection<String> conflictedFiles = e.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(filePath, conflictedFiles.iterator().next());

                assertEquals(baseCommit, e.getBaseCommit());
                assertEquals(theirCommit, e.getTheirCommit());
                assertNotNull(e.getYourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(filePath).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(e.getYourCommit(),
                        repository2.check(filePath).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                String text2 = "foo\nbaz";
                String resolveText = "foo\nbar\nbaz";
                String mergeMessage = "Merge with " + theirCommit;

                List<FileItem> resolveConflicts = Collections
                        .singletonList(new FileItem(filePath, IOUtils.toInputStream(resolveText)));

                FileData fileData = createFileData(filePath, text2);
                fileData.setVersion(baseCommit);
                fileData.addAdditionalData(new ConflictResolveData(e.getTheirCommit(), resolveConflicts, mergeMessage));
                FileData localData = repository2.save(fileData, IOUtils.toInputStream(text2));

                FileItem remoteItem = repository2.read(filePath);
                assertEquals(resolveText, readText(remoteItem));
                FileData remoteData = remoteItem.getData();
                assertEquals(localData.getVersion(), remoteData.getVersion());
                assertEquals("John Smith", remoteData.getAuthor().getDisplayName());
                assertEquals("jsmith@email", remoteData.getAuthor().getEmail());
                assertEquals(mergeMessage, remoteData.getComment());

                // User modifies a file based on old version (baseCommit) and gets conflict.
                // Expected: after conflict their conflicting changes in local repository are not reverted.
                try {
                    String text3 = "test\nbaz";
                    FileData fileData3 = createFileData(filePath, text3);
                    fileData3.setVersion(baseCommit); // It's is needed for this scenario
                    repository2.save(fileData3, IOUtils.toInputStream(text3));
                    fail("MergeConflictException is expected");
                } catch (MergeConflictException ex) {
                    // Check that their changes are still present in repository.
                    assertEquals(localData.getVersion(),
                            repository2.check(filePath).getVersion(),
                            "Their changes were reverted in local repository");
                }
            }
        }
    }

    static String readText(FileItem remoteItem) throws IOException {
        try (var input = remoteItem.getStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void mergeConflictInFileMultipleProjects() throws IOException {
        // Prepare the test: clone master branch
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final String filePath = "rules/project1/file2";

        try (GitRepository repository1 = createRepository(remote, local1);
             GitRepository repository2 = createRepository(remote, local2)) {
            baseCommit = repository1.check(filePath).getVersion();
            // First user commit
            String text1 = "foo\nbar";
            FileData save1 = repository1.save(createFileData(filePath, text1), IOUtils.toInputStream(text1));
            theirCommit = save1.getVersion();

            // Second user commit (our). Will merge with first user's change (their).
            String text2 = "foo\nbaz";
            FileData fileData = createFileData(filePath, text2);
            InputStream stream = IOUtils.toInputStream(text2);
            repository2.save(Collections.singletonList(new FileItem(fileData, stream)));

            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            Collection<String> conflictedFiles = e.getConflictedFiles();

            assertEquals(1, conflictedFiles.size());
            assertEquals(filePath, conflictedFiles.iterator().next());

            assertEquals(baseCommit, e.getBaseCommit());
            assertEquals(theirCommit, e.getTheirCommit());
            assertNotNull(e.getYourCommit());

            try (GitRepository repository2 = createRepository(remote, local2)) {
                assertNotEquals(e.getYourCommit(),
                        repository2.check(filePath).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");
            }
        }
    }

    @Test
    public void mergeConflictInFolder() throws IOException {
        // Prepare the test: clone master branch
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final String folderPath = "rules/project1";

        final String conflictedFile = "rules/project1/file2";
        try (GitRepository repository1 = createRepository(remote, local1);
             GitRepository repository2 = createRepository(remote, local2)) {
            try {
                baseCommit = repository1.check(folderPath).getVersion();
                // First user commit
                String text1 = "foo\nbar";
                List<FileItem> changes1 = Arrays.asList(
                        new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                        new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

                FileData folderData1 = new FileData();
                folderData1.setName("rules/project1");
                folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
                folderData1.setComment("Bulk change by John");

                FileData save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                String text2 = "foo\nbaz";
                List<FileItem> changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

                FileData folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                Collection<String> conflictedFiles = e.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(conflictedFile, conflictedFiles.iterator().next());

                assertEquals(baseCommit, e.getBaseCommit());
                assertEquals(theirCommit, e.getTheirCommit());
                assertNotNull(e.getYourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(conflictedFile).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(e.getYourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                String text2 = "foo\nbaz";
                String resolveText = "foo\nbar\nbaz";
                String mergeMessage = "Merge with " + theirCommit;

                List<FileItem> changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

                List<FileItem> resolveConflicts = Collections
                        .singletonList(new FileItem(conflictedFile, IOUtils.toInputStream(resolveText)));

                FileData folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                folderData2.setVersion(baseCommit);
                folderData2
                        .addAdditionalData(new ConflictResolveData(e.getTheirCommit(), resolveConflicts, mergeMessage));
                FileData localData = repository2.save(folderData2, changes2, ChangesetType.DIFF);

                FileItem remoteItem = repository2.read(conflictedFile);
                assertEquals(resolveText, readText(remoteItem));
                FileData remoteData = remoteItem.getData();
                assertEquals(localData.getVersion(), remoteData.getVersion());
                assertEquals("Jane Smith", remoteData.getAuthor().getDisplayName());
                assertEquals("jasmith@email", remoteData.getAuthor().getEmail());
                assertEquals(mergeMessage, remoteData.getComment());

                String file1Content = readText(repository2.read("rules/project1/file1"));
                assertEquals("Modified", file1Content, "Other user's non-conflicting modification is absent.");

                // User modifies a file based on old version (baseCommit) and gets conflict.
                // Expected: after conflict their conflicting changes in local repository are not reverted.
                try {
                    String text3 = "test\nbaz";
                    List<FileItem> changes3 = Arrays.asList(
                            new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                            new FileItem(conflictedFile, IOUtils.toInputStream(text3)));

                    FileData folderData3 = new FileData();
                    folderData3.setName("rules/project1");
                    folderData3.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                    folderData3.setComment("Bulk change by Jane");
                    folderData3.setVersion(baseCommit); // It's is needed for this scenario
                    repository2.save(folderData3, changes3, ChangesetType.DIFF);
                    fail("MergeConflictException is expected");
                } catch (MergeConflictException ex) {
                    // Check that their changes are still present in repository.
                    assertEquals(localData.getVersion(),
                            repository2.check(conflictedFile).getVersion(),
                            "Their changes were reverted in local repository");
                }
            }
        }
    }

    @Test
    public void mergeConflictInFolderWithFileDeleting() throws IOException {
        // Prepare the test: clone master branch
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final String folderPath = "rules/project1";

        final String conflictedFile = "rules/project1/file2";
        try (GitRepository repository1 = createRepository(remote, local1);
             GitRepository repository2 = createRepository(remote, local2)) {
            try {
                baseCommit = repository1.check(folderPath).getVersion();
                // First user commit
                String text1 = "foo\nbar";
                List<FileItem> changes1 = Arrays.asList(
                        new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                        new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

                FileData folderData1 = new FileData();
                folderData1.setName("rules/project1");
                folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
                folderData1.setComment("Bulk change by John");

                FileData save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                List<FileItem> changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, null));

                FileData folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                Collection<String> conflictedFiles = e.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(conflictedFile, conflictedFiles.iterator().next());

                assertEquals(baseCommit, e.getBaseCommit());
                assertEquals(theirCommit, e.getTheirCommit());
                assertNotNull(e.getYourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(conflictedFile).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(e.getYourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                String mergeMessage = "Merge with " + theirCommit;

                List<FileItem> changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, null));

                List<FileItem> resolveConflicts = Collections.singletonList(new FileItem(conflictedFile, null));

                FileData folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                folderData2.setVersion(baseCommit);
                folderData2
                        .addAdditionalData(new ConflictResolveData(e.getTheirCommit(), resolveConflicts, mergeMessage));
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                FileItem remoteItem = repository2.read(conflictedFile);
                assertNull(remoteItem);
            }
        }
    }

    @Test
    public void mergeConflictInFolderMultipleProjects() throws IOException {
        // Prepare the test: clone master branch
        File local1 = new File(root, "temp1");
        File local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final String folderPath = "rules/project1";

        final String conflictedFile = "rules/project1/file2";
        try (GitRepository repository1 = createRepository(remote, local1);
             GitRepository repository2 = createRepository(remote, local2)) {
            baseCommit = repository1.check(folderPath).getVersion();
            // First user commit
            String text1 = "foo\nbar";
            List<FileItem> changes1 = Arrays.asList(
                    new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                    new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                    new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

            FileData folderData1 = new FileData();
            folderData1.setName("rules/project1");
            folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
            folderData1.setComment("Bulk change by John");

            FileData save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
            theirCommit = save1.getVersion();

            // Second user commit (our). Will merge with first user's change (their).
            String text2 = "foo\nbaz";
            List<FileItem> changes2 = Arrays.asList(
                    new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                    new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

            FileData folderData2 = new FileData();
            folderData2.setName("rules/project1");
            folderData2.setAuthor(new UserInfo("jasmith", "jasmith@eamil", "Jane Smith"));
            folderData2.setComment("Bulk change by Jane");
            repository2.save(folderData2, changes2, ChangesetType.DIFF);

            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            Collection<String> conflictedFiles = e.getConflictedFiles();

            assertEquals(1, conflictedFiles.size());
            assertEquals(conflictedFile, conflictedFiles.iterator().next());

            assertEquals(baseCommit, e.getBaseCommit());
            assertEquals(theirCommit, e.getTheirCommit());
            assertNotNull(e.getYourCommit());

            try (GitRepository repository2 = createRepository(remote, local2)) {
                assertNotEquals(e.getYourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");
            }
        }
    }

    @Test
    public void testBranches() throws IOException {
        repo.createBranch(FOLDER_IN_REPOSITORY, "project1/test1");
        repo.createBranch(FOLDER_IN_REPOSITORY, "project1/test2");
        assertListEquals(Arrays.asList("test", "project1/test1", "project1/test2"),
                repo.getBranches(FOLDER_IN_REPOSITORY));

        // Don't close "project1/test1" and "project1/test2" repositories explicitly.
        // Secondary repositories should be closed by parent repository automatically.
        BranchRepository repoTest1 = repo.forBranch("project1/test1");
        BranchRepository repoTest2 = repo.forBranch("project1/test2");

        assertEquals(BRANCH, repo.getBranch());
        assertEquals("project1/test1", repoTest1.getBranch());
        assertEquals("project1/test2", repoTest2.getBranch());

        repoTest1.deleteBranch(FOLDER_IN_REPOSITORY, "project1/test1");
        assertListEquals(Arrays.asList("test", "project1/test2"), repo.getBranches(FOLDER_IN_REPOSITORY));

        // Test that forBranch() fetches new branch if it has not been cloned before
        File temp = new File(root, "temp");
        try (GitRepository repository = createRepository(remote, temp, Constants.MASTER)) {
            GitRepository branchRepo = repository.forBranch("project1/test2");
            assertNotNull(branchRepo.check("rules/project1/file1"));
        }
    }

    @Test
    public void pathToRepoInsteadOfUri() {
        // Will use this path instead of uri. Git accepts that.
        String remote = new File(root, "remote").getAbsolutePath();

        try (GitRepository repository = createRepository(remote, local, BRANCH)) {
            assertNotNull(repository);
        }
        try (GitRepository repository = createRepository(remote + "/", local, BRANCH)) {
            assertNotNull(repository);
        }
        try (GitRepository repository = createRepository(new File(remote).toURI().toString(), local, BRANCH)) {
            assertNotNull(repository);
        }
    }

    @Test
    public void testIsValidBranchName() {
        assertTrue(repo.isValidBranchName("123"));
        assertFalse(repo.isValidBranchName("[~COM1/NUL]"));
    }

    @Test
    public void testFetchChanges() throws IOException, GitAPIException {
        ObjectId before = repo.getLastRevision();
        String newBranch = "new-branch";

        // Make a copy before any modifications
        File local2 = new File(root, "local2");
        FileUtils.copy(local, local2);

        // Modify on remote
        try (Git git = Git.open(remote)) {
            git.checkout().setName(BRANCH).call();
            git.branchCreate().setName(newBranch).call();

            Repository repository = git.getRepository();

            File rulesFolder = new File(repository.getDirectory().getParentFile(), FOLDER_IN_REPOSITORY);
            File file2 = new File(rulesFolder, "file2");
            writeText(file2, "Modify on remote server");
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit()
                    .setAll(true)
                    .setMessage("Second modification")
                    .setCommitter("User 2", "user2@email.to")
                    .call();
            // Fetch must not fail if some tag is added.
            addTag(git, commit, 42);
        }

        // Force fetching
        ObjectId after = repo.getLastRevision();
        assertNotEquals(before, after, "Last revision should be changed because of a new commit on a server");
        assertTrue(repo.getAvailableBranches().contains(newBranch), "Branch " + newBranch + " must be created");

        // Check that changes are fetched and fast forwarded after getLastRevision()
        List<FileData> file2History = repo.listHistory("rules/project1/file2");
        assertEquals(3, file2History.size());

        // Check that after repo initialization all changes are fetched and fast forwarded
        try (GitRepository repo2 = createRepository(remote, local2)) {
            file2History = repo2.listHistory("rules/project1/file2");
            assertEquals(3, file2History.size());
            assertTrue(repo2.getAvailableBranches().contains(newBranch), "Branch " + newBranch + " must be created");
        }

        // Check that all branches are available when repository is cloned.
        try (GitRepository repo3 = createRepository(remote, new File(root, "local3"))) {
            assertTrue(repo3.getAvailableBranches().contains(newBranch), "Branch " + newBranch + " must be created");
        }

        // Delete a branch on remote repository
        try (Git git = Git.open(remote)) {
            git.checkout().setName(Constants.MASTER).call();
            git.branchDelete().setBranchNames(BRANCH).setForce(true).call();
        }

        // Force fetching
        repo.getLastRevision();
        assertFalse(repo.getAvailableBranches().contains(BRANCH), "Branch " + BRANCH + " must be deleted");

        // Check that after repo initialization the branch is deleted on local repository.
        try (GitRepository repo2 = createRepository(remote, local2, "master")) {
            assertFalse(repo2.getAvailableBranches().contains(BRANCH), "Branch " + BRANCH + " must be deleted");
        }
    }

    @Test
    public void testPullDoesntAutoMerge() throws IOException {
        final String newBranch = "new-branch";
        repo.createBranch(FOLDER_IN_REPOSITORY, newBranch);
        GitRepository newBranchRepo = repo.forBranch(newBranch);

        // Add a new commit in the new branch.
        final String newPath = "rules/project1/folder/file-in-new-branch";
        String newText = "File located in " + newPath;
        newBranchRepo.save(createFileData(newPath, newText), IOUtils.toInputStream(newText));

        // Add a new commit in 'test' branch after 'new-branch' was created. Forces invocation of 'git checkout test' to
        // switch branch.
        String mainText = "Modify";
        repo.save(createFileData("rules/project1/folder/file4", mainText), IOUtils.toInputStream(mainText));

        // After current branch was switched to 'test', invoke pull on 'new-branch'.
        newBranchRepo.pull(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        assertNotNull(newBranchRepo.check(newPath), "The file '" + newPath + "' must exist in '" + newBranch + "'");
        // Check that pull is invoked on correct branch and that 'new-branch' is not merged into 'test'.
        assertNull(
                repo.check(newPath),
                "The file '" + newPath + "' must be absent in '" + BRANCH + "', because the branch '" + newBranch + "' wasn't merged yet.");
    }

    @Test
    public void testOnlySpecifiedBranchesAreMerged() throws IOException {
        final String branch1 = "branch1";
        repo.createBranch(FOLDER_IN_REPOSITORY, branch1);
        GitRepository branch1Repo = repo.forBranch(branch1);

        final String branch2 = "branch2";
        repo.createBranch(FOLDER_IN_REPOSITORY, branch2);
        GitRepository branch2Repo = repo.forBranch(branch2);

        // Add commits in the new branches.
        final String path1 = "rules/project1/folder/new-file1";
        String text1 = "Text1";
        branch1Repo.save(createFileData(path1, text1), IOUtils.toInputStream(text1));

        final String path2 = "rules/project1/folder/new-file2";
        String text2 = "Text2";
        branch2Repo.save(createFileData(path2, text2), IOUtils.toInputStream(text2));

        // Add a new commit in 'test' branch after new branches were created. Forces invocation of 'git checkout test'
        // to switch branch.
        String mainText = "Modify";
        repo.save(createFileData("rules/project1/folder/file4", mainText), IOUtils.toInputStream(mainText));

        // After current branch was switched to 'test', merge 'branch1' to 'branch2'.
        branch2Repo.merge(branch1, new UserInfo("jsmith", "jsmith@email", "John Smith"), null);

        // Check that 'branch1' and 'branch2' aren't merged into 'test'
        assertNull(
                repo.check(path1),
                "The file '" + path1 + "' must be absent in '" + BRANCH + "', because the branch '" + branch1 + "' wasn't merged yet.");
        assertNull(
                repo.check(path2),
                "The file '" + path2 + "' must be absent in '" + BRANCH + "', because the branch '" + branch2 + "' wasn't merged yet.");

        // Check that ''branch2' is not merged into 'branch1'
        assertNotNull(branch1Repo.check(path1), "The file '" + path1 + "' must exist in '" + branch1 + "'");
        assertNull(branch1Repo.check(path2), "The file '" + path2 + "' must be absent in '" + branch1 + "'");

        // Check that 'branch1 is merged into 'branch2'
        assertNotNull(branch2Repo.check(path1), "The file '" + path1 + "' must exist in '" + branch2 + "'");
        assertNotNull(branch2Repo.check(path2), "The file '" + path2 + "' must exist in '" + branch2 + "'");
    }

    @Test
    public void testResetUncommittedChanges() throws IOException {
        File parent;
        try (Git git = repo.getClosableGit()) {
            parent = git.getRepository().getDirectory().getParentFile();
        }
        File existingFile = new File(parent, "file-in-master");
        assertTrue(existingFile.exists());

        // Delete the file but don't commit it. Changes in not committed (modified externally for example or after
        // unsuccessful operation)
        // files must be aborted after repo.save() method.
        FileUtils.delete(existingFile);
        assertFalse(existingFile.exists());

        // Save other file.
        String text = "Some text";
        repo.save(createFileData("folder/any-file", text), IOUtils.toInputStream(text));

        // Not committed changes should be aborted
        assertTrue(existingFile.exists());
    }

    @Test
    public void testURIIdentity() throws URISyntaxException {
        URI a = new URI("https://github.com/openl-tablets/openl-tablets.git");
        URI b = new URI("https://github.com/openl-tablets/openl-tablets.git");
        assertEquals(a, b);
        assertTrue(GitRepository.isSame(a, b));

        b = new URI("http://github.com/openl-tablets/openl-tablets.git/");
        assertNotEquals(a, b);
        assertTrue(GitRepository.isSame(a, b));

        b = new URI("http://github.com/openl-tablets/openl-tablets.git?a=foo&b=bar");
        assertNotEquals(a, b);
        assertFalse(GitRepository.isSame(a, b));
    }

    private GitRepository createRepository(File remote, File local) {
        return createRepository(remote, local, BRANCH);
    }

    private GitRepository createRepository(File remote, File local, String branch) {
        return createRepository(remote.toURI().toString(), local, branch);
    }

    private GitRepository createRepository(String remoteUri, File local, String branch) {
        GitRepository repo = new GitRepository();
        repo.setUri(remoteUri);
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setBranch(branch);
        repo.setTagPrefix(TAG_PREFIX);
        repo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        String settingsPath = local.getParent() + "/git-settings";
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(settingsPath);
        String locksRoot = new File(root, "locks").getAbsolutePath();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }

    private FileData getFileData(List<FileData> files, String fileName) {
        for (FileData fileData : files) {
            if (fileName.equals(fileData.getName())) {
                return fileData;
            }
        }
        return null;
    }

    private static void addTag(Git git, RevCommit commit, int version) throws GitAPIException {
        git.tag().setObjectId(commit).setName(TAG_PREFIX + version).call();
    }

    private FileData find(List<FileData> files, String fileName) {
        for (FileData file : files) {
            if (fileName.equals(file.getName())) {
                return file;
            }
        }

        throw new IllegalArgumentException(String.format("File '%s' is not found.", fileName));
    }

    private static class ChangesCounter implements Listener {
        private int changes = 0;

        @Override
        public void onChange() {
            changes++;
        }

        int getChanges() {
            return changes;
        }
    }

    private static void assertListEquals(List<String> expected, List<String> actual) {
        List<String> rest = new ArrayList<>(actual);
        rest.removeAll(expected);
        if (!rest.isEmpty()) {
            fail(String.format("Unexpected items: %s", String.join(", ", rest)));
        }

        rest = new ArrayList<>(expected);
        rest.removeAll(actual);
        if (!rest.isEmpty()) {
            fail(String.format("Missed expected items: %s", String.join(", ", rest)));
        }
    }

    @Test
    public void testBranchDataSerialization() throws IOException {
        var mapper = YamlMapperFactory.getYamlMapper();
        BranchesData branches = null;
        try (var stream = getClass().getResourceAsStream("/BranchesDataOldStyle.yaml")) {
            branches = mapper.readValue(stream, BranchesData.class);
            assertTheSame(branches);
        }
        assertNotNull(branches);
        assertTheSame(mapper.readValue(mapper.writeValueAsBytes(branches), BranchesData.class));
    }

    private static void assertTheSame(BranchesData branches) {
        assertEquals(1, branches.getDescriptions().size());
        assertEquals("branch2", branches.getDescriptions().get(0).getName());
        assertEquals("1deabde8a096756681a08c4552597ef8850963f7", branches.getDescriptions().get(0).getCommit());
        assertEquals(1, branches.getProjectBranches().size());
        assertEquals(2, branches.getProjectBranches().get("rules/project_2").size());
        assertEquals("master", branches.getProjectBranches().get("rules/project_2").get(0));
        assertEquals("branch2", branches.getProjectBranches().get("rules/project_2").get(1));
    }
}
