package org.openl.rules.lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
        boolean lockAcquired = false;
        if (!Files.exists(lockPath)) {
            try {
                Path prepareLock = createLockFile(lockedBy);
                lockAcquired = finishLockCreating(prepareLock);
                if (!lockAcquired) {
                    // Delete because of it loos lock
                    Files.delete(prepareLock);
                    deleteEmptyParentFolders();
                }
            } catch (IOException e) {
                LOG.error("Failure of lock creation.", e);
            }
        }
        return lockAcquired;
    }

    public boolean tryLock(String lockedBy, long time, TimeUnit unit) {
        long millisTimeout = unit.toMillis(time);
        long deadline = System.currentTimeMillis() + millisTimeout;
        boolean result = tryLock(lockedBy);
        while (!result && deadline > System.currentTimeMillis()) {
            try {
                TimeUnit.MILLISECONDS.sleep(millisTimeout / 10);
                result = tryLock(lockedBy);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return result;
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
            String stringDate = properties.getProperty(DATE);
            Instant date;
            try {
                date = Instant.parse(stringDate);
            } catch (Exception ignored) {
                try {
                    // Fallback to the old approach when date was stored in milliseconds
                    date = Instant.ofEpochMilli(Long.parseLong(stringDate));
                } catch (Exception ignored2) {
                    date = Instant.ofEpochMilli(0);
                    LOG.warn("Impossible to parse date {}", stringDate, ignored);
                }
            }
            return new LockInfo(date, userName);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    Path createLockFile(String userName) throws IOException {
        String userNameHash = Integer.toString(userName.hashCode(), 24);
        Files.createDirectories(lockPath);
        Path lock = lockPath.resolve(userNameHash + ".lock");
        try (Writer os = Files.newBufferedWriter(lock)) {
            os.write("#Lock info\n");
            os.append("user=").append(userName).write('\n');
            os.append("date=").append(Instant.now().toString()).write('\n');
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
            Path anotherName = file.toPath().getFileName();
            FileTime another = Files.getLastModifiedTime(file.toPath());

            if (current
                .compareTo(another) > 0 || (current.compareTo(another) == 0 && lockName.compareTo(anotherName) > 0)) {
                return false;
            }
        }
        Files.move(lock, lockPath.resolve(READY_LOCK));
        return true;
    }
}
