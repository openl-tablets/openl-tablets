package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

/**
 * TimerTask for check file data source modifications.
 */
public final class CheckFileSystemChanges extends TimerTask {
    private File baseDir;
    private HashMap<File, Long> timestamps = new HashMap<File, Long>();

    private FileSystemDataSource fileSystemDataSource;
    private LocalTemporaryDeploymentsStorage storage;

    CheckFileSystemChanges(FileSystemDataSource fileSystemDataSource,
            LocalTemporaryDeploymentsStorage storage) {
        this.fileSystemDataSource = fileSystemDataSource;
        this.storage = storage;
        String path = fileSystemDataSource.getLoadDeploymentsFromDirectory();

        if (path == null) {
            throw new IllegalArgumentException("path argument can't be null");
        }

        baseDir = new File(path);
        File filesArray[] = baseDir.listFiles();

        // transfer to the hashmap be used a reference and keep the
        // lastModfied value
        for (File file : filesArray) {
            add(file);
        }
    }

    private void add(File file) {
        timestamps.put(file, file.lastModified());
        if (file.isDirectory()) {
            File filesArray[] = file.listFiles();
            for (File f : filesArray) {
                add(f);
            }
        }
    }

    private boolean checkModifiedAndNew(File file, Set<File> checkedFiles, boolean changed) {
        File filesArray[] = file.listFiles();

        // scan the files and check for modification/addition
        for (File f : filesArray) {
            Long current = timestamps.get(f);
            checkedFiles.add(f);
            if (current == null) {
                // new file
                changed = true;
                add(f);
            } else {
                if (current != f.lastModified()) {
                    // modified file
                    timestamps.put(f, f.lastModified());
                    changed = true;
                }
                if (f.isDirectory()) {
                    changed = checkModifiedAndNew(f, checkedFiles, changed);
                }
            }
        }
        return changed;
    }

    /**
     * Checks for deleted files.
     *
     * @return true if deleted files were
     */
    private boolean checkDeleted(Set<File> checkedFiles) {
        Set<File> ref = ((HashMap<File, Long>) timestamps.clone()).keySet();
        ref.removeAll(checkedFiles);
        for (File deletedFile : ref) {
            timestamps.remove(deletedFile);
        }
        return !ref.isEmpty();
    }

    public final void run() {
        Set<File> checkedFiles = new HashSet<File>();
        boolean changed = false;
        changed = checkModifiedAndNew(baseDir, checkedFiles, changed);
        changed |= checkDeleted(checkedFiles);
        if (changed) {
            onChange();
        }
    }

    /**
     * Executes once on change if change is detected
     */
    protected synchronized void onChange() {
        List<DataSourceListener> listeners = fileSystemDataSource.listeners;
        for (DataSourceListener listener : listeners) {
            listener.onDeploymentAdded();
        }
        storage.clear();
    }
}
