package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
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

        assertSame(repository, resolver.resolve(repository, "MASTER"));

        verify(repository, never()).branchExists("MASTER");
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
    void existingBranchUsesItsCanonicalName() throws Exception {
        var repository = branchRepository();
        var target = mock(BranchRepository.class);
        when(repository.branchExists("FEATURE/RATES")).thenReturn(true);
        when(repository.listBranches()).thenReturn(List.of("feature/rates"));
        when(repository.forBranch("feature/rates")).thenReturn(target);

        assertSame(target, resolver.resolve(repository, "FEATURE/RATES"));

        verify(repository).forBranch("feature/rates");
        verify(repository, never()).forBranch("FEATURE/RATES");
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
    void absentBranchIsCreatedThroughTheDecoratedRepository() throws Exception {
        var rawRepository = branchRepository();
        var repository = mock(BranchRepository.class, withSettings().extraInterfaces(RepositoryDelegate.class));
        var target = mock(BranchRepository.class);
        var validator = mock(NewBranchValidator.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        when(((RepositoryDelegate) repository).getOriginal()).thenReturn(rawRepository);
        when(rawRepository.branchExists("feature/rates")).thenReturn(false);
        when(rawRepository.listBranches()).thenReturn(List.of("main"));
        when(rawRepository.getBaseBranch()).thenReturn("main");
        when(repository.forBranch("feature/rates")).thenReturn(target);
        when(validatorFactory.apply(rawRepository)).thenReturn(validator);

        assertSame(target, resolver.resolve(repository, "feature/rates"));

        verify(repository).createRepositoryBranch("feature/rates", "main");
        verify(rawRepository, never()).createRepositoryBranch("feature/rates", "main");
    }

    @Test
    void deniedBranchCreationIsReportedAsForbidden() throws Exception {
        var repository = branchRepository();
        var validator = mock(NewBranchValidator.class);
        when(repository.branchExists("feature/rates")).thenReturn(false);
        when(repository.listBranches()).thenReturn(List.of("main"));
        when(repository.getBaseBranch()).thenReturn("main");
        when(validatorFactory.apply(repository)).thenReturn(validator);
        doThrow(new AccessDeniedException("feature/rates"))
                .when(repository)
                .createRepositoryBranch("feature/rates", "main");

        assertThrows(ForbiddenException.class, () -> resolver.resolve(repository, "feature/rates"));
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
