package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchTreeRevision;

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

    @Test
    void readsTreeRevisionsWithRepositoryReadPermission() throws Exception {
        var branches = List.of("main");
        var revisions = Map.of("main", new BranchTreeRevision("commit", "tree"));
        when(aclService.isGranted(REPOSITORY_ID, null, List.of(BasePermission.READ))).thenReturn(true);
        when(delegate.getBranchTreeRevisions(branches, "")).thenReturn(revisions);

        assertEquals(revisions, repository.getBranchTreeRevisions(branches, ""));
    }

    @Test
    void createsAndDeletesRepositoryBranchesWithWritePermission() throws Exception {
        when(aclService.isGranted(REPOSITORY_ID, null, List.of(BasePermission.WRITE))).thenReturn(true);

        repository.createRepositoryBranch("feature", "main");
        repository.deleteRepositoryBranch("feature");

        verify(delegate).createRepositoryBranch("feature", "main");
        verify(delegate).deleteRepositoryBranch("feature");
    }

    @Test
    void rejectsRepositoryBranchChangesWithoutWritePermission() throws Exception {
        assertThrows(AccessDeniedException.class, () -> repository.createRepositoryBranch("feature", "main"));
        assertThrows(AccessDeniedException.class, () -> repository.deleteRepositoryBranch("feature"));
        verify(delegate, never()).createRepositoryBranch("feature", "main");
        verify(delegate, never()).deleteRepositoryBranch("feature");
    }
}
