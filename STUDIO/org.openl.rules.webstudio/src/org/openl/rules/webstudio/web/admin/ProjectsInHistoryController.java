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
import java.util.stream.Collectors;

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

    public static List<ProjectHistoryItem> getProjectHistory(String projectHistoryPath)  {
        File dir = new File(projectHistoryPath);
        String[] historyListFiles = dir.list();
        if (historyListFiles == null) {
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
