package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.tags.service.TagAssignmentValidator;

/**
 * A branch on which the project is locked by another user must not be deleted: the lock means the
 * user is editing the project there. The lock owner keeps the right to delete the branch.
 *
 * @author Yury Molchan
 */
class WorkspaceProjectServiceDeleteBranchTest {

    private static final String BRANCH = "user-a";

    private RepositoryAclService designRepositoryAclService;
    private UserWorkspace userWorkspace;
    private LockEngine lockEngine;
    private RulesProject project;
    private BranchRepository repository;
    private WorkspaceProjectService service;

    @BeforeEach
    void init() throws IOException {
        designRepositoryAclService = mock(RepositoryAclService.class);
        userWorkspace = mock(UserWorkspace.class);
        lockEngine = mock(LockEngine.class);
        when(userWorkspace.getProjectsLockEngine()).thenReturn(lockEngine);
        lenient().when(userWorkspace.getUser())
                .thenReturn(new WorkspaceUserImpl("userB", id -> null));

        repository = mock(BranchRepository.class);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.branchExists(BRANCH)).thenReturn(true);
        when(repository.getId()).thenReturn("design");

        project = mock(RulesProject.class);
        when(project.isSupportsBranches()).thenReturn(true);
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
                List.of(),
                mock(Function.class),
                mock(BeanValidationProvider.class),
                mock(TableCreatorService.class),
                mock(TableWriterExecutor.class),
                mock(TableWritersFactory.class),
                mock(ApplicationEventPublisher.class),
                mock(ProtectedBranchBypassService.class),
                mock(ProjectIdentifierMapper.class),
                mock(DetailedMessageDescriptionMapper.class),
                mock(LocalWorkspaceManager.class),
                mock(MultiUserWorkspaceManager.class),
                mock(AclProjectsHelper.class),
                mock(ProjectAccessService.class),
                mock(ProjectStatusMapper.class),
                mock(Environment.class),
                mock(TagAssignmentValidator.class),
                mock(ProjectTagsCache.class)) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return userWorkspace;
            }
        };
    }

    @Test
    void branchLockedByAnotherUserIsNotDeleted() throws IOException {
        lockedBy("userA");

        assertThrows(ConflictException.class, () -> service.deleteBranch(project, BRANCH, false));

        verify(repository, never()).deleteBranch(any(), anyString());
    }

    @Test
    void lockOwnerDeletesTheBranch() throws IOException {
        lockedBy("userB");

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteBranch(null, BRANCH);
    }

    @Test
    void unlockedBranchIsDeleted() throws IOException {
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);

        assertDoesNotThrow(() -> service.deleteBranch(project, BRANCH, false));

        verify(repository).deleteBranch(null, BRANCH);
    }

    private void lockedBy(String userName) {
        LockInfo lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockInfo.getLockedBy()).thenReturn(userName);
        when(lockEngine.getLockInfo("design", BRANCH, "DESIGN/rules/P1")).thenReturn(lockInfo);
    }
}
