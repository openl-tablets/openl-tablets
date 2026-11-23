package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Pageable;
import org.openl.util.IOUtils;

/**
 * A commit that changes only technical files of a project, such as a version bump in its pom.xml made by a CI
 * pipeline, is a technical revision of the project, and the project stands on its last revision that is not. It
 * holds for a project in a folder of the repository and for one at its root alike, and for an ignored file and a
 * protected one alike: {@code **}{@code /pom.xml} ignored, or {@code pom.xml} protected.
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
        save("pom.xml", "1.0.2");
    }

    private String save(String file, String text) throws IOException {
        return repo.save(createFileData(project + "/" + file, text), IOUtils.toInputStream(text)).getVersion();
    }

    private List<String> history(boolean techRevs) throws IOException {
        return repo.listHistory(project, null, techRevs, Pageable.unpaged())
                .stream()
                .map(data -> data.getComment() + (data.isTechnicalRevision() ? " (technical)" : ""))
                .toList();
    }

    private String comment(String file) {
        return "Comment for " + project + "/" + file;
    }

    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void aCommitThatChangesOnlyTechnicalFilesIsTechnical(String project, boolean protectedFile) throws IOException {
        init(project, protectedFile);

        assertEquals(List.of(comment("rules.xml"), comment("rules.xml")), history(false));
        assertEquals(List.of(comment("rules.xml"),
                        comment("pom.xml") + " (technical)",
                        comment("pom.xml") + " (technical)",
                        comment("rules.xml"),
                        comment("pom.xml") + " (technical)"),
                history(true));
    }

    /** The revision the project stands on is listed, so the history always marks the current one. */
    @ParameterizedTest
    @CsvSource({"rules/Rating, false", "Rating, false", "rules/Rating, true", "Rating, true"})
    void theProjectStandsOnItsLastRevisionThatIsNotTechnical(String project, boolean protectedFile)
            throws IOException {
        init(project, protectedFile);

        assertEquals(changed, repo.check(project).getVersion());
        assertEquals(changed, repo.listHistory(project, null, false, Pageable.unpaged()).getLast().getVersion());
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
     * A save based on the revision the project stands on is merged with a later commit that changes only its
     * protected files, as the save writes them too: the version bump of the CI pipeline is not undone by the copy of
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
