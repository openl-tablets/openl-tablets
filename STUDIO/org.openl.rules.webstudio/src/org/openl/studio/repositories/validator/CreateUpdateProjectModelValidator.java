package org.openl.studio.repositories.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.validator.NewBranchValidator;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CreateUpdateProjectModelValidator implements Validator {

    private final DesignTimeRepository designTimeRepository;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass == CreateUpdateProjectModel.class;
    }

    @Override
    public void validate(Object o, Errors errors) {
        var model = (CreateUpdateProjectModel) o;
        getTargetRepository(model, errors).ifPresent(repository -> {
            if (model.isOverwrite() && designTimeRepository.hasProject(model.getRepoName(), model.getProjectName())) {
                validateProjectUpdate(model, repository);
            } else {
                validateProjectCreation(model, repository);
            }

            if (!repository.supports().mappedFolders() && StringUtils.isNotBlank(model.getPath())) {
                errors.rejectValue("path", "repo.not-supported.path.message");
            }
        });

        try {
            CommentValidator.forRepo(model.getRepoName()).validate(model.getComment());
        } catch (Exception e) {
            errors.rejectValue("comment",
                    "repo.invalid.comment.message",
                    new String[]{e.getMessage()},
                    e.getMessage());
        }
    }

    private void validateProjectUpdate(CreateUpdateProjectModel model, Repository repository) {
        if (repository.supports().mappedFolders()) {
            try {
                var project = designTimeRepository.getProject(model.getRepoName(), model.getProjectName());
                if (!Objects.equals(project.getRealPath(), model.getFullPath())) {
                    throw new NotFoundException("project.message", model.getProjectName());
                }
                if (project.isDeleted()) {
                    throw new ConflictException("project.deleted.message", model.getProjectName());
                }
            } catch (ProjectException e) {
                throw new NotFoundException("project.message", model.getProjectName());
            }
        }
    }

    private void validateProjectCreation(CreateUpdateProjectModel model, Repository repository) {
        if (designTimeRepository.hasProjectInAnyBranch(model.getRepoName(), model.getProjectName())) {
            throw new ConflictException("duplicated.project.message");
        }
        if (repository.supports().mappedFolders()) {
            try {
                var fileData = ((FolderMapper) repository).getDelegate().check(model.getFullPath());
                if (fileData != null && !fileData.isDeleted()) {
                    throw new ConflictException("duplicated.project.1.message");
                } else {
                    final Path currentPath = Path.of(model.getFullPath());
                    if (designTimeRepository.getProjects(model.getRepoName())
                            .stream()
                            .map(AProjectFolder::getRealPath)
                            .map(Path::of)
                            .anyMatch(path -> path.startsWith(currentPath) || currentPath.startsWith(path))) {
                        throw new ConflictException("duplicated.project.2.message");
                    }
                }
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    private Optional<Repository> getTargetRepository(CreateUpdateProjectModel model, Errors errors) {
        var repository = designTimeRepository.getRepository(model.getRepoName());
        if (StringUtils.isBlank(model.getBranch()) || !repository.supports().branches()) {
            return Optional.of(repository);
        }
        var branchRepository = (BranchRepository) repository;
        var branch = model.getBranch();
        var rawRepository = unwrap(branchRepository);
        if (branch.equals(rawRepository.getBaseBranch())) {
            return Optional.of(repository);
        }
        if (!rawRepository.isValidBranchName(branch)) {
            errors.rejectValue("branch", "branch.name.invalid.4.message");
            return Optional.empty();
        }
        try {
            if (rawRepository.branchExists(branch)) {
                return Optional.of(branchRepository.forBranch(branch));
            }
            if (!validateNewBranch(rawRepository, branch, errors)) {
                return Optional.empty();
            }
            return Optional.of(repository);
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private boolean validateNewBranch(BranchRepository repository, String branch, Errors errors) {
        var branchErrors = new BeanPropertyBindingResult(branch, "branch");
        newBranchValidatorFactory.apply(repository).validate(branch, branchErrors);
        branchErrors.getAllErrors().forEach(error -> errors.rejectValue("branch",
                error.getCode(),
                error.getArguments(),
                error.getDefaultMessage()));
        return !branchErrors.hasErrors();
    }

    private static BranchRepository unwrap(BranchRepository repository) {
        Repository current = repository;
        while (current instanceof RepositoryDelegate delegate) {
            current = delegate.getOriginal();
        }
        return (BranchRepository) current;
    }
}
