package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.function.Function;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.validator.NewBranchValidator;
import org.openl.util.StringUtils;

/**
 * Resolves the repository view used to create a project.
 *
 * <p>An existing branch is selected directly. In an empty repository, the requested branch is prepared for its
 * first commit. Otherwise, an absent branch is validated and created from the repository base branch. Omitting the
 * branch preserves the repository's configured view.
 */
@Service
@RequiredArgsConstructor
public class ProjectCreationTargetResolver {

    private final BeanValidationProvider validationProvider;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;

    public Repository resolve(Repository repository, @Nullable String requestedBranch) {
        return resolve(repository, requestedBranch, true);
    }

    public Repository resolve(Repository repository, @Nullable String requestedBranch, boolean allowBranchCreation) {
        var branch = StringUtils.trimToNull(requestedBranch);
        if (branch == null) {
            return repository;
        }
        if (!repository.supports().branches()) {
            throw new ConflictException("repository.branch.unsupported.message");
        }

        var branchRepository = (BranchRepository) repository;
        var rawRepository = unwrap(branchRepository);
        if (branch.equals(rawRepository.getBaseBranch())) {
            return repository;
        }
        try {
            if (!rawRepository.branchExists(branch)) {
                if (!allowBranchCreation) {
                    throw new ConflictException("repository.branch.message");
                }
                validationProvider.validate(branch, newBranchValidatorFactory.apply(rawRepository));
                if (!rawRepository.listBranches().isEmpty()) {
                    rawRepository.createRepositoryBranch(branch, rawRepository.getBaseBranch());
                }
            }
            return branchRepository.forBranch(branch);
        } catch (IOException e) {
            throw new ConflictException("project.create.branch.failed.message", branch);
        }
    }

    private static BranchRepository unwrap(BranchRepository repository) {
        Repository current = repository;
        while (current instanceof RepositoryDelegate delegate) {
            current = delegate.getOriginal();
        }
        return (BranchRepository) current;
    }
}
