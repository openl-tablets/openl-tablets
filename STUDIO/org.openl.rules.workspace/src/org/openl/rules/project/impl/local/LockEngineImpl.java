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
    public synchronized boolean tryLock(String projectName, String userName) throws LockException {
        LockInfo lockInfo = getLockInfo(projectName);
        if (lockInfo.isLocked()) {
            return userName.equals(lockInfo.getLockedBy().getUserName());
        }
        Properties properties = new Properties();
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(DATE, new SimpleDateFormat(DATE_FORMAT).format(new Date()));

        File file = new File(locksRoot, projectName);
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
    public synchronized void unlock(String projectName) {
        File file = new File(locksRoot, projectName);
        file.delete();
    }

    @Override
    public synchronized LockInfo getLockInfo(String projectName) {
        File file = new File(locksRoot, projectName);
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
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
