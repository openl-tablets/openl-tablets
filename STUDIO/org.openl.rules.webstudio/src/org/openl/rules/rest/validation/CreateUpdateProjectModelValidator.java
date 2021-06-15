package org.openl.rules.rest.validation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.inject.Inject;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class CreateUpdateProjectModelValidator implements Validator {

    private final DesignTimeRepository designTimeRepository;

    @Inject
    public CreateUpdateProjectModelValidator(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass == CreateUpdateProjectModel.class;
    }

    @Override
    public void validate(Object o, Errors errors) {
        CreateUpdateProjectModel model = (CreateUpdateProjectModel) o;
        if (model.isOverwrite() && designTimeRepository.hasProject(model.getRepoName(), model.getProjectName())) {
            validateProjectUpdate(model);
        } else {
            validateProjectCreation(model);
        }

        Repository repository = designTimeRepository.getRepository(model.getRepoName());
        if (!repository.supports().mappedFolders()) {
            if (StringUtils.isNotBlank(model.getPath())) {
                errors.rejectValue("path", "repo.not-supported.path.message");
            }
        }

        try {
            CommentValidator.forRepo(model.getRepoName()).validate(model.getComment());
        } catch (Exception e) {
            errors.rejectValue("comment",
                "repo.invalid.comment.message",
                new String[] { e.getMessage() },
                e.getMessage());
        }
    }

    private void validateProjectUpdate(CreateUpdateProjectModel model) {
        Repository repository = designTimeRepository.getRepository(model.getRepoName());
        if (repository.supports().mappedFolders()) {
            try {
                AProject project = designTimeRepository.getProject(model.getRepoName(), model.getProjectName());
                if (!Objects.equals(project.getRealPath(), model.getPath() + model.getProjectName())) {
                    throw new NotFoundException("project.message",
                        new String[] { model.getProjectName() });
                }
                if (project.isDeleted()) {
                    throw new ConflictException("project.archived.message",
                        new String[] { model.getProjectName() });
                }
            } catch (ProjectException e) {
                throw new NotFoundException("project.message",
                        new String[] { model.getProjectName() });
            }
        }
    }

    private void validateProjectCreation(CreateUpdateProjectModel model) {
        if (designTimeRepository.hasProject(model.getRepoName(), model.getProjectName())) {
            throw new ConflictException("duplicated.project.message");
        } else {
            Repository repository = designTimeRepository.getRepository(model.getRepoName());
            if (repository.supports().mappedFolders()) {
                String projectPath = StringUtils.isEmpty(model.getPath()) ? model.getProjectName()
                                                                          : model.getPath() + model.getProjectName();
                try {
                    if (((FolderMapper) repository).getDelegate().check(projectPath) != null) {
                        throw new ConflictException("duplicated.project.1.message");
                    } else {
                        final Path currentPath = Paths.get(projectPath);
                        if (designTimeRepository.getProjects(model.getRepoName())
                            .stream()
                            .map(AProjectFolder::getRealPath)
                            .map(Paths::get)
                            .anyMatch(path -> path.startsWith(currentPath) || currentPath.startsWith(path))) {
                            throw new ConflictException("duplicated.project.2.message");
                        }
                    }
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
            }
        }
    }
}
