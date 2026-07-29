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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.Getter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
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

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

class GitRepositoryTest {
    private static final String BRANCH = "test";
    private static final String FOLDER_IN_REPOSITORY = "rules/project1";
    private static final String TAG_PREFIX = "Rules_";
    private static final String REPO_ID = "design";

    @TempDir
    private static File template;
    @TempDir(cleanup = CleanupMode.NEVER)
    private File root;
    private File remote;
    private File local;
    private String repositoriesFolder;
    @AutoClose
    private GitRepository repo;
    private ChangesCounter changesCounter;

    @BeforeAll
    static void initTest() throws GitAPIException, IOException {
        // Initialize remote repository
        try (var git = Git.init().setDirectory(template).call()) {
            var repository = git.getRepository();
            var config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            var parent = repository.getDirectory().getParentFile();
            var rulesFolder = new File(parent, FOLDER_IN_REPOSITORY);

            // create initial commit in master
            createNewFile(parent, "file-in-master", "root");
            git.add().addFilepattern(".").call();
            var commit = git.commit().setMessage("Initial").setCommitter("User 1", "user1@email.to").call();
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
    void setUp() throws IOException {

        remote = new File(root, "remote");
        var repositoriesFolderFile = new File(root, "repositories");
        repositoriesFolder = repositoriesFolderFile.toString();
        local = new File(repositoriesFolder, "local");

        FileUtils.copy(template, remote);
        repo = createRepository(remote, local, true);

        changesCounter = new ChangesCounter();
        repo.setListener(changesCounter);
    }

    @Test
    void list() throws IOException {
        assertEquals(5, repo.list("").size());

        var files = repo.list("rules/project1/");
        assertNotNull(files);
        assertEquals(3, files.size());

        var file1 = getFileData(files, "rules/project1/file1");
        assertNotNull(file1);
        assertEquals("User 1", file1.getAuthor().getName());
        assertEquals("user1@email.to", file1.getAuthor().getEmail());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        var file2 = getFileData(files, "rules/project1/file2");
        assertNotNull(file2);
        assertEquals("User 2", file2.getAuthor().getName());
        assertEquals("user2@email.to", file2.getAuthor().getEmail());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        var file3 = getFileData(files, "rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("User 2", file3.getAuthor().getName());
        assertEquals("user2@email.to", file3.getAuthor().getEmail());
        assertEquals("Second modification", file3.getComment());
        assertEquals(9, file3.getSize());
    }

    @Test
    void listFolders() throws IOException {
        assertEquals(1, repo.listFolders("").size());

        var folders = repo.listFolders("rules/");
        assertNotNull(folders);
        assertEquals(1, folders.size());

        var folderData = folders.getFirst();
        assertEquals("rules/project1", folderData.getName());
    }

    @Test
    void listFiles() throws IOException {
        var files = repo.listFiles("rules/project1/", "Rules_2");
        assertNotNull(files);
        assertEquals(2, files.size());
        assertContains(files, "rules/project1/file1");
        assertContains(files, "rules/project1/file2");

        var file1Rev2 = find(files, "rules/project1/file1");
        assertEquals("Rules_2", file1Rev2.getVersion());

        var file2Rev2 = find(files, "rules/project1/file2");
        assertEquals("Rules_2", file2Rev2.getVersion());
        assertEquals("User 1", file2Rev2.getAuthor().getName());
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

        var file2Rev3 = find(files, "rules/project1/file2");
        assertEquals("Rules_3", file2Rev3.getVersion());
        assertEquals("User 2", file2Rev3.getAuthor().getName());
        assertEquals("user2@email.to", file2Rev3.getAuthor().getEmail());
        assertEquals("Second modification", file2Rev3.getComment());
        assertEquals(12, file2Rev3.getSize(), "Expected file content: 'Hello World!'");

        var file3Rev3 = find(files, "rules/project1/folder/file3");
        assertEquals("Rules_3", file3Rev3.getVersion());
    }

    @Test
    void check() throws IOException {
        var file1 = repo.check("rules/project1/file1");
        assertNotNull(file1);
        assertEquals("User 1", file1.getAuthor().getName());
        assertEquals("user1@email.to", file1.getAuthor().getEmail());
        assertEquals("Initial commit in test branch", file1.getComment());
        assertEquals(3, file1.getSize());

        var file2 = repo.check("rules/project1/file2");
        assertNotNull(file2);
        assertEquals("User 2", file2.getAuthor().getName());
        assertEquals("user2@email.to", file2.getAuthor().getEmail());
        assertEquals("Second modification", file2.getComment());
        assertEquals(12, file2.getSize());

        var file3 = repo.check("rules/project1/folder/file3");
        assertNotNull(file3);
        assertEquals("User 2", file3.getAuthor().getName());
        assertEquals("user2@email.to", file3.getAuthor().getEmail());
        assertEquals("Second modification", file3.getComment());
        assertEquals(9, file3.getSize());

        var project1 = repo.check("rules/project1");
        assertNotNull(project1);
        assertEquals("rules/project1", project1.getName());
        assertEquals("User 2", project1.getAuthor().getName());
        assertEquals("user2@email.to", project1.getAuthor().getEmail());
        assertEquals("Second modification", project1.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, project1.getSize());
    }

    @Test
    void read() throws IOException {
        assertEquals("Hi.", readText(repo.read("rules/project1/file1")));
        assertEquals("Hello World.", readText(repo.read("rules/project1/file2")));
        assertEquals("In folder", readText(repo.read("rules/project1/folder/file3")));

        assertEquals(0, changesCounter.getChanges());
    }

    @Test
    void save() throws IOException {
        // Create a new file
        var path = "rules/project1/folder/file4";
        var text = "File located in " + path;
        var result = repo.save(createFileData(path, text), IOUtils.toInputStream(text));

        assertNotNull(result);
        assertEquals(path, result.getName());
        assertEquals("John Smith", result.getAuthor().getName());
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
        var temp = new File(root, "temp");
        try (var secondRepo = createRepository(remote, temp, true)) {
            assertEquals(text, readText(secondRepo.read("rules/project1/folder/file4")));
        }

        // Check that creating new folders works correctly
        path = "rules/project1/new-folder/file5";
        text = "File located in " + path;
        assertNotNull(repo.save(createFileData(path, text), IOUtils.toInputStream(text)));
    }

    @Test
    void saveWithBlankDisplayName() throws IOException {
        // Regression for EPBDS-16228: a blank user display name must not break the commit.
        // The git committer name falls back to the username (UserInfo.getName()); otherwise an
        // empty committer ident aborts the commit and, in Studio, leaves a project half-opened.
        var path = "rules/project1/folder/file-blank-dn";
        var text = "File with blank display name author";
        var data = new FileData();
        data.setName(path);
        data.setComment("Comment for " + path);
        data.setAuthor(new UserInfo("jdoe", "jdoe@email", ""));
        var result = repo.save(data, IOUtils.toInputStream(text));

        assertNotNull(result);
        // The committer name persisted to git is the username, not the blank display name.
        assertEquals("jdoe", result.getAuthor().getName());
        assertEquals(text, readText(repo.read(path)));
    }

    @Test
    void commitMetadataIsNotRestoredFromStructuredMessage() throws IOException, GitAPIException {
        var path = "rules/project1/legacy-message";
        var message = "OpenL Studio: DELETE. Visible message";
        try (var git = repo.getClosableGit()) {
            createNewFile(new File(git.getRepository().getWorkTree(), "rules/project1"), "legacy-message", "Content");
            git.add().addFilepattern(path).call();
            git.commit().setMessage(message).setCommitter("Git Committer", "committer@example.org").call();
        }

        var fileData = repo.check(path);

        assertEquals(message, fileData.getComment());
        assertFalse(fileData.isDeleted());
        assertEquals("Git Committer", fileData.getAuthor().getName());
        assertEquals("committer@example.org", fileData.getAuthor().getEmail());
    }

    @Test
    void saveFolder() throws IOException {
        var changes = Arrays.asList(
                new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                new FileItem("rules/project1/file2", IOUtils.toInputStream("Modified")));

        var folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        folderData.setComment("Bulk change");

        var savedData = repo.save(folderData, changes, ChangesetType.FULL);
        assertNotNull(savedData);
        var files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file4");
        assertContains(files, "rules/project1/file2");
        assertEquals(2, files.size());

        // Save second time without changes. Mustn't fail.
        changes.getFirst().getStream().reset();
        changes.get(1).getStream().reset();
        assertNotNull(repo.save(folderData, changes, ChangesetType.FULL));

        for (FileItem file : changes) {
            IOUtils.closeQuietly(file.getStream());
        }
    }

    @Test
    void delete() throws IOException {
        var fileData = new FileData();
        fileData.setName("rules/project1/file2");
        fileData.setComment("Delete file 2");
        fileData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        var deleted = repo.delete(fileData);
        assertTrue(deleted, "'file2' has not been deleted");

        assertNull(repo.check("rules/project1/file2"), "'file2' still exists");

        // Count actual changes in history
        var projectPath = "rules/project1";
        assertEquals(3, repo.listHistory(projectPath).size());

        // Delete the project
        var projectData = new FileData();
        projectData.setName(projectPath);
        projectData.setComment("Delete project1");
        projectData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        assertTrue(repo.delete(projectData), "'project1' has not been deleted");

        var deletedProject = repo.check(projectPath);
        assertNull(deletedProject, "'project1' still exists");

        // Count actual changes in history
        var versionsAfterDelete = repo.listHistory(projectPath);
        assertEquals(4, versionsAfterDelete.size(), "Actual project changes must be 4.");
        var deletedData = versionsAfterDelete.getLast();
        assertTrue(deletedData.isDeleted());
        assertEquals(0, repo.listFiles(projectPath, deletedData.getVersion()).size());
        assertEquals(4, repo.listHistory(projectPath, null, false, Page.unpaged()).size(), "Actual project changes must be 4.");
        Page page = Page.ofSize(2);
        assertEquals(2, repo.listHistory(projectPath, null, false, page).size(), "Actual project changes must be 2.");
        assertEquals(2, repo.listHistory(projectPath, null, false, page.withPage(1)).size(), "Actual project changes must be 2.");
        assertEquals(0, repo.listHistory(projectPath, null, false, page.withPage(2)).size(), "Actual project changes must be 0.");
        assertEquals(0, repo.listHistory(projectPath, null, false, page.withPage(3)).size(), "Actual project changes must be 0.");

        // Create new version after deletion
        var text = "Reincarnation";
        repo.save(createFileData(projectPath + "/folder/reincarnate", text), IOUtils.toInputStream(text));
        assertEquals(5, repo.listHistory(projectPath).size());

        // Manually add the file with name ".archived". It is regular project content now.
        repo.save(createFileData(projectPath + "/.archived", ""), IOUtils.toInputStream(""));
        assertFalse(repo.check(projectPath).isDeleted());
        assertTrue(repo.delete(projectData), "'project1' has not been deleted");
        assertNull(repo.check(projectPath), "'project1' still exists");
    }

    @Test
    @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
    void deleteAndSwitchBranches() throws IOException, GitAPIException {
        repo.createRepositoryBranch("test1", repo.getBranch());
        var repo2 = repo.forBranch("test1");

        final var name = FOLDER_IN_REPOSITORY;

        // Delete the project in main branch
        var fileData = new FileData();
        fileData.setName(name);
        fileData.setComment("Delete project1");
        fileData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        var deleted = repo.delete(fileData);
        assertTrue(deleted, "'file2' has not been deleted");

        // Check that the project is deleted in main branch
        assertEquals(BRANCH, repo.getBranch());
        assertNull(repo.check(name));
        var deletedOnBaseBranch = repo.listHistory(name).getLast();
        assertTrue(deletedOnBaseBranch.isDeleted());

        // Presence in the selected branch tree is authoritative.
        assertFalse(repo2.check(name).isDeleted());

        // Check that the deleted version is available in history and has no files.
        assertEquals(0, repo.listFiles(name, deletedOnBaseBranch.getVersion()).size());

        // Check that isDeleted() is not broken for files: their status shouldn't be get from main branch.
        var filePath = "rules/project1/folder/file-new";
        var text = "text";
        var created = repo2.save(createFileData(filePath, text), IOUtils.toInputStream(text));
        assertFalse(created.isDeleted());
        assertFalse(repo2.check(filePath).isDeleted());
        assertFalse(repo2.checkHistory(filePath, created.getVersion()).isDeleted());

        // Delete the project outside of OpenL
        deleteProjectOutsideOfOpenL(repo2);
        // Recreate a project
        assertNotNull(repo2.save(createFileData(filePath, text), IOUtils.toInputStream(text)));
        // Check that the commit with project deletion can be read. There should be no deadlock.
        var history = repo2.listHistory(name);
        assertTrue(history.size() > 2, "Not enough history records");
        var deletedData = history.get(history.size() - 2);
        assertTrue(deletedData.isDeleted());
    }

    private void deleteProjectOutsideOfOpenL(GitRepository repo) throws IOException, GitAPIException {
        try (var git = repo.getClosableGit()) {
            git.checkout().setName(repo.getBranch()).setForced(true).call();
            git.rm().addFilepattern(FOLDER_IN_REPOSITORY).call();
            git.commit().setMessage("External erase").setCommitter("User 1", "user1@email.to").call();
        }
    }

    @Test
    void listHistory() throws IOException {
        var file2History = repo.listHistory("rules/project1/file2");
        assertEquals(2, file2History.size());
        assertEquals("Rules_2", file2History.getFirst().getVersion());
        assertEquals("Rules_3", file2History.get(1).getVersion());

        var project1History = repo.listHistory("rules/project1");
        assertEquals(2, project1History.size());
        assertEquals("Rules_2", project1History.getFirst().getVersion());
        assertEquals("Rules_3", project1History.get(1).getVersion());

        assertEquals(1, repo.listHistory("rules/project1/folder").size());
    }

    @Test
    void checkHistory() throws IOException {
        assertEquals("Rules_2", repo.checkHistory("rules/project1/file2", "Rules_2").getVersion());
        assertEquals("Rules_3", repo.checkHistory("rules/project1/file2", "Rules_3").getVersion());
        assertNull(repo.checkHistory("rules/project1/file2", "Rules_1"));

        var v3 = repo.checkHistory("rules/project1", "Rules_3");
        assertEquals("Rules_3", v3.getVersion());
        assertEquals("User 2", v3.getAuthor().getName());
        assertEquals("user2@email.to", v3.getAuthor().getEmail());

        var v2 = repo.checkHistory("rules/project1", "Rules_2");
        assertEquals("Rules_2", v2.getVersion());
        assertEquals("User 1", v2.getAuthor().getName());
        assertEquals("user1@email.to", v2.getAuthor().getEmail());

        assertNull(repo.checkHistory("rules/project1", "Rules_1"));
    }

    @Test
    void readHistory() throws IOException {
        assertEquals("Hello.",
                readText(repo.readHistory("rules/project1/file2", "Rules_2")));
        assertEquals("Hello World.",
                readText(repo.readHistory("rules/project1/file2", "Rules_3")));
        assertNull(repo.readHistory("rules/project1/file2", "Rules_1"));
    }

    @Test
    void copyHistory() throws IOException {
        var dest = new FileData();
        dest.setName("rules/project1/file2-copy");
        dest.setComment("Copy file 2");
        dest.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));

        var copy = repo.copyHistory("rules/project1/file2", dest, "Rules_2");
        assertNotNull(copy);
        assertEquals("rules/project1/file2-copy", copy.getName());
        assertEquals("John Smith", copy.getAuthor().getName());
        assertEquals("jsmith@email", copy.getAuthor().getEmail());
        assertEquals("Copy file 2", copy.getComment());
        assertEquals(6, copy.getSize());
        assertEquals("Rules_5", copy.getVersion());
        assertEquals("Hello.", readText(repo.read("rules/project1/file2-copy")));

        var destProject = new FileData();
        destProject.setName("rules/project2");
        destProject.setComment("Copy of project1");
        destProject.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        var project2 = repo.copyHistory("rules/project1", destProject, "Rules_2");
        assertNotNull(project2);
        assertEquals("rules/project2", project2.getName());
        assertEquals("John Smith", project2.getAuthor().getName());
        assertEquals("jsmith@email", project2.getAuthor().getEmail());
        assertEquals("Copy of project1", project2.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, project2.getSize());
        assertEquals("Rules_6", project2.getVersion());
        var project2Files = repo.list("rules/project2/");
        assertEquals(2, project2Files.size());
        assertContains(project2Files, "rules/project2/file1");
        assertContains(project2Files, "rules/project2/file2");
    }

    @Test
    void changesShouldBeRolledBackOnError() throws Exception {
        try {
            var data = new FileData();
            data.setName("rules/project1/file2");
            data.setAuthor(new UserInfo(null));
            data.setComment(null);
            repo.save(data, IOUtils.toInputStream("error"));
            fail("Exception should be thrown");
        } catch (IOException e) {
            assertEquals("Commit author name is blank.", e.getCause().getMessage());
        }

        // Check that there are no uncommitted changes after error
        try (Git git = Git.open(local)) {
            var status = git.status().call();
            assertTrue(status.getUncommittedChanges().isEmpty());
        }
    }

    @Test
    void saveWithUsernameOnlyAuthorUsesUsernameAsCommitter() throws IOException {
        var data = new FileData();
        data.setName("rules/project1/username-only");
        data.setAuthor(new UserInfo("admin"));
        data.setComment("Username-only author");

        var saved = repo.save(data, IOUtils.toInputStream("content"));

        var history = repo.checkHistory(saved.getName(), saved.getVersion());
        assertEquals("admin", history.getAuthor().getName());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void repoFolderExistsButEmpty() throws IOException {
        // Prepare the test: the folder with local repository name exists but it's empty
        repo.close();

        FileUtils.deleteQuietly(local);
        assertFalse(local.exists(), "Cannot delete repository. It shouldn't be locked.");

        if (!local.mkdirs() && !local.exists()) {
            fail("Cannot create the folder for test");
        }

        // Check that repo is cloned successfully
        try (var repository = createRepository(remote, local, true)) {
            assertEquals(5, repository.list("").size());
        }
        // Reuse cloned before repository. Must not fail.
        try (var repository = createRepository(remote, local, true)) {
            assertEquals(5, repository.list("").size());
        }
    }

    @Test
    void neededBranchWasNotClonedBefore() throws IOException {
        // Prepare the test: clone master branch
        var local = new File(root, "temp");
        try (var repository = createRepository(remote, local, Constants.MASTER, true)) {
            assertEquals(2, repository.list("").size());
        }

        // Check: second time initialize the repo. At this time use the branch "test". It must be pulled
        // successfully and repository must be switched to that branch.
        try (var repository = createRepository(remote, local, false)) {
            assertEquals(5, repository.list("").size());

            // Check that changes are saved to correct branch.
            var text = "New file";
            var change1 = new FileItem("rules/project-second/new/file1", IOUtils.toInputStream(text));
            var change2 = new FileItem("rules/project-second/new/file2", IOUtils.toInputStream(text));
            FileData newProjectData = createFileData("rules/project-second/new", text);
            repository.save(newProjectData, Arrays.asList(change1, change2), ChangesetType.FULL);
            assertEquals(7, repository.list("").size());
        }
    }

    @Test
    void twoUsersAddFileSimultaneously() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        // First user starts to save it's changes
        try (var repository1 = createRepository(remote, local1, true)) {
            var text = "New file";

            // Second user is quicker than first
            FileData saved2;
            try (var repository2 = createRepository(remote, local2, true)) {
                saved2 = repository2.save(createFileData("rules/project-second/file2", text),
                        IOUtils.toInputStream(text));
            }

            // First user does not suspect that second user already committed his changes
            var saved1 = repository1.save(createFileData("rules/project-first/file1", text),
                    IOUtils.toInputStream(text));

            // Check that the changes of both users are persist and merged
            assertNotEquals(saved1.getVersion(), saved2.getVersion(), "Versions of two changes must be different.");
            assertEquals(7,
                    repository1.list("").size(),
                    "5 files existed and 2 files must be added (must be 7 files in total).");
            assertEquals("Rules_6", saved1.getVersion());
            assertEquals("Rules_5", saved2.getVersion());
            assertEquals("Rules_6", repository1.check(saved1.getName()).getVersion());
            assertEquals("Rules_6", repository1.listHistory(saved1.getName()).getFirst().getVersion());

            // Just ensure that last commit in the whole repository is merge commit
            assertEquals("Merge branch 'test' into test", repository1.check("rules").getComment());
        }
    }

    @Test
    void mergeConflictInFile() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final var filePath = "rules/project1/file2";

        try (var repository1 = createRepository(remote, local1, true);
             var repository2 = createRepository(remote, local2, true)) {
            try {
                baseCommit = repository1.check(filePath).getVersion();
                // First user commit
                var text1 = "foo\nbar";
                var save1 = repository1.save(createFileData(filePath, text1), IOUtils.toInputStream(text1));
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                var text2 = "foo\nbaz";
                repository2.save(createFileData(filePath, text2), IOUtils.toInputStream(text2));

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                var conflictDetails = e.getDetails();
                Collection<String> conflictedFiles = conflictDetails.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(filePath, conflictedFiles.iterator().next());

                assertEquals(baseCommit, conflictDetails.baseCommit());
                assertEquals(theirCommit, conflictDetails.theirCommit());
                assertNotNull(conflictDetails.yourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(filePath).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(conflictDetails.yourCommit(),
                        repository2.check(filePath).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                var text2 = "foo\nbaz";
                var resolveText = "foo\nbar\nbaz";
                var mergeMessage = "Merge with " + theirCommit;

                var resolveConflicts = List
                        .of(new FileItem(filePath, IOUtils.toInputStream(resolveText)));

                FileData fileData = createFileData(filePath, text2);
                fileData.setVersion(baseCommit);
                fileData.addAdditionalData(new ConflictResolveData(conflictDetails.theirCommit(), resolveConflicts, mergeMessage));
                var localData = repository2.save(fileData, IOUtils.toInputStream(text2));

                var remoteItem = repository2.read(filePath);
                assertEquals(resolveText, readText(remoteItem));
                var remoteData = remoteItem.getData();
                assertEquals(localData.getVersion(), remoteData.getVersion());
                assertEquals("John Smith", remoteData.getAuthor().getName());
                assertEquals("jsmith@email", remoteData.getAuthor().getEmail());
                assertEquals(mergeMessage, remoteData.getComment());

                // User modifies a file based on old version (baseCommit) and gets conflict.
                // Expected: after conflict their conflicting changes in local repository are not reverted.
                try {
                    var text3 = "test\nbaz";
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
    void mergeConflictInFileMultipleProjects() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final var filePath = "rules/project1/file2";

        try (var repository1 = createRepository(remote, local1, true);
             var repository2 = createRepository(remote, local2, true)) {
            baseCommit = repository1.check(filePath).getVersion();
            // First user commit
            var text1 = "foo\nbar";
            var save1 = repository1.save(createFileData(filePath, text1), IOUtils.toInputStream(text1));
            theirCommit = save1.getVersion();

            // Second user commit (our). Will merge with first user's change (their).
            var text2 = "foo\nbaz";
            FileData fileData = createFileData(filePath, text2);
            InputStream stream = IOUtils.toInputStream(text2);
            repository2.save(List.of(new FileItem(fileData, stream)));

            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            var conflictDetails = e.getDetails();
            Collection<String> conflictedFiles = conflictDetails.getConflictedFiles();

            assertEquals(1, conflictedFiles.size());
            assertEquals(filePath, conflictedFiles.iterator().next());

            assertEquals(baseCommit, conflictDetails.baseCommit());
            assertEquals(theirCommit, conflictDetails.theirCommit());
            assertNotNull(conflictDetails.yourCommit());

            try (var repository2 = createRepository(remote, local2, false)) {
                assertNotEquals(conflictDetails.yourCommit(),
                        repository2.check(filePath).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");
            }
        }
    }

    @Test
    void mergeConflictInFolder() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final var folderPath = "rules/project1";

        final var conflictedFile = "rules/project1/file2";
        try (var repository1 = createRepository(remote, local1, true);
             var repository2 = createRepository(remote, local2, true)) {
            try {
                baseCommit = repository1.check(folderPath).getVersion();
                // First user commit
                var text1 = "foo\nbar";
                var changes1 = Arrays.asList(
                        new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                        new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

                var folderData1 = new FileData();
                folderData1.setName("rules/project1");
                folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
                folderData1.setComment("Bulk change by John");

                var save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                var text2 = "foo\nbaz";
                var changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

                var folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                var conflictDetails = e.getDetails();
                Collection<String> conflictedFiles = conflictDetails.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(conflictedFile, conflictedFiles.iterator().next());

                assertEquals(baseCommit, conflictDetails.baseCommit());
                assertEquals(theirCommit, conflictDetails.theirCommit());
                assertNotNull(conflictDetails.yourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(conflictedFile).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(conflictDetails.yourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                var text2 = "foo\nbaz";
                var resolveText = "foo\nbar\nbaz";
                var mergeMessage = "Merge with " + theirCommit;

                var changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

                var resolveConflicts = List
                        .of(new FileItem(conflictedFile, IOUtils.toInputStream(resolveText)));

                var folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                folderData2.setVersion(baseCommit);
                folderData2
                        .addAdditionalData(new ConflictResolveData(conflictDetails.theirCommit(), resolveConflicts, mergeMessage));
                var localData = repository2.save(folderData2, changes2, ChangesetType.DIFF);

                var remoteItem = repository2.read(conflictedFile);
                assertEquals(resolveText, readText(remoteItem));
                var remoteData = remoteItem.getData();
                assertEquals(localData.getVersion(), remoteData.getVersion());
                assertEquals("Jane Smith", remoteData.getAuthor().getName());
                assertEquals("jasmith@email", remoteData.getAuthor().getEmail());
                assertEquals(mergeMessage, remoteData.getComment());

                String file1Content = readText(repository2.read("rules/project1/file1"));
                assertEquals("Modified", file1Content, "Other user's non-conflicting modification is absent.");

                // User modifies a file based on old version (baseCommit) and gets conflict.
                // Expected: after conflict their conflicting changes in local repository are not reverted.
                try {
                    var text3 = "test\nbaz";
                    var changes3 = Arrays.asList(
                            new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                            new FileItem(conflictedFile, IOUtils.toInputStream(text3)));

                    var folderData3 = new FileData();
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
    void mergeConflictInFolderWithFileDeleting() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final var folderPath = "rules/project1";

        final var conflictedFile = "rules/project1/file2";
        try (var repository1 = createRepository(remote, local1, true);
             var repository2 = createRepository(remote, local2, true)) {
            try {
                baseCommit = repository1.check(folderPath).getVersion();
                // First user commit
                var text1 = "foo\nbar";
                var changes1 = Arrays.asList(
                        new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                        new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

                var folderData1 = new FileData();
                folderData1.setName("rules/project1");
                folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
                folderData1.setComment("Bulk change by John");

                var save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
                theirCommit = save1.getVersion();

                // Second user commit (our). Will merge with first user's change (their).
                var changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, null));

                var folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                fail("MergeConflictException is expected");
            } catch (MergeConflictException e) {
                var conflictDetails = e.getDetails();
                Collection<String> conflictedFiles = conflictDetails.getConflictedFiles();

                assertEquals(1, conflictedFiles.size());
                assertEquals(conflictedFile, conflictedFiles.iterator().next());

                assertEquals(baseCommit, conflictDetails.baseCommit());
                assertEquals(theirCommit, conflictDetails.theirCommit());
                assertNotNull(conflictDetails.yourCommit());

                // Check that their changes are still present in repository.
                assertEquals(theirCommit,
                        repository2.check(conflictedFile).getVersion(),
                        "Their changes were reverted in local repository");

                assertNotEquals(conflictDetails.yourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");

                var mergeMessage = "Merge with " + theirCommit;

                var changes2 = Arrays.asList(
                        new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                        new FileItem(conflictedFile, null));

                var resolveConflicts = List.of(new FileItem(conflictedFile, null));

                var folderData2 = new FileData();
                folderData2.setName("rules/project1");
                folderData2.setAuthor(new UserInfo("jasmith", "jasmith@email", "Jane Smith"));
                folderData2.setComment("Bulk change by Jane");
                folderData2.setVersion(baseCommit);
                folderData2
                        .addAdditionalData(new ConflictResolveData(conflictDetails.theirCommit(), resolveConflicts, mergeMessage));
                repository2.save(folderData2, changes2, ChangesetType.DIFF);

                var remoteItem = repository2.read(conflictedFile);
                assertNull(remoteItem);
            }
        }
    }

    @Test
    void mergeConflictInFolderMultipleProjects() throws IOException {
        // Prepare the test: clone master branch
        var local1 = new File(root, "temp1");
        var local2 = new File(root, "temp2");

        String baseCommit = null;
        String theirCommit = null;

        final var folderPath = "rules/project1";

        final var conflictedFile = "rules/project1/file2";
        try (var repository1 = createRepository(remote, local1, true);
             var repository2 = createRepository(remote, local2, true)) {
            baseCommit = repository1.check(folderPath).getVersion();
            // First user commit
            var text1 = "foo\nbar";
            var changes1 = Arrays.asList(
                    new FileItem("rules/project1/file1", IOUtils.toInputStream("Modified")),
                    new FileItem("rules/project1/new-path/file4", IOUtils.toInputStream("Added")),
                    new FileItem(conflictedFile, IOUtils.toInputStream(text1)));

            var folderData1 = new FileData();
            folderData1.setName("rules/project1");
            folderData1.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
            folderData1.setComment("Bulk change by John");

            var save1 = repository1.save(folderData1, changes1, ChangesetType.DIFF);
            theirCommit = save1.getVersion();

            // Second user commit (our). Will merge with first user's change (their).
            var text2 = "foo\nbaz";
            var changes2 = Arrays.asList(
                    new FileItem("rules/project1/new-path/file5", IOUtils.toInputStream("Added")),
                    new FileItem(conflictedFile, IOUtils.toInputStream(text2)));

            var folderData2 = new FileData();
            folderData2.setName("rules/project1");
            folderData2.setAuthor(new UserInfo("jasmith", "jasmith@eamil", "Jane Smith"));
            folderData2.setComment("Bulk change by Jane");
            repository2.save(folderData2, changes2, ChangesetType.DIFF);

            fail("MergeConflictException is expected");
        } catch (MergeConflictException e) {
            var conflictDetails = e.getDetails();
            Collection<String> conflictedFiles = conflictDetails.getConflictedFiles();

            assertEquals(1, conflictedFiles.size());
            assertEquals(conflictedFile, conflictedFiles.iterator().next());

            assertEquals(baseCommit, conflictDetails.baseCommit());
            assertEquals(theirCommit, conflictDetails.theirCommit());
            assertNotNull(conflictDetails.yourCommit());

            try (var repository2 = createRepository(remote, local2, false)) {
                assertNotEquals(conflictDetails.yourCommit(),
                        repository2.check(conflictedFile).getVersion(),
                        "Our conflicted commit must be reverted but it exists.");
            }
        }
    }

    @Test
    void testBranches() throws IOException {
        repo.createRepositoryBranch("project1/test1", repo.getBranch());
        repo.createRepositoryBranch("project1/test2", repo.getBranch());
        assertListEquals(Arrays.asList(Constants.MASTER, BRANCH, "project1/test1", "project1/test2"),
                repo.listBranches());

        // Don't close "project1/test1" and "project1/test2" repositories explicitly.
        // Secondary repositories should be closed by parent repository automatically.
        var repoTest1 = repo.forBranch("project1/test1");
        var repoTest2 = repo.forBranch("project1/test2");

        assertEquals(BRANCH, repo.getBranch());
        assertEquals("project1/test1", repoTest1.getBranch());
        assertEquals("project1/test2", repoTest2.getBranch());

        repoTest1.deleteRepositoryBranch("project1/test1");
        assertListEquals(Arrays.asList(Constants.MASTER, BRANCH, "project1/test2"), repo.listBranches());

        // Test that forBranch() fetches new branch if it has not been cloned before
        var temp = new File(root, "temp");
        try (var repository = createRepository(remote, temp, Constants.MASTER, true)) {
            var branchRepo = repository.forBranch("project1/test2");
            assertNotNull(branchRepo.check("rules/project1/file1"));
        }
    }

    @Test
    void branchListsUseOnlyGitRefs() throws Exception {
        var deletedBranch = "deleted";
        var remoteOnlyBranch = "remote-only";
        repo.createRepositoryBranch(deletedBranch, BRANCH);

        try (var git = repo.getClosableGit()) {
            git.branchDelete().setBranchNames(deletedBranch).setForce(true).call();

            var repository = git.getRepository();
            var deletedRemoteRef = repository.updateRef(
                    Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + deletedBranch);
            deletedRemoteRef.setForceUpdate(true);
            deletedRemoteRef.delete();

            var remoteOnlyRef = repository.updateRef(
                    Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + remoteOnlyBranch);
            remoteOnlyRef.setNewObjectId(repository.resolve(Constants.R_HEADS + BRANCH));
            remoteOnlyRef.forceUpdate();
        }

        var branches = repo.listBranches();
        assertEquals(List.of(Constants.MASTER, remoteOnlyBranch, BRANCH), branches);
        assertEquals(branches, repo.listBranches());
        assertFalse(branches.contains(deletedBranch));
        assertTrue(repo.branchExists(remoteOnlyBranch));
        assertFalse(repo.branchExists(deletedBranch));
    }

    @Test
    void branchOnlyFolderIsPresentInItsBranch() throws IOException {
        var branch = "feature/branch-only-folder";
        var folder = "branch-only-project";
        repo.createRepositoryBranch(branch, repo.getBranch());

        try (var branchRepository = repo.forBranch(branch)) {
            var content = "branch-only";
            branchRepository.save(
                    createFileData(folder + "/rules.xlsx", content),
                    IOUtils.toInputStream(content));

            var folderData = branchRepository.listFolders("")
                    .stream()
                    .filter(data -> folder.equals(data.getName()))
                    .findFirst()
                    .orElseThrow();
            assertFalse(folderData.isDeleted());
        }
    }

    @Test
    void pathToRepoInsteadOfUri() throws IOException {
        // Will use this path instead of uri. Git accepts that.
        var remote = new File(root, "remote").getAbsolutePath();

        try (var repository = createRepository(remote, local, BRANCH, true)) {
            assertNotNull(repository);
        }
        try (var repository = createRepository(remote + "/", local, BRANCH, false)) {
            assertNotNull(repository);
        }
        try (var repository = createRepository(new File(remote).toURI().toString(), local, BRANCH, false)) {
            assertNotNull(repository);
        }
    }

    @Test
    void testIsValidBranchName() {
        assertTrue(repo.isValidBranchName("123"));
        assertFalse(repo.isValidBranchName("[~COM1/NUL]"));
    }

    @Test
    void testFetchChanges() throws IOException, GitAPIException {
        var before = repo.getLastRevision();
        var newBranch = "new-branch";

        // Make a copy before any modifications
        var local2 = new File(root, "local2");
        FileUtils.copy(local, local2);

        // Modify on remote
        try (Git git = Git.open(remote)) {
            git.checkout().setName(BRANCH).call();
            git.branchCreate().setName(newBranch).call();

            var repository = git.getRepository();

            var rulesFolder = new File(repository.getDirectory().getParentFile(), FOLDER_IN_REPOSITORY);
            var file2 = new File(rulesFolder, "file2");
            writeText(file2, "Modify on remote server");
            git.add().addFilepattern(".").call();
            var commit = git.commit()
                    .setAll(true)
                    .setMessage("Second modification")
                    .setCommitter("User 2", "user2@email.to")
                    .call();
            // Fetch must not fail if some tag is added.
            addTag(git, commit, 42);
        }

        // Force fetching
        var after = repo.getLastRevision();
        assertNotEquals(before, after, "Last revision should be changed because of a new commit on a server");
        assertTrue(repo.getAvailableBranches().contains(newBranch), "Branch " + newBranch + " must be created");

        // Check that changes are fetched and fast forwarded after getLastRevision()
        var file2History = repo.listHistory("rules/project1/file2");
        assertEquals(3, file2History.size());

        // Check that after repo initialization all changes are fetched and fast forwarded
        try (var repo2 = createRepository(remote, local2, false)) {
            file2History = repo2.listHistory("rules/project1/file2");
            assertEquals(3, file2History.size());
            assertTrue(repo2.getAvailableBranches().contains(newBranch), "Branch " + newBranch + " must be created");
        }

        // Check that all branches are available when repository is cloned.
        try (var repo3 = createRepository(remote, new File(root, "local3"), true)) {
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
        try (var repo2 = createRepository(remote, local2, "master", false)) {
            assertFalse(repo2.getAvailableBranches().contains(BRANCH), "Branch " + BRANCH + " must be deleted");
        }
    }

    @Test
    void testPullDoesntAutoMerge() throws IOException {
        final var newBranch = "new-branch";
        repo.createRepositoryBranch(newBranch, repo.getBranch());
        var newBranchRepo = repo.forBranch(newBranch);

        // Add a new commit in the new branch.
        final var newPath = "rules/project1/folder/file-in-new-branch";
        var newText = "File located in " + newPath;
        newBranchRepo.save(createFileData(newPath, newText), IOUtils.toInputStream(newText));

        // Add a new commit in 'test' branch after 'new-branch' was created. Forces invocation of 'git checkout test' to
        // switch branch.
        var mainText = "Modify";
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
    void testOnlySpecifiedBranchesAreMerged() throws IOException {
        final var branch1 = "branch1";
        repo.createRepositoryBranch(branch1, repo.getBranch());
        var branch1Repo = repo.forBranch(branch1);

        final var branch2 = "branch2";
        repo.createRepositoryBranch(branch2, repo.getBranch());
        var branch2Repo = repo.forBranch(branch2);

        // Add commits in the new branches.
        final var path1 = "rules/project1/folder/new-file1";
        var text1 = "Text1";
        branch1Repo.save(createFileData(path1, text1), IOUtils.toInputStream(text1));

        final var path2 = "rules/project1/folder/new-file2";
        var text2 = "Text2";
        branch2Repo.save(createFileData(path2, text2), IOUtils.toInputStream(text2));

        // Add a new commit in 'test' branch after new branches were created. Forces invocation of 'git checkout test'
        // to switch branch.
        var mainText = "Modify";
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
    void testResetUncommittedChanges() throws IOException {
        File parent;
        try (var git = repo.getClosableGit()) {
            parent = git.getRepository().getDirectory().getParentFile();
        }
        var existingFile = new File(parent, "file-in-master");
        assertTrue(existingFile.exists());

        // Delete the file but don't commit it. Changes in not committed (modified externally for example or after
        // unsuccessful operation)
        // files must be aborted after repo.save() method.
        Files.delete(existingFile.toPath());
        assertFalse(existingFile.exists());

        // Save other file.
        var text = "Some text";
        repo.save(createFileData("folder/any-file", text), IOUtils.toInputStream(text));

        // Not committed changes should be aborted
        assertTrue(existingFile.exists());
    }

    private GitRepository createRepository(File remote, File local, boolean empty) throws IOException {
        return createRepository(remote, local, BRANCH, empty);
    }

    private GitRepository createRepository(File remote, File local, String branch, boolean empty) throws IOException {
        return createRepository(remote.toURI().toString(), local, branch, empty);
    }

    private GitRepository createRepository(String remoteUri, File local, String branch, boolean empty) throws IOException {
        var newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        newRepo.setUri(remoteUri);
        newRepo.setLocalRepositoriesFolder(repositoriesFolder);
        newRepo.setBranch(branch);
        newRepo.setTagPrefix(TAG_PREFIX);
        newRepo.setGcAutoDetach(false);
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, remoteUri, local, repositoriesFolder, true, empty));

        return newRepo;
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

        throw new IllegalArgumentException("File '%s' is not found.".formatted(fileName));
    }

    private static class ChangesCounter implements Listener {
        @Getter(AccessLevel.PACKAGE)
        private int changes = 0;

        @Override
        public void onChange() {
            changes++;
        }
    }

    private static void assertListEquals(List<String> expected, List<String> actual) {
        List<String> rest = new ArrayList<>(actual);
        rest.removeAll(expected);
        if (!rest.isEmpty()) {
            fail("Unexpected items: %s".formatted(String.join(", ", rest)));
        }

        rest = new ArrayList<>(expected);
        rest.removeAll(actual);
        if (!rest.isEmpty()) {
            fail("Missed expected items: %s".formatted(String.join(", ", rest)));
        }
    }

}
