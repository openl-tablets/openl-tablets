package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createLocalRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;
import org.openl.util.IOUtils;

/**
 * A commit that changes only technical files of a project, such as a version bump in its pom.xml made by a CI
 * pipeline, is a technical revision of the project. It holds for a project in a folder of the repository and for one
 * at its root alike, and for an ignored file and a protected one alike: {@code **}{@code /pom.xml} ignored, or
 * {@code pom.xml} protected.
 *
 * <p>A project stands on the head of the branch, whatever the head changes. Its history hides the technical revisions
 * unless asked for them, but always lists the head. A save is merged with a later change of the project only: a
 * protected file counts, as the save writes it, an ignored one does not.
 */
class IgnoredPathsHistoryTest {
    @TempDir
    private File root;
    @TempDir
    private File localRepositoriesFolder;
    @AutoClose
    private GitRepository repo;

    private String project;
    private String changed;
    private String bumped;

    private void init(String project, boolean protectedFile) throws IOException {
        this.project = project;
        repo = createLocalRepository(new File(root, "design-repository"), localRepositoriesFolder,
                protectedFile ? null : "**/pom.xml");
        if (protectedFile) {
            repo.setProtectedFiles("pom.xml");
        }

        save("rules.xml", "<project/>");
        save("pom.xml", "1.0.0");
        save("pom.xml", "1.0.1");
        changed = save("rules.xml", "<project><name>Rating</name></project>");
        bumped = save("pom.xml", "1.0.2");
    }

    private String save(String file, String text) throws IOException {
        return repo.save(createFileData(project + "/" + file, text), IOUtils.toInputStream(text)).getVersion();
    }

    /** Saves a file of another project, which makes the head of the branch a commit that leaves this one alone. */
    private String saveOtherProject() throws IOException {
        return repo.save(createFileData("Other/rules.xml", "<project/>"), IOUtils.toInputStream("<project/>"))
                .getVersion();
    }

    private List<String> history(boolean techRevs) throws IOException {
        return history(project, techRevs);
    }

    private List<String> history(String path, boolean techRevs) throws IOException {
        return TestGitUtils.labelled(repo.listHistory(path, null, techRevs, Pageable.unpaged()));
    }

    private String comment(String file) {
        return "Comment for " + project + "/" + file;
    }

    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void aCommitThatChangesOnlyTechnicalFilesIsTechnical(String project, boolean protectedFile) throws IOException {
        init(project, protectedFile);

        assertEquals(List.of(comment("rules.xml"), comment("rules.xml"), comment("pom.xml") + " (technical)"),
                history(false));
        assertEquals(List.of(comment("rules.xml"),
                        comment("pom.xml") + " (technical)",
                        comment("pom.xml") + " (technical)",
                        comment("rules.xml"),
                        comment("pom.xml") + " (technical)"),
                history(true));
    }

    /** The project stands on the head of the branch, which its history lists, so it always marks the current one. */
    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void theProjectStandsOnTheHeadOfTheBranch(String project, boolean protectedFile) throws IOException {
        init(project, protectedFile);

        // The head of the branch: the last version bump, protected or ignored.
        var head = versions(repo.listHistory(project, null, true, Pageable.unpaged())).getLast();
        assertEquals(head, repo.check(project).getVersion());
        // It tells who changed it last, a technical revision aside
        assertEquals(comment("rules.xml"), repo.check(project).getComment());
        assertEquals(head, repo.listHistory(project).getLast().getVersion());
    }

    /**
     * A save based on an older head is no save of an old version when only other projects changed since: nothing is
     * merged.
     */
    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void aSaveOverAChangeOfAnotherProjectIsNoSaveOfAnOldVersion(String project, boolean protectedFile)
            throws Exception {
        init(project, protectedFile);
        var opened = repo.check(project).getVersion();

        var head = saveOtherProject();

        assertEquals(head, repo.check(project).getVersion());
        assertFalse(repo.isCheckoutOldVersion(project, opened));
        // The folder of the project is the same tree at both heads, so a working copy opened at the older one is current
        assertEquals(repo.check(project).getUniqueId(), repo.checkHistory(project, opened).getUniqueId());
    }

    /**
     * The head of the branch is listed in the history of every project, as a technical revision of a project it does
     * not change, and older technical revisions are not. The history of a file lists only the revisions that change
     * the file.
     */
    @ParameterizedTest
    @ValueSource(strings = {"rules/Rating", "Rating"})
    void theHistoryOfAProjectListsTheHeadOfTheBranch(String project) throws IOException {
        init(project, true);
        var head = saveOtherProject();

        assertEquals(head, repo.check(project).getVersion());
        assertEquals(comment("rules.xml"), repo.check(project).getComment());
        assertEquals(List.of(comment("rules.xml"), comment("rules.xml"), "Comment for Other/rules.xml (technical)"),
                history(false));
        assertEquals(head, versions(repo.listHistory(project, null, false, Pageable.unpaged())).getLast());
        assertEquals(List.of(comment("rules.xml"), comment("rules.xml")), history(project + "/rules.xml", false));
    }

    /** A deleted project stands on no head: its history ends with the commit that deleted it. */
    @ParameterizedTest
    @ValueSource(strings = {"rules/Rating", "Rating"})
    void theHistoryOfADeletedProjectEndsWithItsDeletion(String project) throws IOException {
        init(project, true);
        repo.delete(createFileData(project, "", "Delete " + project));
        saveOtherProject();

        var history = repo.listHistory(project, null, false, Pageable.unpaged());

        assertEquals("Delete " + project, history.getLast().getComment());
        assertTrue(history.getLast().isDeleted());
    }

    private static List<String> versions(List<FileData> history) {
        return history.stream().map(FileData::getVersion).toList();
    }

    /**
     * A project read at the revision it stands on has the protected files of its latest commit, such as the pom.xml
     * of the last version bump, not those of its last revision that is not technical.
     */
    @ParameterizedTest
    @ValueSource(strings = {"rules/Rating", "Rating"})
    void theProjectIsReadWithItsLatestProtectedFiles(String project) throws IOException {
        init(project, true);

        var version = repo.check(project).getVersion();

        assertEquals("1.0.2", GitRepositoryTest.readText(repo.readHistory(project + "/pom.xml", version)));
        // A version bump changes the tree of the project, so a working copy opened before it is outdated
        assertNotEquals(repo.check(project).getUniqueId(), repo.checkHistory(project, changed).getUniqueId());
        var pom = repo.listFiles(project + "/", version)
                .stream()
                .filter(data -> data.getName().equals(project + "/pom.xml"))
                .findFirst()
                .orElseThrow();
        assertEquals(repo.check(project + "/pom.xml").getUniqueId(), pom.getUniqueId());
    }

    /** The latest commit is listed on the page it falls on, even when it is technical. */
    @ParameterizedTest
    @ValueSource(strings = {"rules/Rating", "Rating"})
    void aPageOfTheHistoryListsTheLatestTechnicalCommit(String project) throws IOException {
        init(project, true);

        var page = repo.listHistory(project, null, false, Page.of(0, 1));

        assertEquals(List.of(bumped), versions(page));
        assertTrue(page.getFirst().isTechnicalRevision());
    }

    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void aCommitThatChangesATechnicalFileAndAnotherOneIsNotTechnical(String project, boolean protectedFile)
            throws IOException {
        init(project, protectedFile);

        var both = repo.save(createFileData(project, ""),
                List.of(new FileItem(project + "/pom.xml", IOUtils.toInputStream("1.1.0")),
                        new FileItem(project + "/rules.xml", IOUtils.toInputStream("<project/>"))),
                ChangesetType.DIFF);

        assertEquals(List.of(comment("rules.xml"), comment("rules.xml"), "Comment for " + project),
                history(false));
        assertEquals(both.getVersion(), repo.check(project).getVersion());
    }

    /**
     * A save based on an earlier head is merged with a later commit that changes only the protected files of the
     * project, as the save writes them too: the version bump of the CI pipeline is not undone by the copy of
     * pom.xml the user read before it. A later commit that changes only ignored files is no reason to merge.
     */
    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void aSaveOverALaterChangeOfAProtectedFileKeepsIt(String project, boolean protectedFile) throws Exception {
        init(project, protectedFile);

        assertEquals(protectedFile, repo.isCheckoutOldVersion(project, changed));

        var data = createFileData(project, "");
        data.setVersion(changed);
        var files = new ArrayList<>(List.of(
                new FileItem(project + "/rules.xml", IOUtils.toInputStream("<project><name>Saved</name></project>"))));
        if (protectedFile) {
            // The copy of pom.xml the user read before the version bump.
            files.add(new FileItem(project + "/pom.xml", IOUtils.toInputStream("1.0.1")));
        }
        repo.save(data, files, ChangesetType.FULL);

        assertEquals("1.0.2", GitRepositoryTest.readText(repo.read(project + "/pom.xml")));
        assertEquals("<project><name>Saved</name></project>",
                GitRepositoryTest.readText(repo.read(project + "/rules.xml")));
    }

    /** A protected file is told by its path from the root of the project: a file named alike deeper is not one. */
    @ParameterizedTest
    @ValueSource(strings = {"rules/Rating", "Rating"})
    void aCommitThatChangesAFileNamedAsAProtectedOneDeeperIsNotTechnical(String project) throws IOException {
        init(project, true);

        var nested = save("rules/pom.xml", "2.0.0");

        assertEquals(nested, repo.check(project).getVersion());
        assertEquals(comment("rules/pom.xml"), history(false).getLast());
    }
}
