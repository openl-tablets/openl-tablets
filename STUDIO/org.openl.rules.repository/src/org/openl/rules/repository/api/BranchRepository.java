package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface BranchRepository extends Repository, SearchableRepository {
    boolean isMergedInto(String from, String to) throws IOException;

    String getBranch();

    boolean isBranchProtected(String branch);

    void createBranch(String projectPath, String branch) throws IOException;

    /**
     * Create branch from startPoint
     *
     * @param projectPath path to project
     * @param branch      name of branch
     * @param startPoint  revision or tag
     * @throws IOException if any
     */
    void createBranch(String projectPath, String branch, String startPoint) throws IOException;

    void deleteBranch(String projectPath, String branch) throws IOException;

    List<String> getBranches(String projectPath) throws IOException;

    /**
     * Commits the given branch is ahead/behind {@code comparedTo}, and the branch's tip commit, for
     * display. Counts are 0 when {@code branch} equals {@code comparedTo}.
     */
    BranchStatus getBranchStatus(String branch, String comparedTo) throws IOException;

    /**
     * Returns display statuses for the requested branches. Implementations may omit a branch when its status
     * cannot be resolved.
     */
    default Map<String, BranchStatus> getBranchStatuses(Collection<String> branches, String comparedTo) throws IOException {
        var result = new LinkedHashMap<String, BranchStatus>();
        for (String branch : branches) {
            try {
                result.put(branch, getBranchStatus(branch, comparedTo));
            } catch (IOException ignored) {
                // Keep the branch list usable when one branch cannot provide status metadata.
            }
        }
        return result;
    }

    BranchRepository forBranch(String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;

    void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
