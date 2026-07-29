package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.WorkspaceUserImpl;

/**
 * The branch marks a project carries — whether it sits on the repository main branch, and whether that
 * branch is protected. Both are read from the design repository, so they stay correct while the project is
 * open and edited locally.
 */
class RulesProjectBranchMarksTest {

    private static final String PROJECT = "Example 1";

    @TempDir
    Path designRoot;

    @TempDir
    Path userDir;

    @Test
    void projectOnTheMainBranchIsDefault() throws Exception {
        var project = createProject(designRepository("main", "main"));

        assertTrue(project.isBranchDefault());
    }

    @Test
    void projectOnAnotherBranchIsNotDefault() throws Exception {
        var project = createProject(designRepository("feature/rates", "main"));

        assertFalse(project.isBranchDefault());
    }

    @Test
    void editedProjectKeepsReadingTheBranchFromTheDesignRepository() throws Exception {
        var project = createProject(designRepository("main", "main"));
        project.open();
        // Editing binds the project to its local copy, which knows nothing about branches.
        project.getLocalRepository().save(fileData(), stream("edited content!"));

        assertEquals(ProjectStatus.EDITING, project.getStatus());
        assertTrue(project.isBranchDefault());
        assertFalse(project.isBranchProtected());
    }

    @Test
    void editedProjectOnANonMainBranchStaysNotDefault() throws Exception {
        var project = createProject(designRepository("feature/rates", "main"));
        project.open();
        project.getLocalRepository().save(fileData(), stream("edited content!"));

        assertEquals(ProjectStatus.EDITING, project.getStatus());
        assertFalse(project.isBranchDefault());
    }

    @Test
    void projectInARepositoryWithoutBranchesIsNeverDefault() throws Exception {
        var project = createProject(new StubDesignRepository());

        assertFalse(project.isBranchDefault());
    }

    private RulesProject createProject(FileSystemRepository designRepository) throws IOException {
        designRepository.setRoot(designRoot);
        designRepository.setId("design");
        designRepository.initialize();
        designRepository.save(fileData(), stream("design content"));

        var localRepository = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        localRepository.setId("design");
        localRepository.initialize();

        return new RulesProject(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localRepository,
                null,
                designRepository,
                designRepository.check(PROJECT),
                new DummyLockEngine());
    }

    private static BranchedDesignRepository designRepository(String branch, String baseBranch) {
        return new BranchedDesignRepository(branch, baseBranch);
    }

    private static FileData fileData() {
        var fileData = new FileData();
        fileData.setName(PROJECT + "/rules/Main.xlsx");
        return fileData;
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** A file-system design repository with a stub revision, which {@code open()} requires. */
    private static class StubDesignRepository extends FileSystemRepository {

        @Override
        protected String getVersion(Path file) {
            return "rev-1";
        }

        @Override
        protected String getVersion(String path) {
            return "rev-1";
        }
    }

    /** A design repository that reports itself as sitting on {@code branch} of a repository based on {@code baseBranch}. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class BranchedDesignRepository extends StubDesignRepository implements BranchRepository {

        @Getter
        private final String branch;
        @Getter
        private final String baseBranch;

        @Override
        public Features supports() {
            return new FeaturesBuilder(this).setVersions(false).setFolders(true).setBranches(true).build();
        }

        @Override
        public boolean isBranchProtected(String branch) {
            return false;
        }

        @Override
        public boolean isMergedInto(String from, String to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createBranch(String projectPath, String branch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createBranch(String projectPath, String branch, String startPoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteBranch(String projectPath, String branch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> listBranches() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getBranches(String projectPath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, BranchStatus> getBranchStatuses(Collection<String> branches) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BranchRepository forBranch(String branch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidBranchName(String branch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean branchExists(String branch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pull(UserInfo author) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FileData> listHistory(String name, String globalFilter, boolean techRevs, Pageable pageable) {
            throw new UnsupportedOperationException();
        }
    }
}
