package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectsInHistoryController {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsInHistoryController.class);

    private static final String REVISION_VERSION = "Revision Version";
    public static final String CURRENT_VERSION = "_current";

    private ProjectsInHistoryController() {
    }

    public static void deleteHistory(String projectName) throws IOException {
        File userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession())
            .getLocalWorkspace()
            .getLocation();
        String projectHistoryPath = Paths.get(userWorkspace.getPath(), projectName, FolderHelper.HISTORY_FOLDER)
            .toString();
        File dir = new File(projectHistoryPath);
        // Project can contain no history
        if (dir.exists()) {
            FileUtils.delete(dir);
        }
    }

    public static void save(String storagePath, File source) {
        Objects.requireNonNull(source);
        File currentVersion = getCurrentVersion(storagePath);
        if (currentVersion != null) {
            try {
                String currentVersionHash = DigestUtils.md5Hex(Files.readAllBytes(currentVersion.toPath()));
                String sourceVersionHash = DigestUtils.md5Hex(Files.readAllBytes(source.toPath()));
                if (!currentVersionHash.equals(sourceVersionHash)) {
                    removeCurrentVersion(storagePath);
                    File destFile = new File(storagePath, String.valueOf(System.currentTimeMillis()) + CURRENT_VERSION);
                    FileUtils.copy(source, destFile);
                    deleteHistoryOverLimit(storagePath);
                }
            } catch (IOException e) {
                LOG.error("Cannot add file", e);
            }
        }
    }

    private static void deleteHistoryOverLimit(String storagePath) {
        int count = Props.integer("project.history.count");
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

    public static File get(String storagePath, String version) {
        File file = new File(storagePath, version);
        return file.exists() ? file : null;
    }

    public static void init(String storagePath, File source) {
        File destFile = new File(storagePath);
        if (destFile.exists() && destFile.listFiles().length > 0) {
            return;
        }
        try {
            FileUtils.copy(source, new File(storagePath, REVISION_VERSION + CURRENT_VERSION));
        } catch (Exception e) {
            LOG.error("Cannot add file", e);
        }
    }

    public static void restore(ProjectModel projectModel, String version) throws Exception {
        File fileToRestore = get(projectModel.getHistoryStoragePath(), version);
        removeCurrentVersion(projectModel.getHistoryStoragePath());
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

    private static File getCurrentVersion(String storagePath) {
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

    private static void removeCurrentVersion(String storagePath) {
        File currentVersion = getCurrentVersion(storagePath);
        if (currentVersion != null) {
            currentVersion.renameTo(new File(currentVersion.getPath().replaceAll(CURRENT_VERSION + "$", "")));
        }
    }

}
