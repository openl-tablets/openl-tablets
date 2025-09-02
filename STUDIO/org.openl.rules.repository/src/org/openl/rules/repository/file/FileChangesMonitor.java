package org.openl.rules.repository.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.openl.rules.repository.common.RevisionGetter;

public final class FileChangesMonitor implements RevisionGetter {
    private final Path baseDir;
    private ArrayList<FileTimeStamp> timestamps = new ArrayList<>(0);
    private int revision;

    FileChangesMonitor(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public Object getRevision() {
        int quantity = timestamps.size();
        // Allocate memory for scanning the base directory.
        // Usually directory size is not increased extensively from the previous scanning,
        // so 10 free cells is enough for fast expanding.
        ArrayList<FileTimeStamp> newTimestamps = new ArrayList<>(quantity + 10);
        // Scanning all files in the base directory
        if (Files.isDirectory(baseDir)) {
            try (var stream = Files.walk(baseDir)) {
                stream.filter(Files::isRegularFile).forEach(path -> newTimestamps.add(new FileTimeStamp(path.toFile())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
