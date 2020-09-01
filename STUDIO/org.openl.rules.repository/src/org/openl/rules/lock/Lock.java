package org.openl.rules.lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shareable, file based locking system.
 * 
 * @author Yury Molchan
 */
public class Lock {

    private static final Logger LOG = LoggerFactory.getLogger(Lock.class);
    private static final String READY_LOCK = "ready.lock";

    // lock info
    private static final String USER_NAME = "user";
    private static final String DATE = "date";

    private final Path locksLocation;
    private final Path lockPath;

    Lock(Path locksLocation, String lockId) {
        this.locksLocation = locksLocation;
        this.lockPath = locksLocation.resolve(lockId);
    }

    public boolean tryLock(String lockedBy) {
        LockInfo info = info();
        if (info.isLocked()) {
            return info.getLockedBy().equals(lockedBy);
        }
        if (!Files.exists(lockPath)) {
            try {
                Path prepareLock = createLockFile(lockedBy);
                boolean success = finishLockCreating(prepareLock);
                if (!success) {
                    // Delete because of it loos lock
                    Files.delete(prepareLock);
                    deleteEmptyParentFolders();
                }
                return success;
            } catch (IOException e) {
                LOG.error("Failure of lock creation.", e);
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean tryLock(String lockedBy, long time, TimeUnit unit) {
        return tryLock(lockedBy);
    }

    public void unlock() {
        try {
            FileUtils.delete(lockPath.toFile());
            deleteEmptyParentFolders();
        } catch (FileNotFoundException ignored) {
            // Ignored
            // It was already deleted
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deleteEmptyParentFolders() {
        File file = lockPath.toFile();
        while (!(file = file.getParentFile()).equals(locksLocation.toFile()) && file.delete()) {
            // Delete empty parent folders
        }
    }

    public LockInfo info() {
        Path lock = lockPath.resolve(READY_LOCK);
        if (!Files.isRegularFile(lock)) {
            return LockInfo.NO_LOCK;
        }
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(lock)) {
            properties.load(is);
            String userName = properties.getProperty(USER_NAME);
            Date date = new Date(Long.parseLong(properties.getProperty(DATE)));
            return new LockInfo(date, userName);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    Path createLockFile(String userName) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(DATE, String.valueOf(System.currentTimeMillis()));
        String userNameHash = Integer.toString(userName.hashCode(), 24);
        Files.createDirectories(lockPath);
        Path lock = lockPath.resolve(userNameHash + ".lock");
        try (OutputStream os = Files.newOutputStream(lock)) {
            properties.store(os, "Lock info");
        }
        return lock;
    }

    boolean finishLockCreating(Path lock) throws IOException {
        File[] files = lockPath.toFile().listFiles();
        if (CollectionUtils.isEmpty(files)) {
            // We assume that at this step we must have one current lock file in the folder at least.
            // So, if there is an empty folder, then unlock is happened, and the lock file has been deleted.
            return false;
        }
        Path lockName = lock.getFileName();
        FileTime current = Files.getLastModifiedTime(lock);
        for (File file : files) {
            Path anotheName = file.toPath().getFileName();
            FileTime another = Files.getLastModifiedTime(file.toPath());

            if (current
                .compareTo(another) > 0 || (current.compareTo(another) == 0 && lockName.compareTo(anotheName) > 0)) {
                return false;
            }
        }
        lockPath.resolve(READY_LOCK);
        Files.move(lock, lockPath.resolve(READY_LOCK));
        return true;
    }
}
