package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.validator.NewBranchValidator;

class ProjectCreationTargetResolverTest {

    private final BeanValidationProvider validationProvider = mock(BeanValidationProvider.class);
    @SuppressWarnings("unchecked")
    private final Function<BranchRepository, NewBranchValidator> validatorFactory = mock(Function.class);
    private final ProjectCreationTargetResolver resolver =
            new ProjectCreationTargetResolver(validationProvider, validatorFactory);

    @Test
    void omittedBranchKeepsConfiguredRepositoryView() {
        var repository = mock(Repository.class);

        assertSame(repository, resolver.resolve(repository, null));
    }

    @Test
    void configuredBaseBranchKeepsRepositoryViewBeforeItsFirstCommit() throws Exception {
        var repository = branchRepository();
        when(repository.getBaseBranch()).thenReturn("master");

        assertSame(repository, resolver.resolve(repository, "master"));

        verify(repository, never()).branchExists("master");
        verify(repository, never()).createRepositoryBranch("master", "master");
        verify(repository, never()).forBranch("master");
    }

    @Test
    void existingBranchSelectsItsRepositoryViewWithoutCreatingARef() throws Exception {
        var repository = branchRepository();
        var target = mock(BranchRepository.class);
        when(repository.branchExists("feature/rates")).thenReturn(true);
        when(repository.forBranch("feature/rates")).thenReturn(target);

        assertSame(target, resolver.resolve(repository, " feature/rates "));

        verify(repository, never()).createRepositoryBranch("feature/rates", "main");
        verify(validationProvider, never()).validate("feature/rates");
    }

    @Test
    void absentBranchIsValidatedAndCreatedFromBase() throws Exception {
        var repository = branchRepository();
        var target = mock(BranchRepository.class);
        var validator = mock(NewBranchValidator.class);
        when(repository.branchExists("feature/rates")).thenReturn(false);
        when(repository.listBranches()).thenReturn(List.of("main"));
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.forBranch("feature/rates")).thenReturn(target);
        when(validatorFactory.apply(repository)).thenReturn(validator);

        assertSame(target, resolver.resolve(repository, "feature/rates"));

        verify(validationProvider).validate("feature/rates", validator);
        verify(repository).createRepositoryBranch("feature/rates", "main");
    }

    @Test
    void absentBranchInEmptyRepositoryIsPreparedForItsFirstCommit() throws Exception {
        var repository = branchRepository();
        var target = mock(BranchRepository.class);
        var validator = mock(NewBranchValidator.class);
        when(repository.branchExists("feature/rates")).thenReturn(false);
        when(repository.listBranches()).thenReturn(List.of());
        when(repository.forBranch("feature/rates")).thenReturn(target);
        when(validatorFactory.apply(repository)).thenReturn(validator);

        assertSame(target, resolver.resolve(repository, "feature/rates"));

        verify(validationProvider).validate("feature/rates", validator);
        verify(repository, never()).createRepositoryBranch("feature/rates", "main");
    }

    @Test
    void newBranchIsRejectedWhenTheCallCannotCreateRefs() {
        var repository = branchRepository();

        assertThrows(ConflictException.class, () -> resolver.resolve(repository, "feature/rates", false));
    }

    @Test
    void branchIsRejectedForRepositoryWithoutBranchSupport() {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        assertThrows(ConflictException.class, () -> resolver.resolve(repository, "feature/rates"));
    }

    private static BranchRepository branchRepository() {
        var repository = mock(BranchRepository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        return repository;
    }
}
