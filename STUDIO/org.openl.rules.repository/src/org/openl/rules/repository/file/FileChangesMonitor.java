package org.openl.rules.repository.file;

import java.io.File;
import java.util.ArrayList;

import org.openl.rules.repository.common.RevisionGetter;

public final class FileChangesMonitor implements RevisionGetter {
    private final File baseDir;
    private ArrayList<FileTimeStamp> timestamps = new ArrayList<>(0);
    private int revision;

    FileChangesMonitor(File baseDir) {
        this.baseDir = baseDir;
    }

    private void collect(File file, ArrayList<FileTimeStamp> result) {
        if (file.isDirectory()) {
            File[] filesArray = file.listFiles();
            if (filesArray != null) {
                for (File f : filesArray) {
                    collect(f, result);
                }
            }
        } else {
            // Assuming that "a not directory is a file"
            result.add(new FileTimeStamp(file));
        }
    }

    @Override
    public Object getRevision() {
        int quantity = timestamps.size();
        // Allocate memory for scanning the base directory.
        // Usually directory size is not increased extensively from the previous scanning,
        // so 10 free cells is enough for fast expanding.
        ArrayList<FileTimeStamp> newTimestamps = new ArrayList<>(quantity + 10);
        // Scanning all files in the base directory
        collect(baseDir, newTimestamps);
        boolean changed = false;
        // If quantity of files is different from the previous scanning
        // e.g. files have been added or deleted
        if (newTimestamps.size() != quantity) {
            // then we can ensure that the base directory has been modified
            changed = true;
        } else {
            // else scan file modification from the previous scanning
            for (FileTimeStamp entry : timestamps) {
                if (entry.isModified()) {
                    // A file has been deleted or recreated to a directory or modified
                    changed = true;
                    break;
                }
            }
        }
        // After above checks if changes have been found, keep the scanned files and fire an event
        if (changed) {
            timestamps = newTimestamps;
            revision++;
        }
        return revision;
    }
}