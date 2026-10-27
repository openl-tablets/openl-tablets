package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createLocalRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.IOUtils;

/**
 * A conflict in an ignored file, such as a version bumped in pom.xml in both branches by a CI pipeline, keeps the
 * version of the branch merged into: no one resolves it in OpenL Studio.
 */
class IgnoredPathsMergeTest {
    private static final String POM = "rules/Rating/pom.xml";
    private static final String RULES = "rules/Rating/rules.xml";
    private static final UserInfo AUTHOR = new UserInfo("admin", "admin@email", "Admin");

    @TempDir
    private File root;
    @TempDir
    private File localRepositoriesFolder;
    @AutoClose
    private GitRepository repo;
    private BranchRepository feature;

    @BeforeEach
    void setUp() throws IOException {
        repo = createLocalRepository(new File(root, "design-repository"), localRepositoriesFolder, "**/pom.xml");
        save(repo, RULES, "<project/>");
        save(repo, POM, "1.0.0");
        repo.createRepositoryBranch("feature", repo.getBranch());
        feature = repo.forBranch("feature");
        save(repo, POM, "1.1.0");
        save(feature, POM, "1.0.1");
    }

    private static void save(BranchRepository repository, String file, String text) throws IOException {
        repository.save(createFileData(file, text), IOUtils.toInputStream(text));
    }

    private String read(String file) throws IOException {
        return GitRepositoryTest.readText(repo.read(file));
    }

    @Test
    void aConflictInAnIgnoredFileKeepsTheVersionOfTheBranchMergedInto() throws IOException {
        save(feature, RULES, "<project><name>Rating</name></project>");

        repo.merge("feature", AUTHOR, null);

        assertEquals("1.1.0", read(POM));
        assertEquals("<project><name>Rating</name></project>", read(RULES));
        var merge = repo.listHistory("rules/Rating", null, true, Pageable.unpaged()).getLast().getComment();
        assertTrue(merge.contains(POM + " (master)"), merge);
    }

    @Test
    void anotherConflictIsResolvedByHandAndTheIgnoredFileKeepsTheVersionOfTheBranchMergedInto() throws IOException {
        save(repo, RULES, "<project><name>Master</name></project>");
        save(feature, RULES, "<project><name>Feature</name></project>");

        var conflict = assertThrows(MergeConflictException.class, () -> repo.merge("feature", AUTHOR, null));

        assertEquals(List.of(RULES), List.copyOf(conflict.getDetails().diffs().keySet()));
        var resolved = List.of(new FileItem(RULES, IOUtils.toInputStream("<project><name>Feature</name></project>")));
        repo.merge("feature",
                AUTHOR,
                new ConflictResolveData(conflict.getDetails().theirCommit(), resolved, "Resolve the conflict"));

        assertEquals("1.1.0", read(POM));
        assertEquals("<project><name>Feature</name></project>", read(RULES));
    }

    /** A resolution given for an ignored file is no way to write it: the file keeps the version merged into. */
    @Test
    void aResolutionOfAnIgnoredFileIsLeftOut() throws IOException {
        save(repo, RULES, "<project><name>Master</name></project>");
        save(feature, RULES, "<project><name>Feature</name></project>");

        var conflict = assertThrows(MergeConflictException.class, () -> repo.merge("feature", AUTHOR, null));

        var resolved = List.of(new FileItem(RULES, IOUtils.toInputStream("<project><name>Feature</name></project>")),
                new FileItem(POM, IOUtils.toInputStream("6.6.6")));
        repo.merge("feature",
                AUTHOR,
                new ConflictResolveData(conflict.getDetails().theirCommit(), resolved, "Resolve the conflict"));

        assertEquals("1.1.0", read(POM));
        assertEquals("<project><name>Feature</name></project>", read(RULES));
    }
}
