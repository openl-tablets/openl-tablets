package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * TimerTask for check file data source modifications.
 */
public final class CheckFileSystemChanges extends TimerTask {
    private File baseDir;
    private ArrayList<FileTimeStamp> timestamps;

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

        ArrayList<FileTimeStamp> timestamps = new ArrayList<FileTimeStamp>();
        collect(baseDir, timestamps);
        this.timestamps = timestamps;
    }

    private void collect(File file, ArrayList<FileTimeStamp> result) {
        if (file.isDirectory()) {
            File filesArray[] = file.listFiles();
            for (File f : filesArray) {
                collect(f, result);
            }
        } else {
            // Assuming that "a not directory is a file"
            result.add(new FileTimeStamp(file));
        }
    }

    public final void run() {
        int quantity = timestamps.size();
        // Allocate memory for scanning the base directory.
        // Usually directory size is not increased extensively from the previous scanning,
        // so 10 free cells is enough for fast expanding.
        ArrayList<FileTimeStamp> newTimestampes = new ArrayList<FileTimeStamp>(quantity + 10);
        // Scanning all files in the base directory
        collect(baseDir, newTimestampes);
        boolean changed = false;
        // If quantity of files is different from the previous scanning
        // e.g. files have been added or deleted
        if (newTimestampes.size() != quantity) {
            // then we can ensure that the base directory has been modified
            changed = true;
        } else {
            // else scan file modification from the previous scanning
            for (FileTimeStamp entry : timestamps) {
                if (entry.isModifyed()) {
                    // A file has been deleted or recreated to a directory or modified
                    changed = true;
                    break;
                }
            }
        }
        // After above checks if changes have been found, keep the scanned files and fire an event
        if (changed) {
            timestamps = newTimestampes;
            onChange();
        }
    }

    /**
     * Executes once on change if change is detected
     */
    protected synchronized void onChange() {
        DataSourceListener listener = fileSystemDataSource.listener;
        if (listener != null) {
            listener.onDeploymentAdded();
        }
        storage.clear();
    }

}
