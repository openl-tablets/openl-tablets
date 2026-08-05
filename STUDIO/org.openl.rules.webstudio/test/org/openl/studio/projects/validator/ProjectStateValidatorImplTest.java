package org.openl.studio.projects.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;

class ProjectStateValidatorImplTest {

    private static final Features NON_BRANCH_FEATURES = new FeaturesBuilder(mock(Repository.class)).build();
    private static final Features BRANCH_FEATURES = new FeaturesBuilder(mock(BranchRepository.class)).build();

    private ProjectStateValidatorImpl validator;
    private ProtectedBranchBypassService bypassService;
    private DesignTimeRepository designTimeRepository;

    @BeforeEach
    void setUp() {
        bypassService = mock(ProtectedBranchBypassService.class);
        designTimeRepository = mock(DesignTimeRepository.class);
        // Default: bypass not granted, so protection is enforced exactly when the branch is
        // marked protected on the repo. Tests asserting the bypass case override this.
        when(bypassService.isProtectionEnforced(any(BranchRepository.class), any(), any(AProject.class)))
                .thenAnswer(inv -> {
                    BranchRepository repo = inv.getArgument(0);
                    String branch = inv.getArgument(1);
                    return repo.isBranchProtected(branch);
                });
        validator = new ProjectStateValidatorImpl(bypassService, designTimeRepository);
    }

    // --- canSave ---

    @Test
    void canSave_null_returnsFalse() {
        assertFalse(validator.canSave(null));
    }

    @Test
    void canSave_notModified_returnsFalse() {
        var project = projectWith().build();
        assertFalse(validator.canSave(project));
    }

    @Test
    void canSave_modifiedLocalOnly_returnsTrue() {
        var project = projectWith().modified(true).localOnly(true).build();
        assertTrue(validator.canSave(project));
    }

    @Test
    void canSave_modifiedAndOpenedForEditing_returnsTrue() {
        var project = projectWith().modified(true).openedForEditing(true).build();
        assertTrue(validator.canSave(project));
    }

    @Test
    void canSave_modifiedNotLocked_returnsTrue() {
        var project = projectWith().modified(true).build();
        assertTrue(validator.canSave(project));
    }

    @Test
    void canSave_modifiedButLockedByOther_returnsFalse() {
        var project = projectWith().modified(true).locked(true).build();
        assertFalse(validator.canSave(project));
    }

    @Test
    void canSave_modifiedOnProtectedBranch_returnsFalse() {
        var project = projectWith().modified(true).openedForEditing(true).protectedBranch(true).build();
        assertFalse(validator.canSave(project));
    }

    @Test
    void canSave_modifiedOnProtectedBranchButBypassEligible_returnsTrue() {
        // Manager with global setting enabled → bypass service reports protection NOT enforced
        var project = projectWith().modified(true).openedForEditing(true).protectedBranch(true).build();
        when(bypassService.isProtectionEnforced(any(BranchRepository.class), any(), any(AProject.class)))
                .thenReturn(false);
        assertTrue(validator.canSave(project));
    }

    // --- canModify ---

    @Test
    void canModify_null_returnsFalse() {
        assertFalse(validator.canModify(null));
    }

    @Test
    void canModify_localOnly_returnsTrue() {
        var project = projectWith().localOnly(true).build();
        assertTrue(validator.canModify(project));
    }

    @Test
    void canModify_notLockedNotOpenedForEditing_returnsTrue() {
        var project = projectWith().build();
        assertTrue(validator.canModify(project));
    }

    @Test
    void canModify_lockedNotOpenedForEditing_returnsFalse() {
        var project = projectWith().locked(true).build();
        assertFalse(validator.canModify(project));
    }

    @Test
    void canModify_lockedAndOpenedForEditing_returnsTrue() {
        var project = projectWith().locked(true).openedForEditing(true).build();
        assertTrue(validator.canModify(project));
    }

    @Test
    void canModify_protectedBranch_returnsFalse() {
        var project = projectWith().protectedBranch(true).build();
        assertFalse(validator.canModify(project));
    }

    // --- canClose ---

    @Test
    void canClose_null_returnsFalse() {
        assertFalse(validator.canClose(null));
    }

    @Test
    void canClose_deleted_returnsFalse() {
        var project = projectWith().deleted(true).opened(true).build();
        assertFalse(validator.canClose(project));
    }

    @Test
    void canClose_localOnly_returnsFalse() {
        var project = projectWith().localOnly(true).opened(true).build();
        assertFalse(validator.canClose(project));
    }

    @Test
    void canClose_openedNotLocal_returnsTrue() {
        var project = projectWith().opened(true).build();
        assertTrue(validator.canClose(project));
    }

    @Test
    void canClose_notOpened_returnsFalse() {
        var project = projectWith().build();
        assertFalse(validator.canClose(project));
    }

    // --- canOpen ---

    @Test
    void canOpen_null_returnsFalse() {
        assertFalse(validator.canOpen(null));
    }

    @Test
    void canOpen_deleted_returnsFalse() {
        var project = projectWith().deleted(true).build();
        assertFalse(validator.canOpen(project));
    }

    @Test
    void canOpen_localOnly_returnsFalse() {
        var project = projectWith().localOnly(true).build();
        assertFalse(validator.canOpen(project));
    }

    @Test
    void canOpen_alreadyOpened_returnsFalse() {
        var project = projectWith().opened(true).build();
        assertFalse(validator.canOpen(project));
    }

    @Test
    void canOpen_openedForEditing_returnsFalse() {
        var project = projectWith().openedForEditing(true).build();
        assertFalse(validator.canOpen(project));
    }

    @Test
    void canOpen_closedNotLocal_returnsTrue() {
        var project = projectWith().build();
        assertTrue(validator.canOpen(project));
    }

    // --- canDeploy ---

    @Test
    void canDeploy_null_returnsFalse() {
        assertFalse(validator.canDeploy(null));
    }

    @Test
    void canDeploy_deleted_returnsFalse() {
        var project = projectWith().deleted(true).build();
        assertFalse(validator.canDeploy(project));
    }

    @Test
    void canDeploy_modified_returnsFalse() {
        var project = projectWith().modified(true).build();
        assertFalse(validator.canDeploy(project));
    }

    @Test
    void canDeploy_notModified_returnsTrue() {
        var project = projectWith().build();
        assertTrue(validator.canDeploy(project));
    }

    // --- canDelete ---

    @Test
    void canDelete_null_returnsFalse() {
        assertFalse(validator.canDelete(null));
    }

    @Test
    void canDelete_deleted_returnsFalse() {
        var project = projectWith().deleted(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_localOnly_returnsTrue() {
        var project = projectWith().localOnly(true).build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_onMainBranch_returnsTrue() {
        var project = projectWith().branchRepo(true).build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_missingFromCurrentBranch_returnsFalse() {
        var project = projectWith().branchRepo(true).existsInBranch(false).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_onNonMainBranch_returnsTrue() {
        var project = projectWith().branch("feature").build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_onProtectedBranch_returnsFalse() {
        var project = projectWith().protectedBranch(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_notOpenedNotLocked_returnsTrue() {
        var project = projectWith().build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_opened_returnsTrue() {
        // A freshly created project is opened in the creator's workspace; deletion closes it first.
        var project = projectWith().opened(true).build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_lockedByOther_returnsFalse() {
        var project = projectWith().locked(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_openedAndLockedByOther_returnsFalse() {
        var project = projectWith().opened(true).locked(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_lockedByMe_returnsTrue() {
        var project = projectWith().locked(true).lockedByMe(true).build();
        assertTrue(validator.canDelete(project));
    }

    // --- canMerge ---

    @Test
    void canMerge_null_returnsFalse() {
        assertFalse(validator.canMerge(null));
    }

    @Test
    void canMerge_noBranchSupport_returnsFalse() {
        var project = mock(RulesProject.class);
        var repo = mock(Repository.class);
        when(repo.supports()).thenReturn(NON_BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        assertFalse(validator.canMerge(project));
    }

    @Test
    void canMerge_localOnly_returnsFalse() {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(true);
        assertFalse(validator.canMerge(project));
    }

    @Test
    void canMerge_modified_returnsFalse() {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(true);
        assertFalse(validator.canMerge(project));
    }

    @Test
    void canMerge_singleBranch_returnsFalse() {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getBranch()).thenReturn("feature");
        try {
            when(repo.listBranches()).thenReturn(List.of("feature"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertFalse(validator.canMerge(project));
    }

    @Test
    void canMerge_whenAnotherRepositoryBranchDoesNotContainProject_returnsTrue() throws Exception {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getBranch()).thenReturn("feature");
        when(repo.listBranches()).thenReturn(List.of("main", "feature"));

        assertTrue(validator.canMerge(project));
    }

    @Test
    void canMerge_whenBranchesCannotBeRead_returnsFalse() throws Exception {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getBranch()).thenReturn("feature");
        when(project.getName()).thenReturn("project");
        when(repo.listBranches()).thenThrow(new IOException("unavailable"));

        assertFalse(validator.canMerge(project));
    }

    // --- canDeleteBranch ---

    @Test
    void canDeleteBranch_null_returnsFalse() {
        assertFalse(validator.canDeleteBranch(null));
    }

    @Test
    void canDeleteBranch_localOnly_returnsFalse() {
        var project = branchProject("feature");
        when(project.isLocalOnly()).thenReturn(true);
        assertFalse(validator.canDeleteBranch(project));
    }

    @Test
    void canDeleteBranch_baseBranch_returnsFalse() {
        var project = branchProject("main");
        heldBy(project, "main", "feature");
        assertFalse(validator.canDeleteBranch(project));
    }

    @Test
    void canDeleteBranch_protectedBranchWithoutBypass_returnsFalse() {
        var project = branchProject("release");
        when(((BranchRepository) project.getDesignRepository()).isBranchProtected("release")).thenReturn(true);
        heldBy(project, "main", "release");
        assertFalse(validator.canDeleteBranch(project));
    }

    @Test
    void canDeleteBranch_nonBaseUnprotectedBranch_returnsTrue() {
        // Whether the deletion also removes the project is answered by isLastProjectBranch.
        assertTrue(validator.canDeleteBranch(branchProject("feature")));
    }

    // --- isLastProjectBranch ---

    @Test
    void isLastProjectBranch_null_returnsFalse() {
        assertFalse(validator.isLastProjectBranch(null));
    }

    @Test
    void isLastProjectBranch_onlyBranchHoldingTheProject_returnsTrue() {
        var project = branchProject("feature");
        heldBy(project, "feature");
        assertTrue(validator.isLastProjectBranch(project));
    }

    @Test
    void isLastProjectBranch_anotherBranchHoldsTheProject_returnsFalse() {
        var project = branchProject("feature");
        heldBy(project, "main", "feature");
        assertFalse(validator.isLastProjectBranch(project));
    }

    @Test
    void isLastProjectBranch_projectMissingFromTheIndex_returnsTrue() {
        var project = branchProject("feature");
        when(designTimeRepository.getBranchedProject("design", "Rates")).thenReturn(Optional.empty());
        // Nothing says another branch holds it, so the deletion is treated as removing the project.
        assertTrue(validator.isLastProjectBranch(project));
    }

    /** A committed project of a branch repository, sitting on the given branch. */
    private static RulesProject branchProject(String branch) {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(repo.getId()).thenReturn("design");
        when(repo.getBaseBranch()).thenReturn("main");
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getBranch()).thenReturn(branch);
        when(project.getDesignProjectName()).thenReturn("Rates");
        return project;
    }

    /** Publishes the project in the index under the given branches. */
    private void heldBy(RulesProject project, String... branches) {
        var status = new BranchStatus(new UserInfo("author"), Instant.EPOCH, "Change", "revision");
        var entries = new LinkedHashMap<String, BranchedProject.BranchEntry>();
        for (String branch : branches) {
            entries.put(branch, new BranchedProject.BranchEntry(mock(AProject.class), status));
        }
        when(designTimeRepository.getBranchedProject("design", "Rates"))
                .thenReturn(Optional.of(new BranchedProject("Rates", branches[0], "main", entries)));
    }

    // --- Test project builder ---

    private static ProjectMockBuilder projectWith() {
        return new ProjectMockBuilder();
    }

    private static class ProjectMockBuilder {
        private boolean modified;
        private boolean localOnly;
        private boolean locked;
        private boolean openedForEditing;
        private boolean deleted;
        private boolean lockedByMe;
        private boolean opened;
        private boolean protectedBranch;
        private boolean branchRepo;
        private boolean existsInBranch = true;
        private String branch = "main";

        ProjectMockBuilder modified(boolean v) {
            this.modified = v;
            return this;
        }

        ProjectMockBuilder localOnly(boolean v) {
            this.localOnly = v;
            return this;
        }

        ProjectMockBuilder locked(boolean v) {
            this.locked = v;
            return this;
        }

        ProjectMockBuilder openedForEditing(boolean v) {
            this.openedForEditing = v;
            return this;
        }

        ProjectMockBuilder deleted(boolean v) {
            this.deleted = v;
            return this;
        }

        ProjectMockBuilder lockedByMe(boolean v) {
            this.lockedByMe = v;
            return this;
        }

        ProjectMockBuilder opened(boolean v) {
            this.opened = v;
            return this;
        }

        ProjectMockBuilder protectedBranch(boolean v) {
            this.protectedBranch = v;
            return this;
        }

        ProjectMockBuilder branchRepo(boolean v) {
            this.branchRepo = v;
            return this;
        }

        ProjectMockBuilder existsInBranch(boolean v) {
            this.existsInBranch = v;
            return this;
        }

        ProjectMockBuilder branch(String v) {
            this.branch = v;
            this.branchRepo = true;
            return this;
        }

        UserWorkspaceProject build() {
            var project = mock(UserWorkspaceProject.class);
            when(project.isModified()).thenReturn(modified);
            when(project.isLocalOnly()).thenReturn(localOnly);
            when(project.isLocked()).thenReturn(locked);
            when(project.isOpenedForEditing()).thenReturn(openedForEditing);
            when(project.isDeleted()).thenReturn(deleted);
            when(project.isLockedByMe()).thenReturn(lockedByMe);
            when(project.isOpened()).thenReturn(opened);

            if (protectedBranch || branchRepo) {
                var repo = mock(BranchRepository.class);
                when(repo.supports()).thenReturn(BRANCH_FEATURES);
                when(repo.isBranchProtected(any())).thenReturn(protectedBranch);
                when(repo.getBaseBranch()).thenReturn("main");
                when(project.getDesignRepository()).thenReturn(repo);
                when(project.getRepository()).thenReturn(repo);
                when(project.getBranch()).thenReturn(branch);
                if (existsInBranch) {
                    var version = mock(ProjectVersion.class);
                    when(project.getVersion()).thenReturn(version);
                }
            } else if (!localOnly) {
                var repo = mock(Repository.class);
                when(repo.supports()).thenReturn(NON_BRANCH_FEATURES);
                when(project.getDesignRepository()).thenReturn(repo);
                when(project.getRepository()).thenReturn(repo);
            }

            return project;
        }
    }
}
