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

    /**
     * Returns what the repository knows about each requested branch: its tip commit, and whether the branch is
     * protected. Implementations may omit a branch when its status cannot be resolved.
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

    /**
     * Returns a repository view scoped to the requested branch.
     *
     * <p>An empty repository may return a view for a valid branch whose first commit has not been created yet.
     * Other repositories require the branch to exist.
     *
     * @param branch branch to select
     * @return the branch-scoped repository view
     * @throws IOException if the branch cannot be selected
     */
    BranchRepository forBranch(@NonNull String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;

    /**
     * Merges another branch into the branch of this repository.
     *
     * <p>A merge whose thread is interrupted stops at the next step it can be stopped at — while it waits for the
     * repository, between the files it merges, at the latest before it publishes the merge — and leaves the branch as
     * it found it. A merge that has started to publish finishes, and announces the moment through
     * {@link MergePublishing}, so the code that stops it no longer interrupts it.
     *
     * @param branchFrom          the branch to merge into this one
     * @param author              the author of the merge commit
     * @param conflictResolveData how the conflicts of the merge are resolved, or {@code null} to merge without
     *                            resolving any
     * @throws java.io.InterruptedIOException when the merge is stopped by the interrupt of its thread; the interrupt
     *                                        status stays set
     * @throws IOException                    when the branches cannot be merged
     */
    void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
