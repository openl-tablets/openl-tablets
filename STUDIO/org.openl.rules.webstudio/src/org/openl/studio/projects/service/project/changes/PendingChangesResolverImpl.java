package org.openl.studio.projects.service.project.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.model.project.status.FileChange;
import org.openl.studio.projects.model.project.status.PendingChanges;

/**
 * Diffs the local working copy against the design revision the project is opened on.
 *
 * <p>Two design-side shapes are supported:
 * <ul>
 *   <li>folder repos ({@code supports().folders()}): list per-file {@link FileData}
 *       entries and diff by {@code uniqueId} equality;</li>
 *   <li>zip-based repos (JDBC and friends): read the project archive via
 *       {@code readHistory} and diff by zip-entry path, using the local
 *       {@code FileData.uniqueId} (cleared on local edits) as the modified marker.</li>
 * </ul>
 *
 * @author Vladyslav Pikus
 */
@Service
@Slf4j
public class PendingChangesResolverImpl implements PendingChangesResolver {

    private static final Comparator<FileChange> CHANGE_ORDER = Comparator.comparing(FileChange::type)
            .thenComparing(FileChange::path, String.CASE_INSENSITIVE_ORDER);

    /**
     * Hard cap on the number of entries inspected when listing a zip-based design project,
     * defending against malformed/zip-bomb archives that contain absurd entry counts. Real
     * OpenL projects don't approach this number.
     */
    private static final int MAX_ZIP_ENTRIES = 10_000;

    @Override
    public PendingChanges resolve(RulesProject project) {
        if (!project.isModified()) {
            return null;
        }
        try {
            var changes = computeChanges(project);
            if (changes.isEmpty()) {
                return null;
            }
            return new PendingChanges(changes.size(), changes);
        } catch (IOException e) {
            log.warn("Failed to compute pending changes for project '{}'", project.getBusinessName(), e);
            return null;
        }
    }

    private List<FileChange> computeChanges(RulesProject project) throws IOException {
        // Use the project's internal (real) path as the path prefix so that the resulting file
        // paths are consistent with the merge API which also exposes files as
        // "<projectRealPath>/<fileWithinProject>" (see ProjectsMergeConflictsServiceImpl /
        // ConflictGroup).
        var projectPath = normalize(project.getRealPath());
        var localRepository = project.getLocalRepository();
        var localPrefix = project.getLocalFolderName() + "/";
        var localFiles = localRepository.list(localPrefix);

        if (project.isLocalOnly()) {
            // No design counterpart yet; every local file is a new addition.
            return localFiles.stream()
                    .map(fileData -> projectScopedPath(fileData.getName(), localPrefix, projectPath))
                    .filter(Objects::nonNull)
                    .map(path -> new FileChange(path, ChangeType.ADDED))
                    .sorted(CHANGE_ORDER)
                    .toList();
        }

        if (project.getDesignRepository().supports().folders()) {
            return diffWithFolderDesign(project, localFiles, localPrefix, projectPath);
        }
        return diffWithZipDesign(project, localFiles, localPrefix, projectPath);
    }

    private List<FileChange> diffWithFolderDesign(RulesProject project,
                                                  List<FileData> localFiles,
                                                  String localPrefix,
                                                  String projectPath) throws IOException {
        var designRepository = project.getDesignRepository();
        var designPrefix = project.getDesignFolderName() + "/";
        var historyVersion = project.getHistoryVersion();
        var designFiles = designRepository.supports().versions() && historyVersion != null
                ? designRepository.listFiles(designPrefix, historyVersion)
                : designRepository.list(designPrefix);

        Map<String, FileData> designByPath = indexByProjectScopedPath(designFiles, designPrefix, projectPath);
        Set<String> visitedDesign = new HashSet<>();
        List<FileChange> result = new ArrayList<>();

        for (FileData local : localFiles) {
            var path = projectScopedPath(local.getName(), localPrefix, projectPath);
            if (path == null) {
                continue;
            }
            var design = designByPath.get(path);
            if (design == null) {
                result.add(new FileChange(path, ChangeType.ADDED));
            } else {
                visitedDesign.add(path);
                var localUniqueId = local.getUniqueId();
                if (localUniqueId == null || !localUniqueId.equals(design.getUniqueId())) {
                    result.add(new FileChange(path, ChangeType.MODIFIED));
                }
            }
        }

        designByPath.forEach((path, ignored) -> {
            if (!visitedDesign.contains(path)) {
                result.add(new FileChange(path, ChangeType.DELETED));
            }
        });

        return result.stream().sorted(CHANGE_ORDER).toList();
    }

    /**
     * Zip-based design repos store the whole project as one archive blob — there is no
     * per-file {@code FileData} on the design side. Compare local file paths against the
     * archive's entry list; for matched paths, treat a cleared local {@code uniqueId}
     * (the local repository clears it on every edit) as the {@code MODIFIED} marker.
     */
    private List<FileChange> diffWithZipDesign(RulesProject project,
                                               List<FileData> localFiles,
                                               String localPrefix,
                                               String projectPath) throws IOException {
        Set<String> designPaths = readZippedDesignEntryPaths(project, projectPath);
        Set<String> visitedDesign = new HashSet<>();
        List<FileChange> result = new ArrayList<>();

        for (FileData local : localFiles) {
            var path = projectScopedPath(local.getName(), localPrefix, projectPath);
            if (path == null) {
                continue;
            }
            if (!designPaths.contains(path)) {
                result.add(new FileChange(path, ChangeType.ADDED));
            } else {
                visitedDesign.add(path);
                if (local.getUniqueId() == null) {
                    result.add(new FileChange(path, ChangeType.MODIFIED));
                }
            }
        }

        designPaths.stream()
                .filter(path -> !visitedDesign.contains(path))
                .forEach(path -> result.add(new FileChange(path, ChangeType.DELETED)));

        return result.stream().sorted(CHANGE_ORDER).toList();
    }

    private static Set<String> readZippedDesignEntryPaths(RulesProject project, String projectPath) throws IOException {
        var fileItem = openDesignSnapshot(project);
        if (fileItem == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        try (var stream = fileItem.getStream(); var zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            int processed = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++processed > MAX_ZIP_ENTRIES) {
                    log.warn("Aborting pending-changes diff for project '{}': design archive exceeds {} entries",
                            project.getBusinessName(), MAX_ZIP_ENTRIES);
                    return Set.of();
                }
                collectDesignEntry(entry, project, projectPath, result);
            }
        }
        return result;
    }

    private static FileItem openDesignSnapshot(RulesProject project) throws IOException {
        Repository designRepository = project.getDesignRepository();
        String folderPath = project.getDesignFolderName();
        String historyVersion = project.getHistoryVersion();
        return designRepository.supports().versions() && historyVersion != null
                ? designRepository.readHistory(folderPath, historyVersion)
                : designRepository.read(folderPath);
    }

    private static void collectDesignEntry(ZipEntry entry,
                                           RulesProject project,
                                           String projectPath,
                                           Set<String> result) {
        if (entry.isDirectory()) {
            return;
        }
        var relative = normalize(entry.getName());
        // Reject path-traversal / absolute names that would let an attacker
        // poison the comparison map (and protect any future caller that
        // resolves these paths against the local filesystem).
        if (relative.contains("../") || relative.startsWith("/")) {
            log.warn("Skipping suspicious zip entry '{}' in project '{}'",
                    entry.getName(), project.getBusinessName());
            return;
        }
        result.add(projectPath.isEmpty() ? relative : projectPath + "/" + relative);
    }

    private static Map<String, FileData> indexByProjectScopedPath(List<FileData> files,
                                                                  String prefix,
                                                                  String projectPath) {
        Map<String, FileData> index = new HashMap<>();
        for (FileData file : files) {
            var path = projectScopedPath(file.getName(), prefix, projectPath);
            if (path != null) {
                index.put(path, file);
            }
        }
        return index;
    }

    private static String projectScopedPath(String fullName, String prefix, String projectPath) {
        if (fullName == null) {
            return null;
        }
        var normalized = normalize(fullName);
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        var relative = normalized.substring(prefix.length());
        return projectPath.isEmpty() ? relative : projectPath + "/" + relative;
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }
}
