package org.openl.rules.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.source.SourceHistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements SourceHistoryManager<File> {

    private final Logger log = LoggerFactory.getLogger(FileBasedProjectHistoryManager.class);

    private ProjectModel projectModel;
    private FileStorage storage;
    private int maxFilesInStorage;
    private boolean unlimitedStorage;

    public FileBasedProjectHistoryManager(ProjectModel projectModel, String storagePath, Integer maxFilesInStorage, boolean unlimitedStorage) {
        if (projectModel == null) {
            throw new IllegalArgumentException();
        }
        if (storagePath == null) {
            throw new IllegalArgumentException();
        }
        if (!unlimitedStorage && maxFilesInStorage == null) {
            throw new IllegalArgumentException();
        }
        this.projectModel = projectModel;
        this.storage = new FileStorage(storagePath, true);
        this.unlimitedStorage = unlimitedStorage;
        this.maxFilesInStorage = maxFilesInStorage;
        if (!this.unlimitedStorage) {
            delete();
        }
    }

    public void save(File source) {
        storage.add(source);
    }

    public final void delete() {
        storage.delete(maxFilesInStorage);
    }

    public File get(long date) {
        List<File> files = new ArrayList<File>(
                storage.list(new AgeFileFilter(date)));
        if (!files.isEmpty()) {
            return files.get(files.size() - 1);
        }
        return null;
    }

    public File getPrev(long date) {
        File current = get(date);

        List<IOFileFilter> filters = new ArrayList<IOFileFilter>();
        filters.add(new AgeFileFilter(date));
        filters.add(new NameFileFilter(current.getName()));

        List<File> files = new ArrayList<File>(
                storage.list(new AndFileFilter(filters)));
        if (files.size() >= 2) {
            return files.get(files.size() - 2);
        }

        return null;
    }

    public SortedMap<Long, File> get(long... dates) {
        SortedMap<Long, File> sources = new TreeMap<Long, File>();
        for (long date : dates) {
            sources.put(date, get(date));
        }
        return sources;
    }

    public SortedMap<Long, File> get(String... names) {
        Collection<File> files;
        if (names != null && names.length > 0) {
            files = storage.list(names);
        } else {
            files = storage.list();
        }
        SortedMap<Long, File> sources = new TreeMap<Long, File>();
        for (File file : files) {
            sources.put(file.lastModified(), file);
        }
        return sources;
    }

    public SortedMap<Long, File> getAll() {
        return get((String[]) null);
    }

    public void revert(long date) throws Exception {
        File fileToRevert = getPrev(date);
        if (fileToRevert != null) {
            File currentSourceFile = projectModel.getSourceByName(fileToRevert.getName());
            try {
                FileUtils.copyFile(fileToRevert, currentSourceFile);
                save(fileToRevert);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                log.info("Project was reverted successfully");
            } catch (Exception e) {
                log.error("Can't revert project at {}", new SimpleDateFormat().format(new Date(date)));
                throw e;
            }
        }
    }

}
