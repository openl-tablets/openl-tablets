package org.openl.rules.project.impl.local;

import java.io.File;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.lock.LockManager;
import org.openl.rules.project.abstraction.LockEngine;

public class LockEngineImpl implements LockEngine {
    public static final String LOCKS_FOLDER_NAME = ".locks";
    private final LockManager lockManager;

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
        this.lockManager = new LockManager(locksRoot.toPath());
    }

    @Override
    public synchronized boolean tryLock(String branch, String projectName, String userName) {
        String lockId = getId("", branch, projectName);
        return lockManager.getLock(lockId).tryLock(userName);
    }

    @Override
    public synchronized void unlock(String branch, String projectName) {
        String lockId = getId("", branch, projectName);
        lockManager.getLock(lockId).unlock();
    }

    @Override
    public synchronized LockInfo getLockInfo(String branch, String projectName) {
        String lockId = getId("", branch, projectName);
        return lockManager.getLock(lockId).info();
    }

    /**
     * If branch null, return the folder ".locks/no-branch". If branch is not null, return the folder
     * .locks/branches/<project-name>/<branch-name>/
     *
     * @param branch branch name. Can be null
     * @param projectName project name
     * @return the folder where lock file is stored
     */

    private String getId(String repo, String branch, String projectName) {
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
        String branchId = branch == null ? "no-branch" : ("branch/" + branch.replace("[/\\\\]", "_"));
        return projectName + "/" + branchId;
    }
}
