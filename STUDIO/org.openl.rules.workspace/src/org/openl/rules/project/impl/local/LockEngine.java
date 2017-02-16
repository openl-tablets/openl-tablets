package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.util.IOUtils;

public class LockEngine {
    private static final String USER_NAME = "user";
    private static final String DATE = "date";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final File locksRoot;

    public LockEngine(File locksRoot) {
        this.locksRoot = locksRoot;
    }

    public void lock(String projectName, String userName) throws ProjectException {
        Properties properties = new Properties();
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(DATE, new SimpleDateFormat(DATE_FORMAT).format(new Date()));

        if (!locksRoot.mkdirs() && !locksRoot.exists()) {
            throw new IllegalStateException("Can't create a folder for locks");
        }
        File file = new File(locksRoot, projectName);

        FileOutputStream os = null;
        try {
            file.createNewFile();
            os = new FileOutputStream(file);
            properties.store(os, "Lock info");
        } catch (IOException e) {
            throw new ProjectException("Can't lock the project " + projectName, e);
        } finally {
            IOUtils.closeQuietly(os);
        }

    }

    public void unlock(String projectName) {
        File file = new File(locksRoot, projectName);
        file.delete();
    }

    public LockInfo getLockInfo(String projectName) {
        File file = new File(locksRoot, projectName);
        if (!file.exists()) {
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

    private static class SimpleLockInfo implements LockInfo {
        private final boolean locked;
        private final Date date;
        private final String userName;

        public SimpleLockInfo(boolean locked, Date date, String userName) {
            this.locked = locked;
            this.date = date;
            this.userName = userName;
        }

        @Override
        public Date getLockedAt() {
            return date;
        }

        @Override
        public CommonUser getLockedBy() {
            return new WorkspaceUserImpl(userName);
        }

        @Override
        public boolean isLocked() {
            return locked;
        }
    }
}
