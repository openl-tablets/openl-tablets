package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.BranchRepository;

class SecureBranchRepositoryTest {

    private static final String REPOSITORY_ID = "design";

    private BranchRepository delegate;
    private SimpleRepositoryAclService aclService;
    private SecureBranchRepository repository;

    @BeforeEach
    void setUp() {
        delegate = mock(BranchRepository.class);
        aclService = mock(SimpleRepositoryAclService.class);
        when(delegate.getId()).thenReturn(REPOSITORY_ID);
        repository = new SecureBranchRepository(delegate, aclService);
    }

    @Test
    void listsRepositoryBranchesWithReadPermission() throws Exception {
        var branches = List.of("main", "feature");
        when(aclService.isGranted(REPOSITORY_ID, null, List.of(BasePermission.READ))).thenReturn(true);
        when(delegate.listBranches()).thenReturn(branches);

        assertEquals(branches, repository.listBranches());
        verify(delegate).listBranches();
    }

    @Test
    void hidesRepositoryBranchesWithoutReadPermission() throws Exception {
        assertEquals(List.of(), repository.listBranches());
        verify(delegate, never()).listBranches();
    }
}
