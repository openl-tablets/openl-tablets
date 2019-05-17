package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

public interface BranchRepository extends Repository {
    String getBranch();

    void createBranch(String projectName, String branch) throws IOException;

    void deleteBranch(String projectName, String branch) throws IOException;

    List<String> getBranches(String projectName) throws IOException;

    BranchRepository forBranch(String branch) throws IOException;

    boolean isValidBranchName(String branch);

    boolean branchExists(String branch) throws IOException;
}
