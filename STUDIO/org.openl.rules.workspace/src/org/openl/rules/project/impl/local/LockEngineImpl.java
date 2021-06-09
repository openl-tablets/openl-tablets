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
    public synchronized boolean tryLock(String repoId, String branch, String projectName, String userName) {
        String lockId = getId(repoId, branch, projectName);
        return lockManager.getLock(lockId).tryLock(userName);
    }

    @Override
    public synchronized void unlock(String repoId, String branch, String projectName) {
        String lockId = getId(repoId, branch, projectName);
        lockManager.getLock(lockId).unlock();
    }

    @Override
    public void forceUnlock(String repoId, String branch, String projectName) {
        String lockId = getId(repoId, branch, projectName);
        lockManager.getLock(lockId).forceUnlock();
    }

    @Override
    public synchronized LockInfo getLockInfo(String repoId, String branch, String projectName) {
        String lockId = getId(repoId, branch, projectName);
        return lockManager.getLock(lockId).info();
    }

    /**
     * Id in the format "${repo}/${projectName}/.branches/${branch}".
     * If the branch is null, then id will be "${repo}/${projectName}/.no-branch".
     *
     * @param repo repository id. For example: design1, design2, design3 etc.
     * @param branch branch name. Can be null. Can contain '/' symbol.
     * @param projectName project name
     * @return the folder where lock file is stored
     */
    private String getId(String repo, String branch, String projectName) {
        // In this method we return only folder without locks filename.
        String branchId = branch == null ? "" : "/[branches]/" + branch;
        return repo + "/" + projectName + branchId;
    }
}
