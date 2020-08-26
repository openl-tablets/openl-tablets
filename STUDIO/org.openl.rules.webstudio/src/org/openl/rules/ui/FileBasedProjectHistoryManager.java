package org.openl.rules.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.source.SourceHistoryManager;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements SourceHistoryManager<File> {

    private final Logger log = LoggerFactory.getLogger(FileBasedProjectHistoryManager.class);

    private ProjectModel projectModel;
    private final String storagePath;

    FileBasedProjectHistoryManager(ProjectModel projectModel, String storagePath, Integer maxFilesInStorage) {
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
                    FileUtils.delete(file);
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
        String name = projectModel.getModuleInfo().getName();
        String destFilePath = Paths.get(storagePath, String.valueOf(currentDate)).toString();
        File destFile = new File(destFilePath, name + "." + org.openl.util.FileUtils.getExtension(source.getName()));
        try {
            FileUtils.copy(source, destFile);
            destFile.setLastModified(currentDate);
        } catch (Exception e) {
            log.error("Cannot add file {}", name, e);
        }
    }

    @Override
    public File get(long date) {
        File[] commit = new File(storagePath, String.valueOf(date)).listFiles();
        return commit != null && commit.length == 1 ? commit[0] : null;
    }

    @Override
    public void init() {
        File source = projectModel.getCurrentModuleWorkbook().getSourceFile();
        synchronized (this) {
            String projectName = projectModel.getModuleInfo().getName();
            File storageDir = new File(storagePath);
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File changeDir : files) {
                    File[] names = changeDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return projectName.equals(FileUtils.getBaseName(name));
                        }
                    });
                    if (CollectionUtils.isNotEmpty(names)) {
                        return;
                    }
                }
            }
        }
        save(source);
    }

    @Override
    public void restore(long date) throws Exception {
        File fileToRestore = get(date);
        if (fileToRestore != null) {
            File currentSourceFile = projectModel.getCurrentModuleWorkbook().getSourceFile();
            try {
                FileUtils.copy(fileToRestore, currentSourceFile);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                log.info("Project was restored successfully");
            } catch (Exception e) {
                log.error("Cannot restore project at {}", new SimpleDateFormat().format(new Date(date)));
                throw e;
            }
        }
    }

}
