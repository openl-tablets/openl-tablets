package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.openl.rules.repository.git.TestGitUtils.createNewFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.xls.merge.HSSFPaletteMatcher;
import org.openl.rules.xls.merge.XlsWorkbooksMatcher;
import org.openl.rules.xls.merge.diff.XlsMatch;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.formatters.FileNameFormatter;

public class GitRepositoryMergeConflictsInExcelTest {

    private static final String COPY_BRANCH_PREF = "_COPY";
    private static final Path TEST_CASES_ROOT = Paths.get("test-resources/EPBDS-8483");
    private static final UserInfo USER_INFO = new UserInfo("jsmith", "jsmith@email", "John Smith");

    private static File template;

    private File root;
    private GitRepository repo;

    @BeforeAll
    public static void initialize() throws IOException, GitAPIException {
        template = Files.createTempDirectory("openl-merge-test-template").toFile();
        // Initialize remote repository
        try (Git git = Git.init().setDirectory(template).call()) {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            config.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            config.save();

            File parent = repository.getDirectory().getParentFile();

            // create initial commit in master
            createNewFile(parent, "README.md", "# openl-merge-template");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").setCommitter("User 1", "user1@email.to").call();
        }
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        FileUtils.delete(template.toPath());
    }

    @BeforeEach
    public void setUp() throws IOException {
        root = Files.createTempDirectory("openl-merge-test").toFile();

        File remote = new File(root, "remote");
        File local = new File(root, "local");

        FileUtils.copy(template, remote);
        repo = createRepository(remote, local);
    }

    @AfterEach
    public void reset() throws IOException {
        try {
            repo.close();
        } finally {
            FileUtils.delete(root.toPath());
        }
    }

    private GitRepository createRepository(File remote, File local) {
        GitRepository repo = new GitRepository();
        repo.setUri(remote.toURI().toString());
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setBranch("master");
        repo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        String settingsPath = local.getParent() + "/git-settings";
        FileSystemRepository settingsRepository = new FileSystemRepository();
        settingsRepository.setUri(settingsPath);
        String locksRoot = new File(local.getParent(), "locks").getAbsolutePath();
        repo.setRepositorySettings(new RepositorySettings(settingsRepository, locksRoot, 1));
        repo.setGcAutoDetach(false);
        repo.initialize();

        return repo;
    }

    @Test
    public void testCase_01() throws IOException, GitAPIException {
        executeTestCase("01", Set.of("01/MyBook.xlsx"), "01/MyBook.xlsx", "01/MyBook.xlsx");
    }

    @Test
    public void testCase_02() throws IOException, GitAPIException {
        executeTestCase("02",
                Set.of("02/Main.xlsx"),
                "02/Main.xlsx\n\t\tSheet1 (02)\n\t\tRules (master)",
                "02/Main.xlsx\n\t\tRules (master)\n\t\tSheet1 (02_COPY)");
    }

    @Test
    public void testCase_03() throws IOException, GitAPIException {
        executeTestCase("03",
                Set.of("03/Case1.xlsx"),
                "03/Case1.xlsx\n\t\tA (03)\n\t\tC (03)\n\t\tB (master)\n\t\tD (master)\n\t\tF (master)\n\t\tG (master)\n\t\tH (master)",
                "03/Case1.xlsx\n\t\tB (master)\n\t\tD (master)\n\t\tF (master)\n\t\tG (master)\n\t\tH (master)\n\t\tA (03_COPY)\n\t\tC (03_COPY)");
    }

    @Test
    public void testCase_04() throws IOException, GitAPIException {
        TestCaseData testCaseData = initializeTestCase("04");
        GitRepository branchRepo = repo.forBranch("04");
        try {
            branchRepo.merge(Constants.MASTER, USER_INFO, null);
            fail("¯\\_(ツ)_/¯");
        } catch (MergeConflictException e) {
            assertEquals(testCaseData.baseRevision, e.getBaseCommit());
            assertEquals(testCaseData.ourRevision, e.getYourCommit());
            assertEquals(testCaseData.theirRevision, e.getTheirCommit());
            assertTrue(e.getDiffs().containsKey("04/Bank Rating.xlsx"));
            assertEquals(1, e.getDiffs().size());

            assertTrue(e.getToAutoResolve().containsKey("04/MyBook.xlsx"));
            assertEquals(1, e.getToAutoResolve().size());
        }

        // Test symmetry merge
        branchRepo = repo.forBranch(Constants.MASTER);
        try {
            branchRepo.merge("04" + COPY_BRANCH_PREF, USER_INFO, null);
            fail("¯\\_(ツ)_/¯");
        } catch (MergeConflictException e) {
            assertEquals(testCaseData.baseRevision, e.getBaseCommit());
            assertEquals(testCaseData.ourRevision, e.getTheirCommit());
            assertEquals(testCaseData.theirRevision, e.getYourCommit());
            assertTrue(e.getDiffs().containsKey("04/Bank Rating.xlsx"));
            assertEquals(1, e.getDiffs().size());

            assertTrue(e.getToAutoResolve().containsKey("04/MyBook.xlsx"));
            assertEquals(1, e.getToAutoResolve().size());
        }
    }

    @Test
    public void testCase_05() throws IOException, GitAPIException {
        executeTestCase("05",
                Set.of("05/Case4.xlsx"),
                "05/Case4.xlsx\n\t\tTargetFactor (05)\n\t\tSalaryAdjustment (master)",
                "05/Case4.xlsx\n\t\tSalaryAdjustment (master)\n\t\tTargetFactor (05_COPY)");
    }

    @Test
    public void testCase_06() throws IOException, GitAPIException {
        executeTestCase("06",
                Set.of("06/BugCase3.xlsx"),
                "06/BugCase3.xlsx\n\t\tClaim Cost Benefit (06)\n\t\tBenefit Adjust Prc (master)",
                "06/BugCase3.xlsx\n\t\tBenefit Adjust Prc (master)\n\t\tClaim Cost Benefit (06_COPY)");
    }

    @Test
    public void testCase_07() throws IOException, GitAPIException {
        executeTestCase("07",
                Set.of("07/Case2.xls"),
                "07/Case2.xls\n" + "\t\tB (07)\n" + "\t\tC (07)\n" + "\t\tG (07)\n" + "\t\tH (07)\n" + "\t\tK (07)\n" + "\t\tL (07)\n" + "\t\tA (master)\n" + "\t\tD (master)\n" + "\t\tE (master)\n" + "\t\tI (master)\n" + "\t\tJ (master)",
                "07/Case2.xls\n" + "\t\tA (master)\n" + "\t\tD (master)\n" + "\t\tE (master)\n" + "\t\tI (master)\n" + "\t\tJ (master)\n" + "\t\tB (07_COPY)\n" + "\t\tC (07_COPY)\n" + "\t\tG (07_COPY)\n" + "\t\tH (07_COPY)\n" + "\t\tK (07_COPY)\n" + "\t\tL (07_COPY)");
    }

    @Test
    public void testCase_08() throws IOException, GitAPIException {
        executeTestCase("08",
                Set.of("08/AutoPolicyCalculation.xlsx"),
                "08/AutoPolicyCalculation.xlsx\n" + "\t\tDriver-Eligibility (08)\n" + "\t\tCalculation (master)\n" + "\t\tUtilities (master)",
                "08/AutoPolicyCalculation.xlsx\n" + "\t\tCalculation (master)\n" + "\t\tUtilities (master)\n" + "\t\tDriver-Eligibility (08_COPY)");
    }

    @Test
    public void testCase_09() throws IOException, GitAPIException {
        executeTestCase("09",
                Set.of("09/defect5.xls"),
                "09/defect5.xls\n" + "\t\tCapping Calculation (09)\n" + "\t\tProgram Name (master)",
                "09/defect5.xls\n" + "\t\tProgram Name (master)\n" + "\t\tCapping Calculation (09_COPY)");
    }

    @Test
    public void testCase_10() throws IOException, GitAPIException {
        executeTestCase("10",
                Set.of("10/Main.xlsx"),
                "10/Main.xlsx\n" + "\t\tRules (10)\n" + "\t\tSheet1 (master)",
                "10/Main.xlsx\n" + "\t\tSheet1 (master)\n" + "\t\tRules (10_COPY)");
    }

    @Test
    public void testCase_11() throws IOException, GitAPIException {
        executeTestCase("11",
                Set.of("11/bugMessageBug.xlsx"),
                "11/bugMessageBug.xlsx\n" + "\t\tSheet1 (11)\n" + "\t\tSheet2 (master)",
                "11/bugMessageBug.xlsx\n" + "\t\tSheet2 (master)\n" + "\t\tSheet1 (11_COPY)");
    }

    @Test
    public void testCase_12() throws IOException, GitAPIException {
        executeTestCase("12",
                Set.of("12/Bank Rating.xlsx"),
                "12/Bank Rating.xlsx\n" + "\t\tSetNonZero Test (12)\n" + "\t\tTest Data (master)",
                "12/Bank Rating.xlsx\n" + "\t\tTest Data (master)\n" + "\t\tSetNonZero Test (12_COPY)");
    }

    private void executeTestCase(String testCase,
                                 Set<String> expectedModifiedFiles,
                                 String expectedMrMessage1,
                                 String expectedMrMessage2) throws IOException, GitAPIException {
        TestCaseData testCaseData = initializeTestCase(testCase);
        GitRepository branchRepo = repo.forBranch(testCase);
        branchRepo.merge(Constants.MASTER, USER_INFO, null);
        String masterToBranchMergeCommit;
        try (Git git = repo.getClosableGit()) {
            RevCommit mergeCommit = git.log().setMaxCount(1).call().iterator().next();
            masterToBranchMergeCommit = mergeCommit.getName();
            String actualCommitMessage = mergeCommit.getFullMessage();
            String expectedCommitMessage = String.format(
                    "Merge commit with %s\n\n Automatically resolved conflicts:\n\t" + expectedMrMessage1,
                    testCaseData.theirRevision);
            assertEquals(expectedCommitMessage, actualCommitMessage);

            var repository = git.getRepository();
            RevCommit theirCommit = repository.parseCommit(repository.resolve(testCaseData.theirRevision));
            List<DiffEntry> diffs = getDiffs(git, mergeCommit, theirCommit);
            for (DiffEntry diffEntry : diffs) {
                assertEquals(DiffEntry.ChangeType.MODIFY, diffEntry.getChangeType());
                assertTrue(expectedModifiedFiles.contains(diffEntry.getNewPath()), "File must be modified");
            }

            RevCommit ourCommit = repository.parseCommit(repository.resolve(testCaseData.ourRevision));
            diffs = getDiffs(git, mergeCommit, ourCommit);
            for (DiffEntry diffEntry : diffs) {
                assertEquals(DiffEntry.ChangeType.MODIFY, diffEntry.getChangeType());
                assertTrue(expectedModifiedFiles.contains(diffEntry.getNewPath()), "File must be modified");
            }
        }

        // Test symmetry merge
        branchRepo = repo.forBranch(Constants.MASTER);
        branchRepo.merge(testCase + COPY_BRANCH_PREF, USER_INFO, null);
        String branchToMasterMergeCommit;
        try (Git git = repo.getClosableGit()) {
            RevCommit mergeCommit = git.log().setMaxCount(1).call().iterator().next();
            branchToMasterMergeCommit = mergeCommit.getName();
            String actualCommitMessage = mergeCommit.getFullMessage();
            String expectedCommitMessage = String.format(
                    "Merge commit with %s\n\n Automatically resolved conflicts:\n\t" + expectedMrMessage2,
                    testCaseData.ourRevision);
            assertEquals(expectedCommitMessage, actualCommitMessage);
        }

        assertSymmetryMerge(testCase, masterToBranchMergeCommit, branchToMasterMergeCommit);
    }

    private List<DiffEntry> getDiffs(Git git, RevCommit commit1, RevCommit commit2) throws IOException {
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        return new ArrayList<>(df.scan(commit1.getTree(), commit2.getTree()));
    }

    private TestCaseData initializeTestCase(String testCase) throws IOException, GitAPIException {
        TestCaseData testCaseData = new TestCaseData();
        Path testCaseRoot = TEST_CASES_ROOT.resolve(testCase);
        try (Git git = repo.getClosableGit()) {
            File gitRoot = git.getRepository().getDirectory().getParentFile();
            File gitTestCase = new File(gitRoot, testCase);
            Files.createDirectory(gitTestCase.toPath());

            testCaseData.baseRevision = initializeBaseRevision(git, gitTestCase, testCaseRoot.resolve("BASE"));
            testCaseData.ourRevision = initializeBranchRevision(git, gitTestCase, testCaseRoot.resolve("OUR"));
            testCaseData.theirRevision = initializeTheirRevision(git, gitTestCase, testCaseRoot.resolve("THEIR"));

            git.push().call();
        }
        return testCaseData;
    }

    private String initializeBaseRevision(Git git, File gitTestCase, Path baseSrc) throws IOException, GitAPIException {
        FileUtils.copy(baseSrc.toFile(), gitTestCase);
        git.add().addFilepattern(".").call();
        RevCommit commit = git.commit()
                .setMessage("Initial test case '" + gitTestCase.getName() + "' commit")
                .setCommitter("User 1", "user1@email.to")
                .call();
        return commit.getName();
    }

    private String initializeBranchRevision(Git git, File gitTestCase, Path ourSrc) throws GitAPIException,
            IOException {
        String testCaseName = gitTestCase.getName();
        git.branchCreate().setName(testCaseName).call();
        git.checkout().setName(testCaseName).call();

        FileUtils.copy(ourSrc.toFile(), gitTestCase);
        git.add().addFilepattern(".").call();
        RevCommit commit = git.commit()
                .setMessage("Our branch test case '" + gitTestCase.getName() + "' commit")
                .setCommitter("User 1", "user1@email.to")
                .call();

        git.branchCreate().setName(testCaseName + COPY_BRANCH_PREF).call();
        return commit.getName();
    }

    private String initializeTheirRevision(Git git, File gitTestCase, Path theirSrc) throws IOException,
            GitAPIException {
        git.checkout().setName(Constants.MASTER).call();
        FileUtils.copy(theirSrc.toFile(), gitTestCase);
        git.add().addFilepattern(".").call();
        RevCommit commit = git.commit()
                .setMessage("Their test case '" + gitTestCase.getName() + "' commit")
                .setCommitter("User 1", "user1@email.to")
                .call();
        return commit.getName();
    }

    private void assertSymmetryMerge(String testCase,
                                     String masterToBranchMergeCommit,
                                     String branchToMasterMergeCommit) throws IOException {
        Path basePath = Paths.get(testCase);
        Path testCaseRoot = TEST_CASES_ROOT.resolve(testCase);
        Set<String> filesToVerify = new HashSet<>();
        Files.walkFileTree(testCaseRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (FileTypeHelper.isExcelFile(file.getFileName().toString())) {
                    Path relative = testCaseRoot.relativize(file);
                    Path fileRelative = relative.subpath(1, relative.getNameCount());
                    filesToVerify.add(FileNameFormatter.normalizePath(basePath.resolve(fileRelative).toString()));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        assertFalse(filesToVerify.isEmpty());
        ZipSecureFile.setMinInflateRatio(0.001);
        for (String fileToVerify : filesToVerify) {
            Map<String, XlsMatch> matchResult = null;
            boolean hssf;
            Map<Short, XlsMatch> paletteMatchResult = null;
            try (var file1 = repo.readHistory(fileToVerify, masterToBranchMergeCommit);
                 var file2 = repo.readHistory(fileToVerify, branchToMasterMergeCommit)) {
                try (var wb1 = WorkbookFactory.create(file1.getStream());
                     var wb2 = WorkbookFactory.create(file2.getStream())) {
                    hssf = wb1 instanceof HSSFWorkbook;
                    matchResult = XlsWorkbooksMatcher.match(wb1, wb2);
                    if (hssf) {
                        paletteMatchResult = HSSFPaletteMatcher.matchPalette((HSSFWorkbook) wb1, (HSSFWorkbook) wb2);
                    }
                }
            }
            assertNotNull(matchResult);
            matchResult.values().forEach(actual -> assertEquals(XlsMatch.EQUAL, actual));
            if (hssf) {
                assertNotNull(paletteMatchResult);
                paletteMatchResult.values().forEach(actual -> assertEquals(XlsMatch.EQUAL, actual));
            }
        }
    }

    private static class TestCaseData {

        String baseRevision;
        String ourRevision;
        String theirRevision;

    }

}
