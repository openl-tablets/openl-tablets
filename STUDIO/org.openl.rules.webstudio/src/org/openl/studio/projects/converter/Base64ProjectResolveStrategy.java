package org.openl.studio.projects.converter;

import java.util.List;
import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Resolves a project by its base64-encoded ID ({@code repositoryId:projectName}).
 *
 * <p>A cached workspace project is reused only while the current secured design-repository view still contains it and
 * its opened state matches the authoritative local metainfo registry. A cache miss or stale entry refreshes the
 * workspace.
 *
 * <p>The id names a project by its design folder, while the workspace indexes an opened project by the folder its
 * copy occupies. The two agree until the project is renamed in {@code rules.xml}: until that rename is saved, the
 * design folder keeps the old name and the workspace copy already carries the new one. For a mapped repository the
 * folder the id names is therefore looked up directly, and only then, as before, by the business name.
 *
 * @author Vladyslav Pikus
 */
@Component
@Order(1)
public class Base64ProjectResolveStrategy implements ProjectResolveStrategy {

    @Override
    public List<RulesProject> resolve(UserWorkspace workspace, String identity) {
        ProjectIdModel projectId;
        try {
            projectId = ProjectIdModel.decode(identity);
        } catch (Exception e) {
            return List.of();
        }
        var repoId = projectId.getRepository();
        var projectName = projectId.getProjectName();
        try {
            return List.of(resolveProject(workspace, repoId, projectName));
        } catch (ProjectException e) {
            return mappedFolderFallback(workspace, repoId, projectName);
        }
    }

    /**
     * The project the id names in a mapped repository.
     *
     * <p>A name the mapper leaves untouched is not a mapped name and names no folder here. Otherwise the folder the
     * id names is matched first, because it identifies exactly one project. The business name is only tried
     * afterwards: more than one project can carry it, so it may answer with a different project that currently
     * occupies the name.
     */
    private List<RulesProject> mappedFolderFallback(UserWorkspace workspace, String repoId, String projectName) {
        var designTimeRepository = workspace.getDesignTimeRepository();
        var repository = designTimeRepository.getRepository(repoId);
        if (repository == null || !repository.supports().mappedFolders()) {
            return List.of();
        }
        var folderMapper = (FolderMapper) repository;
        var businessName = folderMapper.getBusinessName(projectName);
        if (Objects.equals(businessName, projectName)) {
            return List.of();
        }
        // The mapper reads a whole external path, while an id carries only its last segment.
        var externalPath = designTimeRepository.getRulesLocation() + projectName;
        var byDesignFolder = workspace.getProjectByPath(repoId, folderMapper.getRealPath(externalPath));
        if (byDesignFolder.isPresent()) {
            return List.of(byDesignFolder.get());
        }
        try {
            return List.of(resolveProject(workspace, repoId, businessName));
        } catch (ProjectException e) {
            return List.of();
        }
    }

    private RulesProject resolveProject(UserWorkspace workspace,
                                        String repositoryId,
                                        String projectName) throws ProjectException {
        try {
            var cachedProject = workspace.getProject(repositoryId, projectName, false);
            if (cachedProject != null && isCurrent(workspace, repositoryId, cachedProject)) {
                return cachedProject;
            }
        } catch (ProjectException ignored) {
            // A newly granted project is not in the user cache yet. Refresh it below.
        }
        return workspace.getProject(repositoryId, projectName);
    }

    private boolean isCurrent(UserWorkspace workspace, String repositoryId, RulesProject project) {
        if (project.isLocalOnly()) {
            return true;
        }
        var projectName = project.getDesignProjectName();
        if (!workspace.getDesignTimeRepository().hasProject(repositoryId, projectName)) {
            return false;
        }
        var metainfo = workspace.getLocalWorkspace().getMetainfoRegistry().get(project.getBusinessName());
        var isOpened = metainfo != null && repositoryId.equals(metainfo.repositoryId());
        return project.isOpened() == isOpened;
    }
}
