package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.repositories.model.ProjectRevision;

@RequiredArgsConstructor
@Service
public class ProjectRevisionServiceImpl implements ProjectRevisionService {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    @Lookup
    protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
        return null;
    }

    @Override
    public PageResponse<ProjectRevision> getProjectRevision(Repository repository,
                                                            String projectName,
                                                            String branch,
                                                            String searchTerm,
                                                            boolean techRevs,
                                                            Pageable page) throws IOException, ProjectException {
        // Checkout branch if specified
        if (branch != null && !branch.isEmpty()) {
            repository = checkoutBranchIfPresent(repository, branch);
        }

        // Get project and verify it exists
        AProject project;
        try {
            project = designTimeRepository.getProject(repository.getId(), projectName);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", projectName);
        }

        // Check read permission
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new ForbiddenException();
        }

        // Get full path based on repository features
        String fullPath;
        if (repository.supports().mappedFolders()) {
            fullPath = mappedPathIn(repository, project);
            if (fullPath == null) {
                // The branch being read does not hold the folder, so it has no history of it to report.
                return PageResponse.of(List.of(), page, 0L);
            }
        } else {
            fullPath = designTimeRepository.getRulesLocation() + projectName;
        }

        // Retrieve and return project history
        return getHistoryRepositoryMapper(repository).getProjectHistory(fullPath, searchTerm, techRevs, page);
    }

    @Override
    public PageResponse<ProjectRevision> getProjectRevision(RulesProject project,
                                                            String searchTerm,
                                                            boolean techRevs,
                                                            Pageable page) throws IOException {
        if (project.isLocalOnly()) {
            // Never published, so no repository holds a history of it.
            return PageResponse.of(List.of(), page, 0L);
        }
        // The project carries the folder its repository holds it under, which is the folder its own history
        // is read from. No name is resolved, so a rename that is not saved yet changes nothing here.
        return getHistoryRepositoryMapper(project.getDesignRepository())
                .getProjectHistory(project.getDesignFolderName(), searchTerm, techRevs, page);
    }

    /**
     * The path the given repository view knows the project by, or {@code null} when that branch does not hold it.
     *
     * <p>A mapped repository names its folders per branch, so the path a project is listed under is not the path
     * the branch being read uses for it. The folder is the same, so it is translated back through the mapping of
     * that branch.
     */
    private static @Nullable String mappedPathIn(Repository repository, AProject project) {
        var listedPath = project.getFolderPath();
        var listedIn = project.getRepository();
        if (!listedIn.supports().mappedFolders()) {
            return listedPath;
        }
        var internalPath = ((FolderMapper) listedIn).getRealPath(listedPath);
        return ((FolderMapper) repository).findMappedName(internalPath);
    }

    private Repository checkoutBranchIfPresent(Repository repository, String branch) throws IOException {
        if (!repository.supports().branches()) {
            throw new NotFoundException("repository.branch.message");
        }
        branch = branch.replace(' ', '/');
        var branchRepo = ((BranchRepository) repository);
        if (!branchRepo.branchExists(branch)) {
            throw new NotFoundException("repository.branch.message");
        }
        return branchRepo.forBranch(branch);
    }
}
