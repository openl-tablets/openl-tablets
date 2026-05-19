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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.model.project.status.FileChange;
import org.openl.studio.projects.model.project.status.PendingChanges;

/**
 * Diffs the local working copy against the design revision the project is opened on by
 * walking the file lists exposed by the local and design repositories. The local
 * repository stores a per-file {@code uniqueId} that matches the design repository's
 * {@code uniqueId} when the file is in sync; modified files have it cleared.
 *
 * @author Vladyslav Pikus
 */
@Service
@Slf4j
public class PendingChangesResolverImpl implements PendingChangesResolver {

    private static final Comparator<FileChange> CHANGE_ORDER = Comparator.comparing(FileChange::type)
            .thenComparing(FileChange::path, String.CASE_INSENSITIVE_ORDER);

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
                continue;
            }
            visitedDesign.add(path);
            var localUniqueId = local.getUniqueId();
            if (localUniqueId == null || !localUniqueId.equals(design.getUniqueId())) {
                result.add(new FileChange(path, ChangeType.MODIFIED));
            }
        }

        designByPath.forEach((path, ignored) -> {
            if (!visitedDesign.contains(path)) {
                result.add(new FileChange(path, ChangeType.DELETED));
            }
        });

        return result.stream().sorted(CHANGE_ORDER).toList();
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
