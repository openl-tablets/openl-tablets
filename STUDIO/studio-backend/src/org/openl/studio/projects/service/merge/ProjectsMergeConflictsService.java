package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.InputStreamSource;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileItem;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.ConflictTextView;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;

/**
 * Service for analyzing and resolving Git merge conflicts.
 *
 * <p>This service provides functionality for working with merge conflicts after a merge
 * operation has detected conflicting changes. It supports:
 * <ul>
 *   <li>Grouping and organizing conflicted files</li>
 *   <li>Retrieving specific versions of conflicted files (BASE, OURS, THEIRS)</li>
 *   <li>Merging a conflicted text file line by line, so that its conflicts are resolved in place</li>
 *   <li>Applying various resolution strategies to resolve conflicts</li>
 * </ul>
 *
 * <h3>Conflict Grouping</h3>
 * <p>Conflicts are organized by project and automatically sorted with Excel files (containing
 * business logic) appearing first, as they are typically more critical to resolve.
 *
 * <h3>Resolution Strategies</h3>
 * <p>The service supports multiple conflict resolution strategies:
 * <ul>
 *   <li><b>BASE:</b> Use common ancestor version (before branches diverged)</li>
 *   <li><b>OURS:</b> Use current branch version</li>
 *   <li><b>THEIRS:</b> Use merging branch version</li>
 *   <li><b>CUSTOM:</b> Use user-provided merged file</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * <p>Implementations must be thread-safe. Multiple conflict resolution operations can
 * execute concurrently on different projects.
 *
 * @see MergeConflictInfo
 * @see ConflictGroup
 * @see org.openl.studio.projects.model.merge.ConflictResolutionStrategy
 */
public interface ProjectsMergeConflictsService {

    /**
     * Gets detailed conflict information including commit details for all sides.
     *
     * <p>This method provides comprehensive information about the merge conflict,
     * including:
     * <ul>
     *   <li>Conflicted files grouped by project</li>
     *   <li>Commit details for OURS side (author, date, branch)</li>
     *   <li>Commit details for THEIRS side (author, date, branch)</li>
     *   <li>Commit details for BASE side (common ancestor)</li>
     *   <li>Which versions hold each conflicted file, and whether it is UTF-8 text that can be merged line by
     *   line</li>
     *   <li>Default merge commit message</li>
     * </ul>
     *
     * <p>Telling whether a file is text reads its versions, unless its name tells a workbook or an archive. The
     * versions of a stored conflict do not change, so each file is read once for it.
     *
     * @param mergeConflictInfo conflict information from merge operation
     * @return detailed conflict response with all revision information
     * @throws IOException if revision information cannot be read from the Git repository
     */
    ConflictDetailsResponse getConflictDetails(MergeConflictInfo mergeConflictInfo) throws IOException;

    /**
     * Groups conflicted files by project with automatic sorting.
     *
     * <p>This method analyzes the conflict information and organizes conflicted files
     * into groups by project. Within each group, files are sorted with Excel files
     * (.xls, .xlsx) appearing first because they typically contain business logic
     * which is more critical to resolve than configuration or documentation files.
     *
     * <p><b>Sorting Order:</b>
     * <ol>
     *   <li>Excel files (.xls, .xlsx) - alphabetically</li>
     *   <li>Other files - alphabetically</li>
     * </ol>
     *
     * <p><b>Example Result:</b>
     * <pre>{@code
     * ConflictGroup {
     *   projectName: "MyProject",
     *   files: [
     *     "rules/BusinessRules.xlsx",  // Excel first
     *     "rules/ValidationRules.xlsx", // Excel first
     *     "config.xml",                 // Other files second
     *     "README.md"                   // Other files second
     *   ]
     * }
     * }</pre>
     *
     * @param mergeConflictInfo conflict information from merge operation
     * @return list of conflict groups, each containing project information and sorted file paths
     * @throws IllegalArgumentException if mergeConflictInfo is null
     */
    List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo);

    /**
     * Retrieves a specific version of a conflicted file.
     *
     * <p>This method provides access to one of three versions of a file that has merge conflicts:
     * <ul>
     *   <li><b>BASE:</b> Common ancestor version from the merge base (before branches diverged)</li>
     *   <li><b>OURS:</b> Version from the current branch (what was there before merge)</li>
     *   <li><b>THEIRS:</b> Version from the merging branch (incoming changes)</li>
     * </ul>
     *
     * <p><b>Use Case:</b>
     * <p>Users typically download all three versions to understand the differences and decide
     * on the best resolution strategy (or create a custom merged version).
     *
     * <p><b>Implementation Note:</b>
     * <p>The file content is retrieved from Git's three-way merge representation and streamed
     * to avoid loading large files into memory.
     *
     * @param mergeConflictInfo conflict information containing Git repository references
     * @param path relative path to the conflicted file within the project
     * @param side which version to retrieve (BASE, OURS, or THEIRS)
     * @return file item containing input stream and metadata for the requested version
     * @throws IOException if file cannot be read from Git repository
     * @throws org.openl.studio.common.exception.NotFoundException if the file does not exist in the requested revision
     * @throws IllegalArgumentException if path is not in conflict list or side is null
     * @throws IllegalStateException if Git repository is in unexpected state
     */
    FileItem getConflictFileItem(MergeConflictInfo mergeConflictInfo, String path, ConflictBase side) throws IOException;

    /**
     * Merges a conflicted text file line by line, as the merge of the branches merges it, and tells which lines it
     * takes by itself and where the branches conflict.
     *
     * <p>A version of a conflict holds every line its branch has in that place, even the lines both versions start
     * or end with. A base version that does not hold the file counts as an empty file.
     *
     * @param mergeConflictInfo conflict information containing Git repository references
     * @param path              path of the conflicted file, as the conflict lists it
     * @return the lines merged and the conflicts, in the order of the file
     * @throws IOException if a version of the file cannot be read
     * @throws org.openl.studio.common.exception.NotFoundException   if the file is not conflicted, or your version or
     *                                                               their version does not hold it
     * @throws org.openl.studio.common.exception.BadRequestException if a version of the file is no UTF-8 text
     */
    ConflictTextView getConflictText(MergeConflictInfo mergeConflictInfo, String path) throws IOException;

    /**
     * Resolves merge conflicts using specified strategies.
     *
     * <p>This method applies user-specified resolution strategies to each conflicted file
     * and completes the merge operation. The operation is atomic: either all conflicts are
     * resolved successfully, or the entire operation fails and can be retried.
     *
     * <p><b>Resolution Process:</b>
     * <ol>
     *   <li>Validate all resolutions (ensure files exist, custom files provided, etc.)</li>
     *   <li>Apply each resolution strategy to its corresponding file</li>
     *   <li>Stage all resolved files in Git</li>
     *   <li>Commit the merge with provided message (or default message)</li>
     *   <li>Declare the modules of the resolved workbooks again in the descriptors of their projects, in a commit of
     *   its own</li>
     *   <li>Return list of successfully resolved files and, for a merge, what it changed</li>
     * </ol>
     *
     * <p><b>Summary:</b>
     * <p>A resolved merge tells what it changed in the target branch, against the branch before the merge. The
     * descriptors written after the merge count among the changed files, and the revision after the merge is the
     * commit that wrote them. A save conflict has no summary. Neither has a merge whose changes cannot be told, as when
     * the project index has not published it in time; the resolution stays a success.
     *
     * <p><b>Strategy Application:</b>
     * <ul>
     *   <li><b>BASE:</b> Copy BASE version to working directory</li>
     *   <li><b>OURS:</b> Copy OURS version to working directory</li>
     *   <li><b>THEIRS:</b> Copy THEIRS version to working directory</li>
     *   <li><b>CUSTOM:</b> Write custom file content from {@code customFiles} map to working directory</li>
     * </ul>
     *
     * <p><b>Atomicity Guarantee:</b>
     * <p>If any resolution fails (e.g., custom file missing, file write error), the entire
     * operation is rolled back using Git reset, and the repository remains in its pre-resolution
     * state. This allows users to fix issues and retry the operation.
     *
     * <p><b>Commit Message:</b>
     * <p>If {@code mergeMessage} is provided, it's used as the commit message. Otherwise, a
     * default message is generated describing the merge operation and resolution.
     *
     * @param mergeConflictInfo conflict information from merge operation
     * @param resolutions list of resolutions specifying strategy for each conflicted file
     * @param customFiles map of file paths to custom file content (for CUSTOM strategy)
     * @param mergeMessage optional commit message (null for default message)
     * @return resolution response with status, list of successfully resolved file paths and, for a merge, what it
     *         changed in the target branch
     * @throws IOException if Git operation or file operation fails
     * @throws ProjectException if project operation fails
     * @throws IllegalArgumentException if any resolution is invalid (e.g., CUSTOM without file)
     * @throws IllegalStateException if Git repository is in unexpected state
     */
    ResolveConflictsResponse resolveConflicts(MergeConflictInfo mergeConflictInfo,
                                              List<FileConflictResolution> resolutions,
                                              Map<String, InputStreamSource> customFiles,
                                              String mergeMessage) throws IOException, ProjectException;

}
