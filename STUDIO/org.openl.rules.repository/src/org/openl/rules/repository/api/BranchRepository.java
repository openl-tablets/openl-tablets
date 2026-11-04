package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface BranchRepository extends Repository, SearchableRepository {
    boolean isMergedInto(String from, String to) throws IOException;

    /**
     * Tells what a merge of one branch into another would bring in, without merging.
     *
     * <p>It names the files both branches changed since they parted, and those of them that may conflict, as
     * {@link #findConflicts} tells them. The files changed by one branch only are found by comparing
     * trees, so they cost about the same however large the files are. Telling the conflicts reads the files both
     * branches changed.
     *
     * @param from the branch to merge in
     * @param to   the branch the merge would change
     * @return the files the merge would change, those of them both branches changed, and the conflicts among them
     * @throws IOException when a branch cannot be read
     */
    MergeScope getMergeScope(String from, String to) throws IOException;

    /**
     * Tells which files under a folder one revision changes against another.
     *
     * <p>The answer compares trees only, so it costs about the same however large the files are. A file moved to
     * another path is deleted at its old path and added at its new one.
     *
     * <p>Without an earlier revision, the later one is compared with the revision it was made on: the branch as it
     * was before it. For a merge it is the branch the merge went into, so the answer is what the merge brought in. A
     * revision made on nothing is compared with an empty folder, so every file it holds is added.
     *
     * @param path the folder, from the root of the repository; an empty path is the whole repository
     * @param from the earlier revision, or {@code null} for the revision the later one was made on
     * @param to   the later revision
     * @return the changed files under the folder, with the revisions compared
     * @throws java.io.FileNotFoundException when the repository has no such revision
     * @throws IOException                    when a revision cannot be read
     */
    RevisionChanges listChanges(@NonNull String path, @Nullable String from, @NonNull String to) throws IOException;

    /**
     * Tells the revision the histories of two revisions parted at: the last one both of them were made on.
     *
     * <p>A revision made on the other one, or the same revision twice, parted at that revision. Two revisions of
     * branches that never shared a revision have none. Of several such revisions, as a criss-cross history has, the
     * first one is told.
     *
     * @param path   the folder the revision is told for, from the root of the repository
     * @param first  a revision
     * @param second another revision
     * @return the revision as the history of the folder tells it, or {@code null} when the histories share none
     * @throws java.io.FileNotFoundException when the repository has no such revision
     * @throws IOException                    when a revision cannot be read
     */
    @Nullable FileData findCommonRevision(@NonNull String path, @NonNull String first, @NonNull String second)
            throws IOException;

    /**
     * Tells which files under a folder may conflict when two revisions are merged. Nothing is merged.
     *
     * <p>The files the two revisions changed since they parted are found first; only the files both changed, each in
     * its own way, are read. A text file may conflict when the lines both changed overlap. A workbook may conflict
     * when both changed the same cells, rows or columns of a table. Any other file changed by both may conflict, and
     * so may a file one revision deleted and the other one changed. An ignored file never conflicts.
     *
     * <p>It is an estimate of what a merge finds. Two revisions of one line of history never conflict.
     *
     * @param path   the folder, from the root of the repository; an empty path is the whole repository
     * @param first  a revision
     * @param second another revision
     * @return the paths of the conflicting files from the root of the repository
     * @throws java.io.FileNotFoundException when the repository has no such revision
     * @throws IOException                    when a revision cannot be read
     */
    @NonNull Set<@NonNull String> findConflicts(@NonNull String path, @NonNull String first, @NonNull String second)
            throws IOException;

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
     * @return what the merge changed in the branch of this repository, by the paths from the root of the repository
     * @throws java.io.InterruptedIOException when the merge is stopped by the interrupt of its thread; the interrupt
     *                                        status stays set
     * @throws IOException                    when the branches cannot be merged
     */
    MergeOutcome merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
