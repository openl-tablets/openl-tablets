package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;
import org.openl.studio.projects.validator.ProjectStateValidator;

@Validated
@Service
public class ProjectsMergeServiceImpl implements ProjectsMergeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsMergeServiceImpl.class);

    private final ProjectStateValidator projectStateValidator;
    private final RepositoryAclService designRepositoryAclService;

    public ProjectsMergeServiceImpl(ProjectStateValidator projectStateValidator,
                                    RepositoryAclService designRepositoryAclService) {
        this.projectStateValidator = projectStateValidator;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    @NotNull
    public CheckMergeResult checkMerge(@NotNull RulesProject project,
                                       @NotBlank String otherBranch,
                                       @NotNull MergeOpMode mode) throws IOException {
        validateMerge(project, otherBranch);
        var repository = getBranchRepository(project);
        var branchPair = getBranchPair(project, otherBranch, mode);
        validateProjectLock(project, branchPair.target());
        boolean merged = repository.isMergedInto(branchPair.source(), branchPair.target());
        return CheckMergeResult.builder()
                .sourceBranch(branchPair.source())
                .targetBranch(branchPair.target())
                .status(merged ? CheckMergeStatus.UP2DATE : CheckMergeStatus.MERGEABLE)
                .build();
    }

    private BranchPair getBranchPair(RulesProject project, String otherBranch, MergeOpMode mode) {
        return switch (mode) {
            case RECEIVE -> new BranchPair(otherBranch, project.getBranch());
            case SEND -> new BranchPair(project.getBranch(), otherBranch);
        };
    }

    private void validateMerge(RulesProject project, String otherBranch) throws IOException {
        if (!projectStateValidator.canMerge(project)) {
            throw new ConflictException("project.merge.invalid.state.message");
        }
        if (!hasMergePermission(project)) {
            throw new ForbiddenException();
        }
        if (Objects.equals(project.getBranch(), otherBranch)) {
            throw new ConflictException("project.merge.same.branches.message");
        }
        var repository = getBranchRepository(project);
        if (!repository.branchExists(otherBranch)) {
            throw new ConflictException("project.merge.branch.not.found.message", otherBranch);
        }
    }

    private boolean hasMergePermission(RulesProject project) {
        // FIXME Potential performance spike: If the project contains a large number of artifacts, it may result in slower performance.
        for (AProjectArtefact artefact : project.getArtefacts()) {
            if (designRepositoryAclService.isGranted(artefact,
                    List.of(BasePermission.WRITE, BasePermission.DELETE, BasePermission.CREATE))) {
                return true;
            }
        }
        return false;
    }

    private void validateProjectLock(RulesProject project, String targetBranch) {
        var userWorkspace = getUserWorkspace();
        var repository = getBranchRepository(project);
        LockEngine projectsLockEngine = userWorkspace.getProjectsLockEngine();
        LockInfo lockInfo = projectsLockEngine.getLockInfo(repository.getId(), targetBranch, project.getRealPath());
        if (lockInfo.isLocked()) {
            throw new ConflictException("project.merge.branch.locked", targetBranch);
        }
    }

    private BranchRepository getBranchRepository(RulesProject project) {
        var repo = project.getDesignRepository();
        if (repo.supports().branches()) {
            return (BranchRepository) repo;
        }
        throw new ConflictException("project.merge.repository.unsupported.message");
    }

    @Override
    public MergeResult merge(RulesProject project, String otherBranch, MergeOpMode mode) throws IOException {
        validateMerge(project, otherBranch);
        var branchPair = getBranchPair(project, otherBranch, mode);
        validateProjectLock(project, branchPair.target());
        var branchRepository = getBranchRepository(project);
        var currentUser = getUserWorkspace().getUser();
        var mergeResultBuilder = MergeResult.builder();
        try {
            branchRepository.forBranch(branchPair.target()).merge(branchPair.source(), currentUser.getUserInfo(), null);
        } catch (MergeConflictException conflictEx) {
            LOG.warn("Merge conflict occurred while merging branch '{}' into branch '{}' for project '{}'",
                    branchPair.source(),
                    branchPair.target(),
                    project.getName());

            mergeResultBuilder.conflictInfo(MergeConflictInfo.builder()
                    .details(conflictEx.getDetails())
                    .project(project)
                    .mergeBranchFrom(branchPair.source())
                    .mergeBranchTo(branchPair.target())
                    .currentBranch(project.getBranch())
                    .build());
        }
        return mergeResultBuilder.build();
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    private record BranchPair(String source, String target) {
    }

}
