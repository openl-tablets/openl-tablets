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
    private String path;
    private HashMap<File, Long> dir = new HashMap<File, Long>();

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

        this.path = path;
        File filesArray[] = new File(path).listFiles();

        // transfer to the hashmap be used a reference and keep the
        // lastModfied value
        for (File file : filesArray) {
            add(file);
        }
    }

    private void add(File file) {
        dir.put(file, new Long(file.lastModified()));
        if (file.isDirectory()) {
            File filesArray[] = file.listFiles();
            for (File f : filesArray) {
                add(f);
            }
        }
    }

    private boolean checkModifiedAndNew(File file, Set<File> checkedFiles, boolean onChangeFired) {
        File filesArray[] = file.listFiles();

        // scan the files and check for modification/addition
        for (File f : filesArray) {
            Long current = dir.get(f);
            checkedFiles.add(f);
            if (current == null) {
                // new file
                if (!onChangeFired) {
                    onChange();
                    onChangeFired = true;
                }
                add(f);
            } else {
                if (current.longValue() != f.lastModified()) {
                    // modified file
                    dir.put(f, new Long(f.lastModified()));
                    if (!onChangeFired) {
                        onChange();
                        onChangeFired = true;
                    }
                }
                if (f.isDirectory()) {
                    onChangeFired = checkModifiedAndNew(f, checkedFiles, onChangeFired);
                }
            }
        }
        return onChangeFired;
    }

    private void checkDeleted(Set<File> checkedFiles, boolean onChangeFired) {
        // now check for deleted files
        Set<File> ref = ((HashMap<File, Long>) dir.clone()).keySet();
        ref.removeAll(checkedFiles);
        for (File deletedFile : ref) {
            dir.remove(deletedFile);
            if (!onChangeFired) {
                onChange();
                onChangeFired = true;
            }
        }
    }

    public final void run() {
        Set<File> checkedFiles = new HashSet<File>();
        boolean onChangedFired = false;
        onChangedFired = checkModifiedAndNew(new File(path), checkedFiles, onChangedFired);
        checkDeleted(checkedFiles, onChangedFired);
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
