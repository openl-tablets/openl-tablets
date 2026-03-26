package org.openl.studio.projects.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;

class ProjectStateValidatorImplTest {

    private static final Features NON_BRANCH_FEATURES = new FeaturesBuilder(mock(Repository.class)).build();
    private static final Features BRANCH_FEATURES = new FeaturesBuilder(mock(BranchRepository.class)).build();

    private ProjectStateValidatorImpl validator;

    @BeforeEach
    void setUp() {
        validator = new ProjectStateValidatorImpl();
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
    void canDelete_branchRepoNoVersion_returnsFalse() {
        var project = projectWith().branchRepo(true).build();
        when(project.getVersion()).thenReturn(null);
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_notOpenedNotLocked_returnsTrue() {
        var project = projectWith().build();
        assertTrue(validator.canDelete(project));
    }

    @Test
    void canDelete_opened_returnsFalse() {
        var project = projectWith().opened(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_locked_returnsFalse() {
        var project = projectWith().locked(true).build();
        assertFalse(validator.canDelete(project));
    }

    @Test
    void canDelete_lockedByMe_returnsFalse() {
        var project = projectWith().lockedByMe(true).build();
        assertFalse(validator.canDelete(project));
    }

    // --- canErase ---

    @Test
    void canErase_null_returnsFalse() {
        assertFalse(validator.canErase(null));
    }

    @Test
    void canErase_deleted_returnsTrue() {
        var project = projectWith().deleted(true).build();
        assertTrue(validator.canErase(project));
    }

    @Test
    void canErase_notDeleted_returnsFalse() {
        var project = projectWith().build();
        assertFalse(validator.canErase(project));
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
    void canMerge_singleBranch_returnsFalse() throws IOException {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getDesignFolderName()).thenReturn("project");
        when(repo.getBranches("project")).thenReturn(List.of("main"));
        assertFalse(validator.canMerge(project));
    }

    @Test
    void canMerge_twoBranches_returnsTrue() throws IOException {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getDesignFolderName()).thenReturn("project");
        when(repo.getBranches("project")).thenReturn(List.of("main", "feature"));
        assertTrue(validator.canMerge(project));
    }

    @Test
    void canMerge_ioException_returnsFalse() throws IOException {
        var project = mock(RulesProject.class);
        var repo = mock(BranchRepository.class);
        when(repo.supports()).thenReturn(BRANCH_FEATURES);
        when(project.getDesignRepository()).thenReturn(repo);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.isModified()).thenReturn(false);
        when(project.getDesignFolderName()).thenReturn("project");
        when(repo.getBranches("project")).thenThrow(new IOException("connection failed"));
        assertFalse(validator.canMerge(project));
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
                when(project.getDesignRepository()).thenReturn(repo);
                when(project.getRepository()).thenReturn(repo);
                when(project.getBranch()).thenReturn("main");
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
