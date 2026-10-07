package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * Unit tests for {@link ProjectIndex}.
 *
 * @author Yury Molchan
 */
class ProjectIndexTest {

    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);

    @AfterEach
    void clearInterrupt() {
        // A test that interrupts the thread must not leave the flag to the next one.
        Thread.interrupted();
    }

    @Test
    void repositoryWithoutBranchesIsRefreshedWithoutWaiting() {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        assertTrue(ProjectIndex.awaitBranch(designTimeRepository, repository, null));

        verify(designTimeRepository).refresh();
        verify(designTimeRepository, never()).refreshBranch(anyString(), anyString());
    }

    @Test
    void branchPublishedByTheIndexIsReported() {
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertTrue(ProjectIndex.awaitBranch(designTimeRepository, branchRepository(), "feature"));

        verify(designTimeRepository).refreshBranch("design", "feature");
    }

    @Test
    void branchTheIndexFailedToPublishIsReported() {
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("The index is down")));

        assertFalse(ProjectIndex.awaitBranch(designTimeRepository, branchRepository(), "feature"));
    }

    @Test
    void interruptedWaitIsReportedAndKeepsTheInterrupt() {
        when(designTimeRepository.refreshBranch("design", "feature")).thenReturn(new CompletableFuture<>());
        Thread.currentThread().interrupt();

        assertFalse(ProjectIndex.awaitBranch(designTimeRepository, branchRepository(), "feature"));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void repositoryPublishedByTheIndexIsReported() {
        when(designTimeRepository.refreshRepository("design")).thenReturn(CompletableFuture.completedFuture(null));

        assertTrue(ProjectIndex.awaitRepository(designTimeRepository, branchRepository()));
    }

    @Test
    void repositoryTheIndexFailedToPublishIsReported() {
        when(designTimeRepository.refreshRepository("design"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("The index is down")));

        assertFalse(ProjectIndex.awaitRepository(designTimeRepository, branchRepository()));
    }

    private static BranchRepository branchRepository() {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        return repository;
    }
}
