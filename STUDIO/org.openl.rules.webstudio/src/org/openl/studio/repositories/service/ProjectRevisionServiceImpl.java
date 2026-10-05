package org.openl.studio.repositories.service;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.util.StringUtils;

@Service
public class ProjectRevisionServiceImpl implements ProjectRevisionService {

    @Lookup
    protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @Override
    public PageResponse<ProjectRevision> getProjectRevision(RulesProject project,
                                                            String branch,
                                                            String searchTerm,
                                                            boolean techRevs,
                                                            Pageable page) throws IOException {
        if (project.isLocalOnly()) {
            // Never published, so no repository holds a history of it.
            return PageResponse.of(List.of(), page, 0L);
        }
        if (StringUtils.isBlank(branch)) {
            // The project carries the folder its repository holds it under, which is the folder its own
            // history is read from. No name is resolved, so a rename that is not saved yet changes
            // nothing here.
            return getHistoryRepositoryMapper(project.getDesignRepository())
                    .getProjectHistory(project.getDesignFolderName(), searchTerm, techRevs, page);
        }
        var repository = checkoutBranchIfPresent(project.getDesignRepository(), branch);
        var folder = folderIn(repository, project);
        if (folder == null) {
            // The branch being read does not hold the folder, so it has no history of it to report.
            return PageResponse.of(List.of(), page, 0L);
        }
        return getHistoryRepositoryMapper(repository).getProjectHistory(folder, searchTerm, techRevs, page);
    }

    @Override
    public PageResponse<ProjectRevision> getFileRevision(RulesProject project,
                                                         String path,
                                                         String searchTerm,
                                                         boolean techRevs,
                                                         Pageable page) throws IOException {
        if (project.isLocalOnly()) {
            // Never published, so no repository holds a history of it.
            return PageResponse.of(List.of(), page, 0L);
        }
        var filePath = path.startsWith("/") ? path.substring(1) : path;
        if (filePath.isEmpty() || "/".equals(filePath)) {
            // No file named: the history asked for is the project's own, on the branch it is on.
            return getProjectRevision(project, null, searchTerm, techRevs, page);
        }
        // Reject absolute paths and parent traversal before they are anchored to the project folder. The
        // container already refuses most of them, but the repository contract is enforced here rather than
        // relied upon from the outside.
        try {
            Repository.validatePath(filePath);
        } catch (InvalidPathException e) {
            throw new BadRequestException("file.path.invalid.message");
        }
        // The repository keeps the file under the project folder, and the history of any path is read the
        // same way, so the file's own history is the project folder's history narrowed to that path.
        return getHistoryRepositoryMapper(project.getDesignRepository())
                .getProjectHistory(project.getDesignFolderName() + "/" + filePath, searchTerm, techRevs, page);
    }

    /**
     * The folder the given branch holds the project under.
     *
     * <p>A repository that maps folders knows a project by a name of its own on every branch, so the
     * name the project carries - the one its own branch maps it under - cannot be used to read another
     * branch. The path inside the repository is the same everywhere, and the branch maps its own name
     * onto it, or holds no such folder at all.
     */
    private static @Nullable String folderIn(Repository repository, RulesProject project) {
        return repository.supports().mappedFolders()
                ? ((FolderMapper) repository).findMappedName(project.getRealPath())
                : project.getDesignFolderName();
    }

    private static Repository checkoutBranchIfPresent(Repository repository, String branch) throws IOException {
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
