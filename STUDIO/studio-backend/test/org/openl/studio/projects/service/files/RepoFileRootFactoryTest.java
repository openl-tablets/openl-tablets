package org.openl.studio.projects.service.files;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * A mount built over a design repository waits for the project index of the user's design repository, on the
 * branch the mount writes to. A mount of a repository without branches waits for nothing (EPBDS-16088).
 *
 * @author Yury Molchan
 */
class RepoFileRootFactoryTest {

    private BranchRepository repository;
    private DesignTimeRepository designTimeRepository;
    private RepoFileRootFactory factory;

    @BeforeEach
    void init() {
        repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("main");
        designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch("design", "main")).thenReturn(CompletableFuture.completedFuture(null));
        var userWorkspace = mock(UserWorkspace.class, RETURNS_DEEP_STUBS);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        factory = new RepoFileRootFactory(mock(AclProjectsHelper.class), mock(UserManagementService.class),
                mock(ProjectFileLookupService.class)) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return userWorkspace;
            }
        };
    }

    @Test
    void mountWaitsForTheIndexOfItsBranch() {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());

        factory.of(repository, null).awaitIndex();

        verify(designTimeRepository).refreshBranch("design", "main");
    }

    @Test
    void mountOfRepositoryWithoutBranchesWaitsForNothing() {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(false).build());

        factory.of(repository, null).awaitIndex();

        verifyNoInteractions(designTimeRepository);
    }
}
