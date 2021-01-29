package org.openl.rules.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/history")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectHistoryService.class);
    private static final String CURRENT_VERSION = "_current";
    private static final String REVISION_VERSION = "Revision Version";

    @Autowired
    private HttpSession httpSession;

    @GET
    @Path("/project")
    public List<ProjectHistoryItem> getProjectHistory() {
        WebStudio webStudio = WebStudioUtils.getWebStudio(httpSession);
        ProjectModel model = webStudio.getModel();
        String projectHistoryPath = Paths
            .get(webStudio.getWorkspacePath(),
                model.getProject().getFolderPath(),
                FolderHelper.HISTORY_FOLDER,
                model.getModuleInfo().getName())
            .toString();
        File dir = new File(projectHistoryPath);
        String[] historyListFiles = dir.list();
        if (historyListFiles == null || historyListFiles.length == 1) {
            return Collections.emptyList();
        }
        Arrays.sort(historyListFiles, Comparator.reverseOrder());
        List<ProjectHistoryItem> collect = Arrays.stream(historyListFiles)
            .map(this::createItem)
            .collect(Collectors.toList());
        ProjectHistoryItem revisionVersion = collect.remove(0);
        collect.add(revisionVersion);
        return collect;
    }

    @POST
    @Path("/restore")
    public void restore(String versionToRestore) throws Exception {
        ProjectModel model = WebStudioUtils.getWebStudio(httpSession).getModel();
        if (model == null) {
            return;
        }
        String historyStoragePath = model.getHistoryStoragePath();
        File fileToRestore = get(historyStoragePath, versionToRestore);
        File currentVersion = getCurrentVersion(historyStoragePath);
        if (fileToRestore != null) {
            File currentSourceFile = model.getCurrentModuleWorkbook().getSourceFile();
            try {
                FileUtils.copy(fileToRestore, currentSourceFile);
            } catch (FileNotFoundException e) {
                String msg;
                if (e.getMessage().contains(".xls")) {
                    msg = "Restoring changes was failed. Please close module Excel file and try again.";
                } else {
                    msg = "Restoring changes was failed because some resources are used.";
                }
                throw new IOException(msg);
            }
            model.reset(ReloadType.RELOAD);
            fileToRestore.renameTo(new File(fileToRestore.getPath() + CURRENT_VERSION));
            if (currentVersion != null) {
                currentVersion.renameTo(new File(currentVersion.getPath().replaceAll(CURRENT_VERSION + "$", "")));
            }
        }
    }

    @DELETE
    public void deleteAllHistory() throws IOException {
        String projectHistoryHome = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        File userWorkspace = new File(projectHistoryHome);
        if (userWorkspace.exists() && userWorkspace.isDirectory()) {
            Files.walkFileTree(userWorkspace.toPath(), new HashSet<>(), 3, new DeleteHistoryVisitor());
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
                byte[] currentVersionBytes = Files.readAllBytes(currentVersion.toPath());
                byte[] sourceBytes = Files.readAllBytes(source.toPath());
                if (!Arrays.equals(currentVersionBytes, sourceBytes)) {
                    File destFile = new File(storagePath, System.currentTimeMillis() + CURRENT_VERSION);
                    FileUtils.copy(source, destFile);
                    removeCurrentVersion(currentVersion);
                    deleteHistoryOverLimit(storagePath);
                }
            } catch (IOException e) {
                LOG.error("Cannot add file", e);
            }
        }
    }

    static class DeleteHistoryVisitor extends SimpleFileVisitor<java.nio.file.Path> {
        @Override
        public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
            FileVisitResult fileVisitResult = super.visitFile(file, attrs);
            File f = file.toFile();
            if (f.isDirectory() && f.getName().equals(FolderHelper.HISTORY_FOLDER)) {
                FileUtils.delete(f);
            }
            return fileVisitResult;
        }
    }

    private static void deleteHistoryOverLimit(String storagePath) {
        Integer count = Props.integer("project.history.count");
        if (count == null) {
            // Infinity history
            return;
        }
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
            if (count == 0) {
                File revisionVersion = new File(storagePath, REVISION_VERSION);
                if (revisionVersion.exists()) {
                    revisionVersion.renameTo(new File(revisionVersion.getPath() + CURRENT_VERSION));
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot delete history", e);
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

    private static void removeCurrentVersion(File currentVersion) {
        if (currentVersion != null) {
            currentVersion.renameTo(new File(currentVersion.getPath().replaceAll(CURRENT_VERSION + "$", "")));
        }
    }

    private ProjectHistoryItem createItem(String name) {
        String version = name.split("_")[0];
        SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        String modifiedOn;
        try {
            long time = Long.parseLong(version);
            modifiedOn = formatter.format(new Date(time));
        } catch (NumberFormatException e) {
            modifiedOn = version;
        }

        return new ProjectHistoryItem(name, modifiedOn, name.endsWith(CURRENT_VERSION));
    }
}
