package org.openl.rules.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.source.SourceHistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements SourceHistoryManager<File> {

    private final Logger log = LoggerFactory.getLogger(FileBasedProjectHistoryManager.class);

    private ProjectModel projectModel;
    private final String storagePath;

    FileBasedProjectHistoryManager(ProjectModel projectModel,
            String storagePath,
            Integer maxFilesInStorage) {
        if (projectModel == null) {
            throw new IllegalArgumentException();
        }
        if (storagePath == null) {
            throw new IllegalArgumentException();
        }
        this.storagePath = storagePath;
        this.projectModel = projectModel;
        if (maxFilesInStorage != null) {
            delete(maxFilesInStorage);
        }
    }

    private void delete(int count) {
        File[] files = new File(storagePath).listFiles();
        // Initial version must always exist
        if (files != null && files.length > count) {
            Arrays.sort(files);
            for (int i = 0; i < files.length - count; i++) {
                File file = files[i];
                try {
                    FileUtils.deleteDirectory(file);
                } catch (Exception e) {
                    log.error("Cannot delete folder {}", file.getName(), e);
                }
            }
        }
    }

    @Override
    public synchronized void save(File source) {
        if (source == null) {
            throw new IllegalArgumentException();
        }

        long currentDate = System.currentTimeMillis();

        String destFilePath = currentDate + File.separator + source.getName();
        File destFile = new File(storagePath, destFilePath);

        try {
            FileUtils.copyFile(source, destFile);
            destFile.setLastModified(currentDate);
        } catch (Exception e) {
            log.error("Cannot add file {}", source.getName(), e);
        }
    }

    @Override
    public File get(long date) {
        List<File> files = new ArrayList<>(list(new AgeFileFilter(date)));
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

        List<File> files = new ArrayList<>(list(new AndFileFilter(filters)));
        if (files.size() >= 2) {
            return files.get(files.size() - 2);
        }

        return null;
    }

    @Override
    public List<File> get(long... dates) {
        List<File> sources = new ArrayList<>();
        for (long date : dates) {
            sources.add(get(date));
        }
        return sources;
    }

    @Override
    public List<File> get(String... names) {
        Collection<File> files;
        if (names != null && names.length > 0) {
            files = list(new NameFileFilter(names));
        } else {
            files = list(TrueFileFilter.TRUE);
        }
        return new ArrayList<>(files);
    }

    @Override
    public void restore(long date) throws Exception {
        File fileToRestore = get(date);
        if (fileToRestore != null) {
            File currentSourceFile = projectModel.getSourceByName(fileToRestore.getName());
            try {
                FileUtils.copyFile(fileToRestore, currentSourceFile);
                save(fileToRestore);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                log.info("Project was restored successfully");
            } catch (Exception e) {
                log.error("Cannot restore project at {}", new SimpleDateFormat().format(new Date(date)));
                throw e;
            }
        }
    }

    private synchronized Collection<File> list(IOFileFilter fileFilter) {
        File storageDir = new File(storagePath);
        if (storageDir.exists()) {
            return FileUtils.listFiles(storageDir, fileFilter, TrueFileFilter.TRUE);
        }
        return Collections.emptyList();
    }

}
