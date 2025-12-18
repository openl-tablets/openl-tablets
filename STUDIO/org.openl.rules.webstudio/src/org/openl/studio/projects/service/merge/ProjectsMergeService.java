package org.openl.studio.projects.service.merge;

import java.io.IOException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;

/**
 * Service for Git branch merge operations in OpenL Tablets projects.
 *
 * <p>This service provides core merge functionality including merge feasibility checks
 * and actual merge execution with conflict detection. It abstracts the underlying Git
 * operations and provides a simplified interface for branch merging.
 *
 * <h3>Merge Operation Modes</h3>
 * <p>The service supports bidirectional merging through {@link MergeOpMode}:
 * <ul>
 *   <li><b>RECEIVE:</b> Merge changes FROM {@code otherBranch} INTO current branch
 *       <br>Use case: Pulling feature branch changes into main branch</li>
 *   <li><b>SEND:</b> Merge changes FROM current branch INTO {@code otherBranch}
 *       <br>Use case: Pushing main branch changes to release branch</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * <p>Implementations must be thread-safe. Multiple merge operations can execute concurrently
 * on different projects, but callers are responsible for preventing concurrent operations
 * on the same project.
 *
 * @see MergeOpMode
 * @see CheckMergeResult
 * @see MergeResult
 */
public interface ProjectsMergeService {

    /**
     * Checks if two branches can be merged without conflicts.
     *
     * <p>This is a read-only operation that validates merge feasibility without modifying
     * the repository. It determines the relationship between branches and whether they
     * can be merged cleanly.
     *
     * <p><b>Branch Resolution:</b>
     * <ul>
     *   <li>In RECEIVE mode: source={@code otherBranch}, target=current branch</li>
     *   <li>In SEND mode: source=current branch, target={@code otherBranch}</li>
     * </ul>
     *
     * <p><b>Possible Results:</b>
     * <ul>
     *   <li>{@link CheckMergeStatus#MERGEABLE}: Branches can be merged without conflicts</li>
     *   <li>{@link CheckMergeStatus#UP2DATE}: Target already contains all source changes</li>
     * </ul>
     *
     * <p><b>Implementation Note:</b>
     * <p>This method uses Git's merge-base and diff algorithms to determine merge feasibility
     * without actually performing the merge.
     *
     * @param project the project containing the branches to check
     * @param otherBranch the other branch name (interpretation depends on mode)
     * @param mode merge operation mode (RECEIVE or SEND)
     * @return merge check result with source branch, target branch, and merge status
     * @throws IOException if Git operation fails or repository is inaccessible
     * @throws IllegalArgumentException if branch does not exist
     */
    @NotNull
    CheckMergeResult checkMerge(@NotNull RulesProject project,
                                @NotBlank String otherBranch,
                                @NotNull MergeOpMode mode) throws IOException;

    /**
     * Performs an actual Git merge operation between two branches.
     *
     * <p>This method executes a merge and returns the result. If conflicts are detected,
     * they are returned in the {@link MergeResult} but the merge is not committed.
     * If no conflicts occur, the merge is committed automatically.
     *
     * <p><b>Branch Resolution:</b>
     * <ul>
     *   <li>In RECEIVE mode: merges {@code otherBranch} into current branch</li>
     *   <li>In SEND mode: merges current branch into {@code otherBranch}</li>
     * </ul>
     *
     * <p><b>Success Path (No Conflicts):</b>
     * <ol>
     *   <li>Merge is performed</li>
     *   <li>Changes are committed automatically</li>
     *   <li>Result status is {@link org.openl.studio.projects.model.merge.MergeResultStatus#SUCCESS}</li>
     *   <li>Conflict info is null</li>
     * </ol>
     *
     * <p><b>Conflict Path:</b>
     * <ol>
     *   <li>Merge is attempted but conflicts detected</li>
     *   <li>Changes are NOT committed</li>
     *   <li>Repository remains in pre-merge state</li>
     *   <li>Result status is {@link org.openl.studio.projects.model.merge.MergeResultStatus#CONFLICTS}</li>
     *   <li>Conflict info contains list of conflicted files</li>
     * </ol>
     *
     * <p><b>Atomicity:</b>
     * <p>This operation is atomic: either the merge completes successfully and is committed,
     * or it fails/conflicts and no changes are made to the repository.
     *
     * <p><b>Implementation Note:</b>
     * <p>Implementations should use Git's three-way merge algorithm and properly handle
     * merge conflicts by preserving all three versions (BASE, OURS, THEIRS) for later resolution.
     *
     * @param project the project to merge
     * @param otherBranch the other branch name (interpretation depends on mode)
     * @param mode merge operation mode (RECEIVE or SEND)
     * @return merge result with status and optional conflict information
     * @throws IOException if Git operation fails or repository is inaccessible
     * @throws IllegalArgumentException if branch does not exist
     */
    @NotNull
    MergeResult merge(@NotNull RulesProject project,
                      @NotBlank String otherBranch,
                      @NotNull MergeOpMode mode) throws IOException;

}
