package org.openl.rules.project.impl.local;

import java.io.File;

import org.openl.rules.common.LockInfo;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.LockException;

public class LockEngineImpl implements LockEngine {
    public static final String LOCKS_FOLDER_NAME = ".locks";
    private final File locksRoot;

    /**
     * Create Lock Engine
     *
     * @param workspacesRoot the folder where all workspaces for all users are stored
     * @param type projects type, used as a subfolder name. For example "rules" or "deployments"
     * @return Lock Engine
     */
    public static LockEngine create(File workspacesRoot, String type) {
        File locksRoot = new File(workspacesRoot, LOCKS_FOLDER_NAME);
        File projectLocksRoot = new File(locksRoot, type);
        return new LockEngineImpl(projectLocksRoot);
    }

    private LockEngineImpl(File locksRoot) {
        this.locksRoot = locksRoot;
    }

    @Override
    public synchronized boolean tryLock(String branch, String projectName, String userName) throws LockException {
        File branchFolder = getBranchFolder(branch, projectName);
        LockInfo lockInfo = IndependentLocks.getLockInfo(branchFolder.getAbsolutePath(), projectName);
        if (lockInfo.isLocked()) {
            return userName.equals(lockInfo.getLockedBy().getUserName());
        }
        return IndependentLocks.createLock(branchFolder.getAbsolutePath(), projectName, userName);
    }

    @Override
    public synchronized void unlock(String branch, String projectName) {
        File branchFolder = getBranchFolder(branch, projectName);
        try {
            IndependentLocks.unlock(branchFolder.getAbsolutePath() , projectName);
        } catch (LockException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized LockInfo getLockInfo(String branch, String projectName) {
        File branchFolder = getBranchFolder(branch, projectName);
        LockInfo lockInfo = IndependentLocks.getLockInfo(branchFolder.getAbsolutePath(), projectName);
        return lockInfo;
    }

    /**
     * If branch null, return the folder ".locks/no-branch". If branch is not null, return the folder
     * .locks/branches/<project-name>/<branch-name>/
     *
     * @param branch branch name. Can be null
     * @param projectName project name
     * @return the folder where lock file is stored
     */
    private File getBranchFolder(String branch, String projectName) {
        if (branch == null) {
            return new File(locksRoot, "no-branch");
        } else {
            // To avoid conflict between branch and project names, branch folder will be:
            // .locks/branches/<project-name>/<branch-name>/
            // and inside that folder a file with the name <project-name> will be stored.
            // So full path for a file will be: .locks/branches/<project-name>/<branch-name>/<project-name>
            // Note: branch name can contain '/' symbol
            // Example:
            // project1 name: "test", branch1: "WebStudio/test/user1"
            // project2 name: "user1", branch2: "WebStudio/test"
            // Then full paths for both lock files will be:
            // .locks/branches/test/WebStudio/test/user1/test
            // .locks/branches/user1/WebStudio/test/user1
            // If we don't include project name before branch name, there will be conflict:
            // locks/branches/WebStudio/test/user1 will be treated both as a folder and a file. So we must include it.
            //
            // In this method we return only folder without locks filename.

            File branchesRoot = new File(locksRoot, "branches");
            File projectParent = new File(branchesRoot, projectName);
            return new File(projectParent, branch);
        }

    }
}
