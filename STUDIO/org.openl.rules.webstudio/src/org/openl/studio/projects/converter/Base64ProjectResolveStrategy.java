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
 * Resolves a project by its base64-encoded ID ({@code repositoryId:projectName}). For repositories that support mapped
 * folders, retries the lookup with the business name when the direct lookup fails.
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
            return List.of(workspace.getProject(repoId, projectName));
        } catch (ProjectException e) {
            return mappedFolderFallback(workspace, repoId, projectName);
        }
    }

    private List<RulesProject> mappedFolderFallback(UserWorkspace workspace, String repoId, String projectName) {
        var repository = workspace.getDesignTimeRepository().getRepository(repoId);
        if (repository == null || !repository.supports().mappedFolders()) {
            return List.of();
        }
        var businessName = ((FolderMapper) repository).getBusinessName(projectName);
        if (Objects.equals(businessName, projectName)) {
            return List.of();
        }
        try {
            return List.of(workspace.getProject(repoId, businessName));
        } catch (ProjectException e) {
            return List.of();
        }
    }
}
