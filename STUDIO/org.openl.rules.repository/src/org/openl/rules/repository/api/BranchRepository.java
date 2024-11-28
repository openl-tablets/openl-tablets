package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

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

    BranchRepository forBranch(String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;

    void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
