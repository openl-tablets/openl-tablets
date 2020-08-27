package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.ProjectHistoryItem;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.springframework.stereotype.Service;

@Service("projectsInHistory")
public class ProjectsInHistoryController {

    private ProjectsInHistoryController() {
    }

    public static List<ProjectHistoryItem> getProjectHistory(String projectHistoryPath) throws IOException {
        File dir = new File(projectHistoryPath);
        List<File> historyListFiles = new ArrayList<>();
        if (dir.exists()) {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult fileVisitResult = super.visitFile(file, attrs);
                    File f = file.toFile();
                    historyListFiles.add(f);
                    return fileVisitResult;
                }
            });
        }
        SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        return historyListFiles.stream()
            .map(f -> new ProjectHistoryItem(f.lastModified(), formatter.format(new Date(f.lastModified()))))
            .sorted(Comparator.comparingLong(ProjectHistoryItem::getVersion).reversed())
            .collect(Collectors.toList());
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
