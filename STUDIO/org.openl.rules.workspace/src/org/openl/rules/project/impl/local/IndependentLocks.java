package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.common.LockInfo;
import org.openl.rules.project.abstraction.LockException;

public class IndependentLocks {

    private static final int FILE_CREATION_TIMEOUT_MS = 5;
    private static final String LOCK_EXTENSION = ".lock";
    private static final String PREPARED_LOCK_PREFIX = "prepare";
    private static final String READY_LOCK_PREFIX = "ready";
    private static final String NAME_SEPARATOR = "_";
    private static final String LOCK_INFO = "Lock info";

    // lock info
    private static final String USER_NAME = "user";
    private static final String DATE = "date";

    // Error messages
    private static final String CANNOT_CREATE_LOCK = "Cannot lock the project ";
    private static final String CANNOT_UNLOCK_LOCK = "Cannot unlock the project ";
    private static final String CANNOT_CREATE_FOLDER = "Cannot create a folder for locks";

    private IndependentLocks() {
    }

    public static boolean createLock(String folder, String projectName, String userName) throws LockException {
        if (!projectLockExist(folder, projectName, null)) {
            String userNameHash = getUserNameHash(userName);
            File prepareLock = createLockFile(folder, projectName, userName, userNameHash);
            waitFileCreationTimeout();
            return finishLockCreating(folder, projectName, prepareLock, userNameHash);
        } else {
            return false;
        }
    }

    public static LockInfo getLockInfo(String folder, String projectName) {
        File file = getProjectLock(folder, projectName);
        if (file == null || !file.isFile()) {
            return SimpleLockInfo.NO_LOCK;
        }
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream(file)) {
            properties.load(is);
            String userName = properties.getProperty(USER_NAME);
            Date date = new Date(Long.parseLong(properties.getProperty(DATE)));
            return new SimpleLockInfo(true, date, userName);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void unlock(String folderPath, String projectName) throws LockException {
        File[] listFiles = getLockFolderFiles(folderPath);
        for (File file : listFiles) {
            if (file.getName().endsWith(READY_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION) || file
                .getName()
                .endsWith(PREPARED_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION)) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    throw new LockException(CANNOT_UNLOCK_LOCK + projectName);
                }
            }
        }
    }

    static boolean finishLockCreating(String folder,
            String projectName,
            File prepareLock,
            String userNameHash) throws LockException {
        if (projectLockExist(folder, projectName, prepareLock.getName())) {
            try {
                if (isCurrentLockEarliest(folder, projectName, prepareLock)) {
                    finishLock(folder, projectName, userNameHash, prepareLock);
                } else {
                    Files.delete(prepareLock.toPath());
                    return false;
                }
            } catch (IOException e) {
                throw new LockException(CANNOT_CREATE_LOCK + projectName, e);
            }
        } else {
            finishLock(folder, projectName, userNameHash, prepareLock);
        }
        return true;
    }

    static File createLockFile(String folderPath,
            String projectName,
            String userName,
            String userNameHash) throws LockException {
        Properties properties = new Properties();
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(DATE, String.valueOf(System.currentTimeMillis()));
        String fileName = userNameHash + NAME_SEPARATOR + PREPARED_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION;
        File lock = new File(folderPath, fileName);
        File folder = lock.getParentFile();
        if (folderPath != null && !folder.mkdirs() && !folder.exists()) {
            throw new IllegalStateException(CANNOT_CREATE_FOLDER);
        }
        try (FileOutputStream os = new FileOutputStream(lock)) {
            if (!lock.exists() && !lock.createNewFile()) {
                throw new LockException(CANNOT_CREATE_LOCK + projectName);
            } else {
                properties.store(os, LOCK_INFO);
            }
        } catch (IOException e) {
            throw new LockException(CANNOT_CREATE_LOCK + projectName, e);
        }
        return lock;
    }

    private static void finishLock(String folder,
            String projectName,
            String userNameHash,
            File prepareLock) throws LockException {
        File readyLock = new File(folder,
            userNameHash + NAME_SEPARATOR + READY_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION);
        if (!prepareLock.renameTo(readyLock)) {
            throw new LockException(CANNOT_CREATE_LOCK + projectName);
        }
    }

    private static boolean projectLockExist(String folderPath, String projectName, String currentLock) {
        File[] listFiles = getLockFolderFiles(folderPath);
        for (File file : listFiles) {
            if (!file.getName().equals(currentLock) && file.getName()
                .endsWith(PREPARED_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION) || file.getName()
                    .endsWith(READY_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCurrentLockEarliest(String folderPath,
            String projectName,
            File currentLock) throws IOException {
        String currentLockName = currentLock.getName();
        File[] listFiles = getLockFolderFiles(folderPath);
        long currentLockModificationDate = getLastModificationFileDate(currentLock);
        for (File file : listFiles) {
            String anotherLockName = file.getName();
            if (!anotherLockName.equals(currentLockName) && (anotherLockName
                .endsWith(READY_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION) || (anotherLockName
                    .endsWith(projectName + LOCK_EXTENSION)))) {
                if ((currentLockModificationDate - getLastModificationFileDate(
                    file)) >= FILE_CREATION_TIMEOUT_MS || currentLockName.compareTo(anotherLockName) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static File getLockFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() && !folder.mkdirs() && folder.isFile()) {
            throw new IllegalStateException(CANNOT_CREATE_FOLDER);
        }
        return folder;
    }

    private static File getProjectLock(String folderPath, String projectName) {
        File[] listFiles = getLockFolderFiles(folderPath);
        for (File file : listFiles) {
            if (file.getName().endsWith(READY_LOCK_PREFIX + NAME_SEPARATOR + projectName + LOCK_EXTENSION)) {
                return file;
            }
        }
        return null;
    }

    private static File[] getLockFolderFiles(String folderPath) {
        File folder = getLockFolder(folderPath);
        File[] files = folder.listFiles();
        return files != null ? files : new File[0];
    }

    private static String getUserNameHash(String userName) {
        return DigestUtils.md5Hex(userName);
    }

    private static long getLastModificationFileDate(File file) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return attr.lastModifiedTime().toMillis();
    }

    // necessary to ensure that all file locks were created if started simultaneously with the current method
    // otherwise a creation time conflict may occur
    // creation time has precision in milliseconds, and file creation may take less
    private static void waitFileCreationTimeout() {
        try {
            TimeUnit.MILLISECONDS.sleep(FILE_CREATION_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
