package org.openl.rules.ui;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.source.SourceHistoryManager;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrei Astrouski
 */
public class FileBasedProjectHistoryManager implements SourceHistoryManager<File> {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedProjectHistoryManager.class);

    private ProjectModel projectModel;
    private final String storagePath;

    private static final String REVISION_VERSION = "Revision Version";
    public static final String CURRENT_VERSION = "(current)";

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
        File dir = new File(storagePath);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        try {
            Arrays.sort(files);
            for (int i = 0; i < files.length - count - 1; i++) {
                File file = files[i];
                FileUtils.delete(file);
            }
        } catch (Exception e) {
            LOG.error("Cannot delete history", e);
        }
    }

    @Override
    public synchronized void save(File source) {
        Objects.requireNonNull(source);
        removeCurrentVersion();
        File destFile = new File(storagePath, String.valueOf(System.currentTimeMillis()) + CURRENT_VERSION);
        try {
            FileUtils.copy(source, destFile);
        } catch (Exception e) {
            LOG.error("Cannot add file", e);
        }
    }

    @Override
    public File get(String version) {
        if (version.endsWith(CURRENT_VERSION)) {
            return getCurrentVersion();
        }
        return new File(storagePath, version);
    }

    @Override
    public void init(File source) {
        File destFile = new File(storagePath, REVISION_VERSION);
        if (destFile.exists()) {
            return;
        }
        try {
            FileUtils.copy(source, destFile);
        } catch (Exception e) {
            LOG.error("Cannot add file", e);
        }
    }

    @Override
    public void restore(String version) throws Exception {
        File fileToRestore = get(version);
        removeCurrentVersion();
        if (fileToRestore != null) {
            File currentSourceFile = projectModel.getCurrentModuleWorkbook().getSourceFile();
            try {
                FileUtils.copy(fileToRestore, currentSourceFile);
                projectModel.reset(ReloadType.FORCED);
                projectModel.buildProjectTree();
                fileToRestore.renameTo(new File(fileToRestore.getPath() + CURRENT_VERSION));
                LOG.info("Project was restored successfully");
            } catch (Exception e) {
                LOG.error("Cannot restore project at {}", version);
                throw e;
            }
        }
    }

    private File getCurrentVersion() {
        File dir = new File(storagePath);
        String[] historyListFiles = dir.list();
        if (historyListFiles == null) {
            return null;
        }
        Arrays.sort(historyListFiles, Comparator.reverseOrder());
        for (String file : historyListFiles) {
            if (file.endsWith(CURRENT_VERSION)) {
                return new File(storagePath, file);
            }
        }
        return null;
    }

    private void removeCurrentVersion() {
        File currentVersion = getCurrentVersion();
        if (currentVersion != null) {
            currentVersion.renameTo(new File(currentVersion.getPath().replaceAll(Pattern.quote(CURRENT_VERSION) + "$", "")));
        }
    }
}
