package org.openl.rules.webstudio.repositories.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.SecurityException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.openl.rules.rest.service.HistoryRepositoryMapper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;

@Service
public class ProjectRevisionServiceImpl implements ProjectRevisionService {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    public ProjectRevisionServiceImpl(DesignTimeRepository designTimeRepository,
                                      RepositoryAclService designRepositoryAclService) {
        this.designTimeRepository = designTimeRepository;
        this.designRepositoryAclService = designRepositoryAclService;
    }

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
            throw new SecurityException();
        }

        // Get full path based on repository features
        String fullPath;
        if (repository.supports().mappedFolders()) {
            fullPath = designTimeRepository.getProject(repository.getId(), projectName).getFolderPath();
        } else {
            fullPath = designTimeRepository.getRulesLocation() + projectName;
        }

        // Retrieve and return project history
        return getHistoryRepositoryMapper(repository).getProjectHistory(fullPath, searchTerm, techRevs, page);
    }

    private Repository checkoutBranchIfPresent(Repository repository, String branch) throws IOException {
        if (!repository.supports().branches()) {
            throw new NotFoundException("repository.branch.message");
        }
        branch = branch.replace(' ', '/');
        BranchRepository branchRepo = ((BranchRepository) repository);
        if (!branchRepo.branchExists(branch)) {
            throw new NotFoundException("repository.branch.message");
        }
        return branchRepo.forBranch(branch);
    }
}
