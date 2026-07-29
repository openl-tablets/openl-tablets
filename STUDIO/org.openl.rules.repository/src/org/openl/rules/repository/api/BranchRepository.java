package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface BranchRepository extends Repository, SearchableRepository {
    boolean isMergedInto(String from, String to) throws IOException;

    String getBranch();

    boolean isBranchProtected(String branch);

    void createBranch(String projectPath, String branch) throws IOException;

    /**
     * @deprecated Project membership is derived from Git trees. Use
     * {@link #createRepositoryBranch(String, String)}.
     *
     * @param projectPath ignored compatibility parameter
     * @param branch      name of branch
     * @param startPoint  existing branch, tag or commit revision
     * @throws IOException if the branch cannot be created
     */
    void createBranch(String projectPath, String branch, String startPoint) throws IOException;

    void deleteBranch(String projectPath, String branch) throws IOException;

    /**
     * Creates a repository branch without changing project-to-branch metadata.
     *
     * @param branch     the new branch name
     * @param startPoint an existing branch, tag, or commit revision; the current branch when {@code null}
     * @throws IOException if the start point cannot be resolved or the branch cannot be created
     */
    void createRepositoryBranch(@NonNull String branch, @Nullable String startPoint) throws IOException;

    /**
     * Deletes a repository branch without changing project-to-branch metadata.
     *
     * @param branch the branch name
     * @throws IOException if the branch cannot be deleted
     */
    void deleteRepositoryBranch(@NonNull String branch) throws IOException;

    /**
     * Returns all branches available in the repository.
     *
     * <p>Project-specific branch selections are not included.
     */
    List<String> listBranches() throws IOException;

    List<String> getBranches(String projectPath) throws IOException;

    /**
     * Returns the tip-commit status for the requested branches. Implementations may omit a branch when its
     * status cannot be resolved.
     */
    Map<String, BranchStatus> getBranchStatuses(Collection<String> branches) throws IOException;

    /**
     * Returns branch tip and content revisions for one repository-relative path.
     *
     * <p>Every resolved branch has an entry. An entry with a {@code null} tree revision means that the path is absent
     * from that branch. The entry also reports whether the tip changes the path relative to at least one parent, so a
     * merge commit that preserves the final tree can still refresh revision metadata. An unresolved branch is omitted.
     *
     * @param branches branches to resolve
     * @param path     repository-relative file or folder path; an empty path resolves the root tree
     * @return resolved revisions keyed by branch name
     * @throws IOException if the repository cannot be read
     */
    Map<String, BranchTreeRevision> getBranchTreeRevisions(@NonNull Collection<@NonNull String> branches,
                                                           @NonNull String path) throws IOException;

    BranchRepository forBranch(String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;

    void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
