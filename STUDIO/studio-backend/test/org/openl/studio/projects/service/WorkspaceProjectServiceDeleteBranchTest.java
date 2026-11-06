package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryProjects;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.ProjectAuditor;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.project.compile.ModuleCompilationLauncher;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.SystemPropertiesService;
import org.openl.studio.projects.service.tables.TableCopyService;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.TableDetailsService;
import org.openl.studio.projects.service.tables.TablePropertiesService;
import org.openl.studio.projects.service.tables.TableRunStateService;
import org.openl.studio.projects.service.tables.TableVersionService;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.read.TableEditorsReader;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * A branch on which the project is locked by another user must not be deleted: the lock means the
 * user is editing the project there. The lock owner keeps the right to delete the branch.
 *
 * @author Yury Molchan
 */
class WorkspaceProjectServiceDeleteBranchTest {

    private static final String BRANCH = "user-a";

    private RepositoryAclService designRepositoryAclService;
    private AclProjectsHelper aclProjectsHelper;
    private UserWorkspace userWorkspace;
    private LockEngine lockEngine;
    private RulesProject project;
    private BranchRepository repository;
    private WorkspaceProjectService service;
    private final List<Object> events = new ArrayList<>();

    @BeforeEach
    void init() throws IOException {
        designRepositoryAclService = mock(RepositoryAclService.class);
        aclProjectsHelper = mock(AclProjectsHelper.class);
        userWorkspace = mock(UserWorkspace.class);
        lockEngine = mock(LockEngine.class);
        when(userWorkspace.getProjectsLockEngine()).thenReturn(lockEngine);
        lenient().when(userWorkspace.getUser())
                .thenReturn(new WorkspaceUserImpl("userB", id -> null));

        repository = mock(BranchRepository.class);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.listBranches()).thenReturn(List.of("main", BRANCH));
        when(repository.getId()).thenReturn("design");
        lenient().when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshRepository("design"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);

        project = mock(RulesProject.class);
        when(project.isSupportsBranches()).thenReturn(true);
        lenient().when(project.getRepository()).thenReturn(repository);
        lenient().when(project.getBusinessName()).thenReturn("P1");
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn("DESIGN/rules/P1");
        when(project.getBranch()).thenReturn("main");
        when(project.getArtefacts()).thenAnswer(invocation -> List.of(mock(AProjectArtefact.class)));
        when(designRepositoryAclService.isGranted(any(AProjectArtefact.class), anyList())).thenReturn(true);

        service = new WorkspaceProjectService(designRepositoryAclService,
                mock(ProjectStateValidator.class),
                mock(ProjectDependencyResolver.class),
                mock(SummaryTableReader.class),
                mock(RawTableReader.class),
                mock(TableEditorsReader.class),
                List.of(),
                mock(Function.class),
                mock(BeanValidationProvider.class),
                mock(TableCreatorService.class),
                mock(TableCopyService.class),
                mock(TablePropertiesService.class),
                mock(TableRunStateService.class),
                mock(TableDetailsService.class),
                new TableVersionService(),
                mock(ProjectMetadataService.class),
                mock(TableWriterExecutor.class),
                mock(TableWritersFactory.class),
                mock(SystemPropertiesService.class),
                events::add,
                new ProjectAuditor(events::add),
                mock(ProtectedBranchBypassService.class),
                mock(DetailedMessageDescriptionMapper.class),
                mock(LocalWorkspaceManager.class),
                mock(MultiUserWorkspaceManager.class),
                aclProjectsHelper,
                mock(ProjectAccessService.class),
                mock(ProjectStatusMapper.class),
                mock(Environment.class),
                mock(ProjectTagsCache.class),
                new ProjectListingContext(),
                mock(ModuleCompilationLauncher.class),
                () -> userWorkspace, mock(UploadFormatting.class));
    }

    @Test
    void branchLockedByAnotherUserIsNotDeleted() throws IOException {
        lockedBy("userA");

        assertThrows(ConflictException.class, () -> service.deleteBranch(project, BRANCH, false));

        verify(repository, never()).deleteRepositoryBranch(anyString());
    }

    @Test
    void lockOwnerDeletesTheBranch() throws IOException {
        lockedBy("userB");

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteRepositoryBranch(BRANCH);
    }

    @Test
    void unlockedBranchIsDeleted() throws IOException {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteRepositoryBranch(BRANCH);
    }

    @Test
    void branchIsDeletedThroughTheDecoratedRepository() throws IOException {
        var decorated = mock(BranchRepository.class, withSettings().extraInterfaces(RepositoryDelegate.class));
        when(((RepositoryDelegate) decorated).getOriginal()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(decorated);
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(decorated).deleteRepositoryBranch(BRANCH);
        verify(repository, never()).deleteRepositoryBranch(BRANCH);
    }

    @Test
    void deniedBranchDeletionIsReportedAsForbidden() throws IOException {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        doThrow(new AccessDeniedException(BRANCH)).when(repository).deleteRepositoryBranch(BRANCH);

        assertThrows(ForbiddenException.class, () -> service.deleteBranch(project, BRANCH, false));
    }

    @Test
    void aProtectedBranchDeletedRecordsTheBypassAfterTheDeletion() {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        when(repository.isBranchProtected(BRANCH)).thenReturn(true);

        service.deleteBranch(project, BRANCH, true);

        assertEquals(List.of(AuditAction.BRANCH_DELETED, AuditAction.PROTECTION_BYPASSED), auditActions());
    }

    @Test
    void aProtectedBranchThatFailedToBeDeletedRecordsNoBypass() throws IOException {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        when(repository.isBranchProtected(BRANCH)).thenReturn(true);
        doThrow(new AccessDeniedException(BRANCH)).when(repository).deleteRepositoryBranch(BRANCH);

        assertThrows(ForbiddenException.class, () -> service.deleteBranch(project, BRANCH, true));

        assertEquals(List.of(), auditActions());
    }

    private List<AuditAction> auditActions() {
        return events.stream()
                .filter(AuditEvent.class::isInstance)
                .map(event -> ((AuditEvent) event).action())
                .toList();
    }

    @Test
    void theOnlyBranchHoldingTheProjectNeedsThePermissionToDeleteTheProject() throws IOException {
        heldOnlyBy(BRANCH);

        // Deleting it deletes the project, so managing branches alone is not enough.
        assertThrows(ForbiddenException.class, () -> service.deleteBranch(project, BRANCH, false));

        verify(repository, never()).deleteRepositoryBranch(anyString());
    }

    @Test
    void theOnlyBranchHoldingTheProjectIsDeletedByWhoMayDeleteTheProject() throws IOException {
        heldOnlyBy(BRANCH);
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteRepositoryBranch(BRANCH);
    }

    @Test
    void unindexedProjectStillNeedsThePermissionToDeleteTheProject() throws IOException {
        // The index lists no holders while it has no entry for the project. That silence must not read as
        // "another branch holds it": the branch may keep the last copy, so the deletion stays permission-gated.
        when(userWorkspace.getDesignTimeRepository().isLastProjectBranch("design", "P1", BRANCH)).thenReturn(true);
        when(project.getDesignProjectName()).thenReturn("P1");

        assertThrows(ForbiddenException.class, () -> service.deleteBranch(project, BRANCH, false));

        verify(repository, never()).deleteRepositoryBranch(anyString());
    }

    @Test
    void theAccessOfAProjectTheDeletedBranchHeldAloneGoesWithIt() {
        heldOnlyBy(BRANCH);
        allowedToDeleteAnUnlockedProject();
        when(userWorkspace.getDesignTimeRepository().getProjectsInAllBranches("design"))
                .thenReturn(new RepositoryProjects(List.of(), true));
        var jdoe = new PrincipalSid("jdoe");
        when(designRepositoryAclService.listPermissions("design", "/DESIGN/rules/P1"))
                .thenReturn(Map.of(jdoe, List.of(BasePermission.READ)));

        service.deleteBranch(project, BRANCH, false);

        verify(designRepositoryAclService).deleteAcl("design", "/DESIGN/rules/P1");
        assertEquals(List.of(AuditAction.BRANCH_DELETED, AuditAction.ROLE_REVOKED), auditActions());
    }

    @Test
    void theAccessOfAFolderAnotherBranchHoldsStays() {
        heldOnlyBy(BRANCH);
        allowedToDeleteAnUnlockedProject();
        var survivor = mock(AProject.class);
        var survivorData = new FileData();
        survivorData.setName("DESIGN/rules/P1");
        when(survivor.getFileData()).thenReturn(survivorData);
        when(userWorkspace.getDesignTimeRepository().getProjectsInAllBranches("design"))
                .thenReturn(new RepositoryProjects(List.of(survivor), true));

        service.deleteBranch(project, BRANCH, false);

        verify(designRepositoryAclService, never()).deleteAcl(anyString(), anyString());
        assertEquals(List.of(AuditAction.BRANCH_DELETED), auditActions());
    }

    @Test
    void theAccessStaysWhileTheRepositoryIsNotIndexedWhole() {
        heldOnlyBy(BRANCH);
        allowedToDeleteAnUnlockedProject();
        when(userWorkspace.getDesignTimeRepository().getProjectsInAllBranches("design"))
                .thenReturn(RepositoryProjects.UNKNOWN);

        service.deleteBranch(project, BRANCH, false);

        verify(designRepositoryAclService, never()).deleteAcl(anyString(), anyString());
    }

    @Test
    void aFailureToRemoveTheAccessFailsNoBranchDeletion() throws IOException {
        heldOnlyBy(BRANCH);
        allowedToDeleteAnUnlockedProject();
        when(userWorkspace.getDesignTimeRepository().getProjectsInAllBranches("design"))
                .thenReturn(new RepositoryProjects(List.of(), true));
        doThrow(new IllegalStateException("database unavailable"))
                .when(designRepositoryAclService).deleteAcl("design", "/DESIGN/rules/P1");

        // The branch is gone already: the next project created in the folder replaces what is left there.
        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteRepositoryBranch(BRANCH);
        verify(userWorkspace).refresh();
    }

    @Test
    void theAccessOfAProjectOtherBranchesHoldIsNotTouched() {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);

        service.deleteBranch(project, BRANCH, false);

        verify(userWorkspace.getDesignTimeRepository(), never()).getProjectsInAllBranches(anyString());
        verify(designRepositoryAclService, never()).deleteAcl(anyString(), anyString());
    }

    private void allowedToDeleteAnUnlockedProject() {
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        when(designRepositoryAclService.getPath(project)).thenReturn("/DESIGN/rules/P1");
    }

    /** The index reports a project that no branch other than this one holds. */
    private void heldOnlyBy(String branch) {
        when(userWorkspace.getDesignTimeRepository().getProjectsHeldOnlyBy("design", branch))
                .thenReturn(List.of(project));
    }

    @Test
    void baseBranchCannotBeDeletedWithDifferentCasing() throws IOException {
        assertThrows(ConflictException.class, () -> service.deleteBranch(project, "MAIN", false));

        verify(repository, never()).deleteRepositoryBranch(anyString());
    }

    private void lockedBy(String userName) {
        LockInfo lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockInfo.getLockedBy()).thenReturn(userName);
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(lockInfo);
    }

}
