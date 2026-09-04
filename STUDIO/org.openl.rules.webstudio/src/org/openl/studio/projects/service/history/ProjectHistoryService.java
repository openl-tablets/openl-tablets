package org.openl.studio.projects.service.history;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.LockedException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.history.ProjectHistoryItem;
import org.openl.util.FileUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectHistoryService {

    private static final String CURRENT_VERSION = "_current";
    private static final String REVISION_VERSION = "Revision Version";

    @Qualifier("designRepositoryAclService")
    private final RepositoryAclService designRepositoryAclService;

    public List<ProjectHistoryItem> getLocalHistory(RulesProject project, @Nullable String moduleName) {
        return readHistory(resolveHistoryLocation(project, moduleName).historyFolder());
    }

    private List<ProjectHistoryItem> readHistory(Path projectHistoryPath) {
        var dir = projectHistoryPath.toFile();
        var historyListFiles = dir.list();
        if (historyListFiles == null || historyListFiles.length == 1) {
            return List.of();
        }
        Arrays.sort(historyListFiles, Comparator.reverseOrder());
        var collect = Arrays.stream(historyListFiles)
                .map(this::createItem)
                .collect(Collectors.toList());
        var revisionVersion = collect.removeFirst();
        collect.add(revisionVersion);
        return collect;
    }

    private static Module resolveModule(Path projectFolder, @Nullable String moduleName) {
        try {
            var descriptor = ProjectResolver.getInstance().resolve(projectFolder);
            if (descriptor == null) {
                throw new NotFoundException("project.identifier.message");
            }
            return descriptor.getModules()
                    .stream()
                    .filter(module -> moduleName == null || moduleName.equals(module.getName()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("project.module.identifier.message"));
        } catch (ProjectResolvingException e) {
            throw new NotFoundException("project.identifier.message");
        }
    }

    public List<File> getHistoryVersions(RulesProject project,
                                         @Nullable String moduleName,
                                         List<String> versions) {
        var historyFolder = resolveHistoryLocation(project, moduleName).historyFolder();
        return versions.stream().map(version -> getHistoryVersion(historyFolder, version)).toList();
    }

    public void restore(RulesProject project,
                        @Nullable String moduleName,
                        String versionToRestore,
                        @Nullable WebStudio webStudio) throws Exception {
        var location = resolveHistoryLocation(project, moduleName);
        requireWriteAccess(project.getArtefact(location.module().getRulesRootPath()));
        var fileToRestore = getHistoryVersion(location.historyFolder(), versionToRestore);
        var currentVersion = getCurrentVersion(location.historyFolder().toString());
        if (fileToRestore.equals(currentVersion)) {
            return;
        }

        var currentSourceFile = location.module().getRulesPath().toFile();
        try {
            FileUtils.copy(fileToRestore, currentSourceFile);
        } catch (FileNotFoundException e) {
            throw new LockedException(
                    e.getMessage().contains(".xls") ? "restore.xls-file.message" : "restore.file.message");
        }
        reloadOpenedModule(webStudio, currentSourceFile);
        fileToRestore.renameTo(new File(fileToRestore.getPath() + CURRENT_VERSION));
        if (currentVersion != null) {
            currentVersion.renameTo(new File(currentVersion.getPath().replaceAll(CURRENT_VERSION + "$", "")));
        }
    }

    private static void reloadOpenedModule(@Nullable WebStudio webStudio, File restoredSource) throws Exception {
        if (webStudio == null) {
            return;
        }
        var currentModule = webStudio.getCurrentModule();
        if (currentModule == null || !Files.isSameFile(currentModule.getRulesPath(), restoredSource.toPath())) {
            return;
        }
        var model = webStudio.getModel();
        if (model != null) {
            model.reset(ReloadType.SINGLE);
        }
    }

    private static HistoryLocation resolveHistoryLocation(RulesProject project, @Nullable String moduleName) {
        if (!project.isOpened()) {
            throw new ConflictException("project.not.opened.message");
        }
        var projectFolder = project.getLocalRepository().getRoot().resolve(project.getFolderPath());
        var module = resolveModule(projectFolder, moduleName);
        return new HistoryLocation(module, FolderHelper.resolveHistoryFolder(projectFolder, module));
    }

    private static File getHistoryVersion(Path historyFolder, String version) {
        try {
            var normalizedHistoryFolder = historyFolder.toAbsolutePath().normalize();
            var versionPath = normalizedHistoryFolder.resolve(version).normalize();
            var fileName = versionPath.getFileName();
            if (fileName == null
                    || !version.equals(fileName.toString())
                    || !normalizedHistoryFolder.equals(versionPath.getParent())
                    || !Files.isRegularFile(versionPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new NotFoundException("file.version.not.found.message");
            }
            return versionPath.toFile();
        } catch (InvalidPathException e) {
            throw new NotFoundException("file.version.not.found.message");
        }
    }

    private record HistoryLocation(Module module, Path historyFolder) {
    }

    public void deleteProjectHistory(RulesProject project) throws IOException {
        requireWriteAccess(project);
        if (!project.isOpened()) {
            throw new ConflictException("project.not.opened.message");
        }
        var projectFolder = project.getLocalRepository().getRoot().resolve(project.getFolderPath());
        var historyFolder = projectFolder.resolveSibling(FolderHelper.HISTORY_FOLDER)
                .resolve(projectFolder.getFileName().toString());
        if (Files.exists(historyFolder)) {
            FileUtils.delete(historyFolder);
        }
    }

    private void requireWriteAccess(AProjectArtefact artefact) {
        if (!designRepositoryAclService.isGranted(artefact, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
    }

    public void deleteAllHistory() throws IOException {
        String projectHistoryHome = Props.text(AdministrationSettings.USER_WORKSPACE_HOME);
        var userWorkspace = new File(projectHistoryHome);
        if (userWorkspace.exists() && userWorkspace.isDirectory()) {
            // The history of every project lives in the .history folder of each user workspace directory.
            Files.walkFileTree(userWorkspace.toPath(), new HashSet<>(), 2, new DeleteHistoryVisitor());
        }
    }

    public static void init(String storagePath, File source) {
        var destFile = new File(storagePath);
        if (destFile.exists() && destFile.listFiles().length > 0) {
            return;
        }
        try {
            FileUtils.copy(source, new File(storagePath, REVISION_VERSION + CURRENT_VERSION));
        } catch (Exception e) {
            log.error("Cannot add file", e);
        }
    }

    public static void deleteHistory(String projectName) throws IOException {
        var userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession())
                .getLocalWorkspace()
                .getLocation();
        var projectHistoryPath = Path.of(userWorkspace.getPath(), FolderHelper.HISTORY_FOLDER, projectName)
                .toString();
        var dir = new File(projectHistoryPath);
        // Project can contain no history
        if (dir.exists()) {
            FileUtils.delete(dir.toPath());
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
                    var destFile = new File(storagePath, System.currentTimeMillis() + CURRENT_VERSION);
                    FileUtils.copy(source, destFile);
                    removeCurrentVersion(currentVersion);
                    deleteHistoryOverLimit(storagePath);
                }
            } catch (IOException e) {
                log.error("Cannot add file", e);
            }
        }
    }

    private static final class DeleteHistoryVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (isHistoryFolder(dir)) {
                FileUtils.delete(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isDirectory() && isHistoryFolder(file)) {
                FileUtils.delete(file);
            }
            return FileVisitResult.CONTINUE;
        }

        private static boolean isHistoryFolder(Path path) {
            var name = path.getFileName();
            return name != null && FolderHelper.HISTORY_FOLDER.equals(name.toString());
        }
    }

    private static void deleteHistoryOverLimit(String storagePath) {
        Integer count = Props.integer("project.history.count");
        if (count == null) {
            // Infinity history
            return;
        }
        var dir = new File(storagePath);
        var files = dir.listFiles();
        if (files == null) {
            return;
        }
        try {
            Arrays.sort(files);
            for (var i = 0; i < files.length - count - 1; i++) {
                var file = files[i];
                FileUtils.delete(file.toPath());
            }
            if (count == 0) {
                var revisionVersion = new File(storagePath, REVISION_VERSION);
                if (revisionVersion.exists()) {
                    revisionVersion.renameTo(new File(revisionVersion.getPath() + CURRENT_VERSION));
                }
            }
        } catch (Exception e) {
            log.error("Cannot delete history", e);
        }
    }

    private static File getCurrentVersion(String storagePath) {
        var dir = new File(storagePath);
        var historyListFiles = dir.list();
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
        var version = name.split("_")[0];
        var formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        String modifiedOn;
        try {
            var time = Long.parseLong(version);
            modifiedOn = formatter.format(new Date(time));
        } catch (NumberFormatException e) {
            modifiedOn = version;
        }

        return new ProjectHistoryItem(name, modifiedOn, name.endsWith(CURRENT_VERSION));
    }
}
