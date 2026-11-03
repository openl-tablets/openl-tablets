package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.lib.Constants;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.IOUtils;

/**
 * A revision tells the files under a folder it changed, the way Git shows a commit: against the commit it was made
 * on, or against an earlier revision asked for. Two revisions tell the revision their histories parted at.
 */
class GitRevisionChangesTest {

    private static final String REPO_ID = "design";
    private static final String PROJECT = "rules/project1";

    @TempDir
    private File root;
    @TempDir
    private Path localRepositoriesFolder;
    @AutoClose
    private GitRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        var local = new File(root, "design-repository");
        repo = new GitRepository();
        repo.setId(REPO_ID);
        var uri = local.getAbsolutePath();
        repo.setUri(uri);
        var repositoriesFolder = localRepositoriesFolder.toFile().getAbsolutePath();
        repo.setLocalRepositoriesFolder(repositoriesFolder);
        repo.setGcAutoDetach(false);
        repo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, uri, local, repositoriesFolder, false, true));
    }

    @Test
    void theFirstRevisionAddsEveryFileOfTheFolder() throws IOException {
        write(repo, PROJECT + "/rules.xml", "<project/>");
        var first = head();

        var changes = repo.listChanges(PROJECT, null, first);

        assertNull(changes.from());
        // The revision is told as the history lists it.
        assertEquals(first, changes.to().getVersion());
        assertEquals("Comment for " + PROJECT + "/rules.xml", changes.to().getComment());
        assertEquals("John Smith", changes.to().getAuthor().getName());
        assertEquals(List.of(new FileChange(PROJECT + "/rules.xml", FileChange.Kind.ADDED)), changes.changes());
    }

    @Test
    void aRevisionIsComparedWithTheOneItWasMadeOnWithinTheFolder() throws IOException {
        write(repo, PROJECT + "/Rules.xlsx", "one");
        write(repo, PROJECT + "/old.txt", "old");
        var before = head();
        // The whole folder is saved at once, so the file it no longer holds is deleted.
        var folder = createFileData(PROJECT, "");
        repo.save(folder,
                List.of(new FileItem(PROJECT + "/Rules.xlsx", IOUtils.toInputStream("two")),
                        new FileItem(PROJECT + "/folder/new.txt", IOUtils.toInputStream("new"))),
                ChangesetType.FULL);
        var after = head();

        var changes = repo.listChanges(PROJECT, null, after);

        assertEquals(before, changes.from().getVersion());
        assertEquals(after, changes.to().getVersion());
        assertEquals(List.of(new FileChange(PROJECT + "/Rules.xlsx", FileChange.Kind.MODIFIED),
                new FileChange(PROJECT + "/folder/new.txt", FileChange.Kind.ADDED),
                new FileChange(PROJECT + "/old.txt", FileChange.Kind.DELETED)), changes.changes());
    }

    @Test
    void twoRevisionsTellTheNetDifferenceBetweenThem() throws IOException {
        write(repo, PROJECT + "/a.txt", "1");
        var first = head();
        write(repo, PROJECT + "/a.txt", "2");
        write(repo, PROJECT + "/b.txt", "added");
        write(repo, "rules/project11/other.txt", "another folder whose name starts like the project's");
        write(repo, PROJECT + "/a.txt", "1");
        var last = head();

        var changes = repo.listChanges(PROJECT, first, last);

        assertEquals(first, changes.from().getVersion());
        // a.txt was changed and changed back, and project11 is another folder: the added file is the difference.
        assertEquals(List.of(new FileChange(PROJECT + "/b.txt", FileChange.Kind.ADDED)), changes.changes());
    }

    @Test
    void aMergeTellsWhatItBroughtIntoTheBranch() throws IOException {
        write(repo, PROJECT + "/a.txt", "base");
        repo.createRepositoryBranch("feature", repo.getBranch());
        write(repo, PROJECT + "/main.txt", "made on the branch merged into");
        var beforeMerge = head();
        write(repo.forBranch("feature"), PROJECT + "/feature.txt", "made on the feature branch");
        repo.merge("feature", new UserInfo("admin", "admin@email", "Admin"), null);
        var merge = head();

        var changes = repo.listChanges(PROJECT, null, merge);

        assertEquals(beforeMerge, changes.from().getVersion());
        assertEquals(List.of(new FileChange(PROJECT + "/feature.txt", FileChange.Kind.ADDED)), changes.changes());
    }

    @Test
    void anUnknownRevisionCannotBeTold() throws IOException {
        write(repo, PROJECT + "/a.txt", "1");

        assertThrows(FileNotFoundException.class,
                () -> repo.listChanges(PROJECT, null, "0123456789abcdef0123456789abcdef01234567"));
        assertThrows(FileNotFoundException.class, () -> repo.listChanges(PROJECT, "no-such-revision", head()));
    }

    @Test
    void twoBranchesPartedAtTheRevisionBothWereMadeOn() throws IOException {
        write(repo, PROJECT + "/a.txt", "base");
        var parted = head();
        repo.createRepositoryBranch("feature", repo.getBranch());
        write(repo, PROJECT + "/main.txt", "made on the main branch");
        var main = head();
        write(repo.forBranch("feature"), PROJECT + "/feature.txt", "made on the feature branch");
        var feature = head("feature");

        var common = repo.findCommonRevision(PROJECT, main, feature);

        // The revision is told as the history lists it.
        assertEquals(parted, common.getVersion());
        assertEquals("Comment for " + PROJECT + "/a.txt", common.getComment());
        assertEquals(parted, repo.findCommonRevision(PROJECT, feature, main).getVersion());
    }

    @Test
    void aRevisionMadeOnAnotherOnePartedAtThatOne() throws IOException {
        write(repo, PROJECT + "/a.txt", "1");
        var first = head();
        write(repo, PROJECT + "/a.txt", "2");
        var second = head();

        assertEquals(first, repo.findCommonRevision(PROJECT, first, second).getVersion());
        assertEquals(first, repo.findCommonRevision(PROJECT, second, first).getVersion());
        assertEquals(second, repo.findCommonRevision(PROJECT, second, second).getVersion());
        // A branch named twice is the last revision of the branch, told by its id.
        assertEquals(second, repo.findCommonRevision(PROJECT, repo.getBranch(), repo.getBranch()).getVersion());
    }

    @Test
    void anUnknownRevisionHasNoCommonRevision() throws IOException {
        write(repo, PROJECT + "/a.txt", "1");

        assertThrows(FileNotFoundException.class, () -> repo.findCommonRevision(PROJECT, head(), "no-such-revision"));
    }

    private String head() throws IOException {
        return head(repo.getBranch());
    }

    private String head(String branch) throws IOException {
        try (var git = repo.getClosableGit()) {
            return git.getRepository().resolve(Constants.R_HEADS + branch).getName();
        }
    }

    private static void write(Repository repository, String path, String text) throws IOException {
        repository.save(createFileData(path, text), IOUtils.toInputStream(text));
    }
}
