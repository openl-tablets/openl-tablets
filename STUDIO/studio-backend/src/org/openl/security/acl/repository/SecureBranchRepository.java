package org.openl.security.acl.repository;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.UserInfo;

public class SecureBranchRepository extends SecureRepository implements BranchRepository {
    private final BranchRepository branchRepository;

    public SecureBranchRepository(BranchRepository repository, SimpleRepositoryAclService simpleRepositoryAclService) {
        super(repository, simpleRepositoryAclService);
        this.branchRepository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public boolean isMergedInto(String from, String to) throws IOException {
        if (simpleRepositoryAclService.isGranted(getId(), null, List.of(BasePermission.READ))) {
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
    public void createRepositoryBranch(String branch, @Nullable String startPoint) throws IOException {
        if (simpleRepositoryAclService.isGranted(getId(), null, List.of(BasePermission.WRITE))) {
            branchRepository.createRepositoryBranch(branch, startPoint);
        } else {
            throw new AccessDeniedException("There is no permission for creating a branch.");
        }
    }

    @Override
    public void deleteRepositoryBranch(String branch) throws IOException {
        if (simpleRepositoryAclService.isGranted(getId(), null, List.of(BasePermission.WRITE))) {
            branchRepository.deleteRepositoryBranch(branch);
        } else {
            throw new AccessDeniedException("There is no permission for deleting a branch.");
        }
    }

    @Override
    public List<String> listBranches() throws IOException {
        if (simpleRepositoryAclService.isGranted(getId(), null, List.of(BasePermission.READ))) {
            return branchRepository.listBranches();
        }
        return List.of();
    }

    @Override
    public Map<String, BranchStatus> getBranchStatuses(Collection<String> branches) throws IOException {
        return branchRepository.getBranchStatuses(branches);
    }

    @Override
    public Map<String, BranchTreeRevision> getBranchTreeRevisions(Collection<String> branches,
                                                                  String path) throws IOException {
        if (simpleRepositoryAclService.isGranted(getId(), null, List.of(BasePermission.READ))) {
            return branchRepository.getBranchTreeRevisions(branches, path);
        }
        return Map.of();
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        return (BranchRepository) SecuredRepositoryFactory
                .wrapToSecureRepo(branchRepository.forBranch(branch), simpleRepositoryAclService);
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
        if (conflictResolveData != null) {
            for (FileItem fileItem : conflictResolveData.getResolvedFiles()) {
                // A conflict names its files by the path they hold in the repository itself, which is the path
                // the permissions are kept under.
                if (!simpleRepositoryAclService
                        .isGranted(getId(), fileItem.getData().getName(), List.of(BasePermission.WRITE))) {
                    throw new AccessDeniedException("There is no permission for merging changes to a branch.");
                }
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
