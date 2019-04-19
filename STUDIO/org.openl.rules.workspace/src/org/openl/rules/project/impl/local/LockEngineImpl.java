package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.openl.rules.common.LockInfo;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.LockException;
import org.openl.util.IOUtils;

public class LockEngineImpl implements LockEngine {
    public static final String LOCKS_FOLDER_NAME = ".locks";
    private static final String USER_NAME = "user";
    private static final String DATE = "date";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
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
        LockInfo lockInfo = getLockInfo(branch, projectName);
        if (lockInfo.isLocked()) {
            return userName.equals(lockInfo.getLockedBy().getUserName());
        }
        Properties properties = new Properties();
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(DATE, new SimpleDateFormat(DATE_FORMAT).format(new Date()));

        File branchFolder = getBranchFolder(branch, projectName);
        File file = new File(branchFolder, projectName);
        File folder = file.getParentFile();
        if (folder != null && !folder.mkdirs() && !folder.exists()) {
            throw new IllegalStateException("Can't create a folder for locks");
        }

        FileOutputStream os = null;
        try {
            file.createNewFile();
            os = new FileOutputStream(file);
            properties.store(os, "Lock info");
        } catch (IOException e) {
            throw new LockException("Can't lock the project " + projectName, e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return true;

    }

    @Override
    public synchronized void unlock(String branch, String projectName) {
        File branchFolder = getBranchFolder(branch, projectName);
        File file = new File(branchFolder, projectName);
        file.delete();
    }

    @Override
    public synchronized LockInfo getLockInfo(String branch, String projectName) {
        File branchFolder = getBranchFolder(branch, projectName);
        File file = new File(branchFolder, projectName);
        if (!file.exists() || !file.isFile()) {
            return new SimpleLockInfo(false, null, null);
        }

        Properties properties = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            properties.load(is);
            final String userName = properties.getProperty(USER_NAME);
            final Date date = new SimpleDateFormat(DATE_FORMAT).parse(properties.getProperty(DATE));

            return new SimpleLockInfo(true, date, userName);
        } catch (IOException | ParseException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * If branch null, return the folder ".locks/no-branch".
     * If branch isn't null, return the folder .locks/branches/<project-name>/<branch-name>/
     *
     * @param branch      branch name. Can be null
     * @param projectName project name
     * @return the folder where lock file is stored
     */
    private File getBranchFolder(String branch, String projectName) {
        if (branch == null) {
            return new File(locksRoot, "no-branch");
        } else {
            // To avoid conflict between branch and project names, branch folder will be:
            //   .locks/branches/<project-name>/<branch-name>/
            // and inside that folder a file with the name <project-name> will be stored.
            // So full path for a file will be: .locks/branches/<project-name>/<branch-name>/<project-name>
            // Note: branch name can contain '/' symbol
            // Example:
            //   project1 name: "test", branch1: "WebStudio/test/user1"
            //   project2 name: "user1", branch2: "WebStudio/test"
            // Then full paths for both lock files will be:
            //   .locks/branches/test/WebStudio/test/user1/test
            //   .locks/branches/user1/WebStudio/test/user1
            // If we don't include project name before branch name, there will be conflict:
            //   locks/branches/WebStudio/test/user1 will be treated both as a folder and a file. So we must include it.
            //
            // In this method we return only folder without locks filename.

            File branchesRoot = new File(locksRoot, "branches");
            File projectParent = new File(branchesRoot, projectName);
            return new File(projectParent, branch);
        }

    }
}
