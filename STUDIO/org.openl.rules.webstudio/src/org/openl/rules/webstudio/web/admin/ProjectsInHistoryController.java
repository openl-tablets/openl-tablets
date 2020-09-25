package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.ProjectHistoryItem;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("projectsInHistory")
public class ProjectsInHistoryController {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsInHistoryController.class);

    private static final String REVISION_VERSION = "Revision Version";
    public static final String CURRENT_VERSION = "_current";

    private ProjectsInHistoryController() {
    }

    public static List<ProjectHistoryItem> getProjectHistory(String projectHistoryPath) {
        File dir = new File(projectHistoryPath);
        String[] historyListFiles = dir.list();
        if (historyListFiles == null || historyListFiles.length == 1) {
            return Collections.emptyList();
        }
        Arrays.sort(historyListFiles, Comparator.reverseOrder());
        List<ProjectHistoryItem> collect = Arrays.stream(historyListFiles)
            .map(ProjectHistoryItem::new)
            .collect(Collectors.toList());
        ProjectHistoryItem revisionVersion = collect.remove(0);
        collect.add(revisionVersion);
        return collect;
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

    public static void deleteAllHistory() throws IOException {
        String projectHistoryHome = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        File userWorkspace = new File(projectHistoryHome);
        if (userWorkspace.exists() && userWorkspace.isDirectory()) {
            Files.walkFileTree(userWorkspace.toPath(), new HashSet<>(), 3, new DeleteHistoryVisitor());
        }
        WebStudioUtils.addInfoMessage("History has been successfully deleted");
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

    public static void deleteHistoryOverLimit(String storagePath) {
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

    static class DeleteHistoryVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            FileVisitResult fileVisitResult = super.visitFile(file, attrs);
            File f = file.toFile();
            if (f.isDirectory() && f.getName().equals(FolderHelper.HISTORY_FOLDER)) {
                FileUtils.delete(f);
            }
            return fileVisitResult;
        }
    }

}
