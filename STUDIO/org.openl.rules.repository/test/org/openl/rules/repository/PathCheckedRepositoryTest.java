package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergeScope;

class PathCheckedRepositoryTest {

    @Test
    void keepsBranchViewsPathChecked() throws Exception {
        var delegate = mock(BranchRepository.class);
        var target = mock(BranchRepository.class);
        when(delegate.isValidBranchName(anyString())).thenReturn(true);
        when(delegate.forBranch("feature")).thenReturn(target);
        var repository = new PathCheckedRepository(delegate);

        var branchView = repository.forBranch("feature");

        assertInstanceOf(PathCheckedRepository.class, branchView);
        assertThrows(InvalidPathException.class, () -> branchView.list("../outside"));
        verify(target, never()).list(anyString());
    }

    @Test
    void tellsTheScopeAndTheOutcomeOfAMergeOfValidBranchesOnly() throws Exception {
        var delegate = mock(BranchRepository.class);
        when(delegate.isValidBranchName("feature")).thenReturn(true);
        when(delegate.isValidBranchName("main")).thenReturn(true);
        var scope = new MergeScope(List.of(), Set.of());
        when(delegate.getMergeScope("feature", "main")).thenReturn(scope);
        var outcome = new MergeOutcome("before", "after", List.of(), List.of());
        when(delegate.merge("feature", null, null)).thenReturn(outcome);
        var repository = new PathCheckedRepository(delegate);

        assertSame(scope, repository.getMergeScope("feature", "main"));
        assertSame(outcome, repository.merge("feature", null, null));
        assertThrows(IllegalArgumentException.class, () -> repository.getMergeScope("../outside", "main"));
    }
}
