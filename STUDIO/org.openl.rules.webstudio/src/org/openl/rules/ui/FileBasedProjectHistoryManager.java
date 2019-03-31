package org.openl.rules.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
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

    public FileBasedProjectHistoryManager(ProjectModel projectModel,
            String storagePath,
            Integer maxFilesInStorage,
            boolean unlimitedStorage) {
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
        this.storage = new FileStorage(storagePath);
        this.unlimitedStorage = unlimitedStorage;
        this.maxFilesInStorage = maxFilesInStorage;
        if (!this.unlimitedStorage) {
            delete();
        }
    }

    @Override
    public void save(File source) {
        storage.add(source);
    }

    public final void delete() {
        storage.delete(maxFilesInStorage);
    }

    @Override
    public File get(long date) {
        List<File> files = new ArrayList<>(storage.list(new AgeFileFilter(date)));
        if (!files.isEmpty()) {
            for (File file : files) {
                if (file.lastModified() == date) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    public File getPrev(long date) {
        File current = get(date);

        List<IOFileFilter> filters = new ArrayList<>();
        filters.add(new AgeFileFilter(date));
        filters.add(new NameFileFilter(current.getName()));

        List<File> files = new ArrayList<>(storage.list(new AndFileFilter(filters)));
        if (files.size() >= 2) {
            return files.get(files.size() - 2);
        }

        return null;
    }

    @Override
    public SortedMap<Long, File> get(long... dates) {
        SortedMap<Long, File> sources = new TreeMap<>();
        for (long date : dates) {
            sources.put(date, get(date));
        }
        return sources;
    }

    @Override
    public SortedMap<Long, File> get(String... names) {
        Collection<File> files;
        if (names != null && names.length > 0) {
            files = storage.list(new NameFileFilter(names));
        } else {
            files = storage.list(TrueFileFilter.TRUE);
        }
        SortedMap<Long, File> sources = new TreeMap<>();
        for (File file : files) {
            sources.put(file.lastModified(), file);
        }
        return sources;
    }

    @Override
    public void restore(long date) throws Exception {
        File fileToRestore = get(date);
        if (fileToRestore != null) {
            File currentSourceFile = projectModel.getSourceByName(fileToRestore.getName());
            if (currentSourceFile == null) {
                // Module compilation error, can't find source by logical modules.
                // Check current module's path (most often user restores only current module)
                String path = projectModel.getModuleInfo().getRulesRootPath().getPath();
                String[] pathElements = path.replace('\\', '/').split("/");
                if (fileToRestore.getName().equals(pathElements[pathElements.length - 1])) {
                    currentSourceFile = new File(path);
                } else {
                    throw new IllegalStateException("Can restore only current module");
                }
            }
            try {
                FileUtils.copyFile(fileToRestore, currentSourceFile);
                save(fileToRestore);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                log.info("Project was restored successfully");
            } catch (Exception e) {
                log.error("Can't restore project at {}", new SimpleDateFormat().format(new Date(date)));
                throw e;
            }
        }
    }

}
