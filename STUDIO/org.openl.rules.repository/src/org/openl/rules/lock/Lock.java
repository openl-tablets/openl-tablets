package org.openl.rules.lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
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
        LockInfo info = getInfo();
        if (info.isLocked()) {
            // If lockedBy is empty, will return false. Can't lock second time with empty user.
            return !info.getLockedBy().isEmpty() && info.getLockedBy().equals(lockedBy);
        }
        boolean lockAcquired = false;
        Path prepareLock = null;
        try {
            prepareLock = createLockFile(lockedBy);
            if (prepareLock != null) {
                lockAcquired = finishLockCreating(prepareLock);
            }
        } catch (Exception e) {
            LOG.info("Failure to create a lock file '{}'. Because of {} : {}",
                lockPath,
                e.getClass().getName(),
                e.getMessage());
        } finally {
            if (!lockAcquired) {
                // Delete because of it loos lock
                deleteLockAndFolders(prepareLock);
            }
        }
        return lockAcquired;
    }

    public boolean tryLock(String lockedBy, long time, TimeUnit unit) {
        long millisTimeout = unit.toMillis(time);
        long deadline = System.currentTimeMillis() + millisTimeout;
        boolean result = tryLock(lockedBy);
        while (!result && !Thread.currentThread().isInterrupted()) {
            long restTime = deadline - System.currentTimeMillis();
            if (restTime <= 0) {
                // No time for waiting! Exit.
                break;
            }

            // 1 is guaranty non zero sleep time
            long sleepTime = Math.min(restTime / 10 + 1, ThreadLocalRandom.current().nextLong(20, 1000));
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                result = tryLock(lockedBy);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Thread is interrupted. Quit the loop.
                break;
            }
        }
        return result;
    }

    public boolean forceLock(String lockedBy, long timeToLive, TimeUnit unit) {
        boolean result = tryLock(lockedBy, timeToLive, unit);
        if (!result && !Thread.currentThread().isInterrupted()) {
            LockInfo info = getInfo();
            String message = "Too much time after the lock file has been created. Seems the lock file is never gonna be unlocked. Try to unlock it by ourselves.\n" + "Lock path: {}\n" + "Locked at: {}\n" + "Locked by: {}\n" + "Time to live: {} {}";
            LOG.warn(message, lockPath, info.getLockedAt(), info.getLockedBy(), timeToLive, unit);
            forceUnlock();
            result = tryLock(lockedBy);
        }
        return result;
    }

    public void unlock() {
        unlock(false);
    }

    public void forceUnlock() {
        unlock(true);
    }

    private void unlock(boolean force) {
        try {
            if (force) {
                FileUtils.delete(lockPath.toFile());
            } else {
                Files.deleteIfExists(lockPath.resolve(READY_LOCK));
            }
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
        while (!file.equals(locksLocation.toFile()) && file.delete()) {
            file = file.getParentFile();
        }
    }

    public LockInfo info() {
        return getInfo();
    }

    private LockInfo getInfo() {
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
            } catch (Exception e) {
                try {
                    // Fallback to the old approach when date was stored in yyyy-MM-dd format
                    // TODO: remove this block on OpenL v5.25.0
                    date = LocalDate.parse(stringDate).atStartOfDay(ZoneOffset.UTC).toInstant();
                } catch (Exception ignored2) {
                    date = Instant.ofEpochMilli(0);
                    LOG.warn("Failed to parse date '{}'.", stringDate, e);
                }
            }
            return new LockInfo(date, userName);
        } catch (NoSuchFileException e) {
            // Lock can be deleted in another thread
            return LockInfo.NO_LOCK;
        } catch (IOException e) {
            LOG.info("Impossible to read the lock file.", e);
            // Lock file exists but failed to retrieve lock info.
            return new LockInfo(Instant.now(), null);
        }
    }

    Path createLockFile(String userName) {
        String userNameHash = Integer.toString(userName.hashCode(), 24);
        try {
            Files.createDirectories(lockPath);
            Path lock = lockPath.resolve(userNameHash + ".lock");
            try (Writer os = Files.newBufferedWriter(lock, StandardOpenOption.CREATE_NEW)) {
                os.write("#Lock info\n");
                os.append("user=").append(userName).write('\n');
                os.append("date=").append(Instant.now().toString()).write('\n');
            } catch (FileAlreadyExistsException | AccessDeniedException | NoSuchFileException e) {
                // Can't create lock file
                return null;
            } catch (Exception e) {
                // Lock file is created but with error. So delete it.
                LOG.info("Lock file '{}' is created with errors. Lock file is deleted.", lock);
                deleteLockAndFolders(lock);
                return null;
            }
            return lock;
        } catch (Exception e) {
            return null;
        }
    }

    boolean finishLockCreating(Path lock) throws IOException {
        File[] files = lockPath.toFile().listFiles();
        if (CollectionUtils.isEmpty(files)) {
            // We assume that at this step we must have one current lock file in the folder at least.
            // So, if there is an empty folder, then unlock is happened, and the lock file has been deleted.
            return false;
        }
        try {
            Path lockName = lock.getFileName();
            FileTime current = Files.getLastModifiedTime(lock);
            for (File file : files) {
                Path anotherName = file.toPath().getFileName();
                FileTime another = Files.getLastModifiedTime(file.toPath());
                if (current.compareTo(
                    another) > 0 || (current.compareTo(another) == 0 && lockName.compareTo(anotherName) > 0)) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        }
        Files.move(lock, lockPath.resolve(READY_LOCK));
        return true;
    }

    private void deleteLockAndFolders(Path lock) {
        try {
            if (lock != null) {
                Files.delete(lock);
            }
            deleteEmptyParentFolders();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
