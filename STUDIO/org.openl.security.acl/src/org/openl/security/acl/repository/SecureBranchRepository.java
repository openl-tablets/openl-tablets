package org.openl.security.acl.repository;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openl.rules.repository.api.*;
import org.openl.security.acl.permission.AclPermission;

public class SecureBranchRepository extends SecureFolderRepository implements BranchRepository {
    private final BranchRepository branchRepository;

    public SecureBranchRepository(BranchRepository repository, RepositoryAclService repositoryAclService) {
        super(repository, repositoryAclService);
        this.branchRepository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public boolean isMergedInto(String from, String to) throws IOException {
        if (repositoryAclService
            .isGranted(branchRepository.getId(), null, List.of(AclPermission.DESIGN_REPOSITORY_READ))) {
            return branchRepository.isMergedInto(from, to);
        }
        throw new AccessDeniedException("There is no permission for the action.");
    }

    @Override
    public String getBranch() {
        return branchRepository.getBranch();
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return branchRepository.isBranchProtected(branch);
    }

    @Override
    public void createBranch(String projectPath, String branch) throws IOException {
        if (repositoryAclService
            .isGranted(branchRepository.getId(), null, List.of(AclPermission.DESIGN_REPOSITORY_WRITE))) {
            branchRepository.createBranch(projectPath, branch);
        } else {
            throw new AccessDeniedException("There is no permission for creating a branch.");
        }
    }

    @Override
    public void deleteBranch(String projectPath, String branch) throws IOException {
        if (repositoryAclService
            .isGranted(branchRepository.getId(), null, List.of(AclPermission.DESIGN_REPOSITORY_DELETE))) {
            branchRepository.deleteBranch(projectPath, branch);
        } else {
            throw new AccessDeniedException("There is no permission for deleting a branch.");
        }
    }

    @Override
    public List<String> getBranches(String projectPath) throws IOException {
        if (repositoryAclService
            .isGranted(branchRepository.getId(), projectPath, List.of(AclPermission.DESIGN_REPOSITORY_READ))) {
            return branchRepository.getBranches(projectPath);
        }
        return Collections.emptyList();
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        return branchRepository.forBranch(branch);
    }

    @Override
    public boolean isValidBranchName(String branch) {
        return branchRepository.isValidBranchName(branch);
    }

    @Override
    public boolean branchExists(String branch) throws IOException {
        return branchRepository.branchExists(branch);
    }

    @Override
    public void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException {
        for (FileItem fileItem : conflictResolveData.getResolvedFiles()) {
            if (repositoryAclService.isGranted(branchRepository.getId(),
                fileItem.getData().getName(),
                List.of(AclPermission.DESIGN_REPOSITORY_WRITE))) {
                throw new AccessDeniedException("There is no permission for merging changes to a branch.");
            }
        }
        branchRepository.merge(branchFrom, author, conflictResolveData);
    }

    @Override
    public String getBaseBranch() {
        return branchRepository.getBaseBranch();
    }

    @Override
    public void pull(UserInfo author) throws IOException {
        branchRepository.pull(author);
    }

    @Override
    public List<FileData> listHistory(String name,
            String globalFilter,
            boolean techRevs,
            Pageable pageable) throws IOException {
        return branchRepository.listHistory(name, globalFilter, techRevs, pageable);
    }
}
