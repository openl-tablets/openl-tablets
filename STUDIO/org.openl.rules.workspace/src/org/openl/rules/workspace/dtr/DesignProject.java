package org.openl.rules.workspace.dtr;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;

/**
 * One design project as a listing reports it, together with the branches that hold it.
 *
 * <p>The project is the view of the home branch, the one a caller that asks for a project alone is given.
 * The branch entries are the same resolution the home was chosen from, so a caller that needs another branch
 * does not have to resolve the project a second time.
 *
 * <p>A project of a repository without branches has none, and so does one served from the configured branch
 * while the cross-branch index is still being built.
 */
@NullMarked
public record DesignProject(AProject project, @Nullable BranchedProject branches) {

    public DesignProject {
        Objects.requireNonNull(project);
    }

    /**
     * Whether the default branch of the repository holds the project.
     *
     * <p>Without the branches that hold it, as while the index is still being built, the project is known only from
     * the branch it was listed on: it is held by the default branch when that branch is the default one, matched the
     * way the home branch matches it. A project of a repository without branches is held by none.
     */
    public boolean inDefaultBranch() {
        if (branches != null) {
            return branches.inBaseBranch();
        }
        // A mapped repository is a BranchRepository even over a store without branches, so ask for branches first.
        var repository = project.getRepository();
        return repository.supports().branches()
                && repository instanceof BranchRepository branchRepository
                && branchRepository.getBaseBranch().equalsIgnoreCase(branchRepository.getBranch());
    }
}
