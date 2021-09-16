package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

public interface BranchRepository extends Repository {
    boolean isMergedInto(String from, String to) throws IOException;

    String getBranch();

    boolean isBranchProtected(String branch);

    void createBranch(String projectPath, String branch) throws IOException;

    void deleteBranch(String projectPath, String branch) throws IOException;

    List<String> getBranches(String projectPath) throws IOException;

    BranchRepository forBranch(String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;

    void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException;

    String getBaseBranch();

    void pull(UserInfo author) throws IOException;
}
