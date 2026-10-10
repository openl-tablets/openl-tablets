package org.openl.studio.projects.service.project.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.model.project.status.FileChange;
import org.openl.studio.projects.model.project.status.PendingChanges;

/**
 * Lists the files of a workspace project changed since the project was opened or saved.
 *
 * <p>The working copy is compared with the files as they were then ({@link RulesProject#getLocalChanges()}), whatever
 * the design repository is. The repository is not read: the revision opened is what the copy was taken from, and a
 * folder on the file system is written by other programs as well, so a file they changed is no change made in OpenL
 * Studio.
 *
 * <p>A project with no design counterpart yet has every file added.
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
        } catch (IOException | ProjectException e) {
            log.warn("Failed to compute pending changes for project '{}'", project.getBusinessName(), e);
            return null;
        }
    }

    private static List<FileChange> computeChanges(RulesProject project) throws IOException, ProjectException {
        // Use the project's internal (real) path as the path prefix so that the resulting file
        // paths are consistent with the merge API which also exposes files as
        // "<projectRealPath>/<fileWithinProject>" (see ProjectsMergeConflictsServiceImpl /
        // ConflictGroup).
        var projectPath = normalize(project.getRealPath());
        var result = new ArrayList<FileChange>();
        if (project.isLocalOnly()) {
            // No design counterpart yet; every local file is a new addition.
            var localPrefix = project.getLocalFolderName() + "/";
            project.getLocalRepository()
                    .list(localPrefix)
                    .stream()
                    .map(file -> normalize(file.getName()))
                    .filter(name -> name.startsWith(localPrefix))
                    .map(name -> change(projectPath, name.substring(localPrefix.length()), ChangeType.ADDED))
                    .forEach(result::add);
        } else {
            var changes = project.getLocalChanges();
            addAll(result, projectPath, changes.added(), ChangeType.ADDED);
            addAll(result, projectPath, changes.modified(), ChangeType.MODIFIED);
            addAll(result, projectPath, changes.deleted(), ChangeType.DELETED);
        }
        return result.stream().sorted(CHANGE_ORDER).toList();
    }

    /** Adds a change of each file, given by its path in the project starting with {@code /}. */
    private static void addAll(List<FileChange> result, String projectPath, List<String> paths, ChangeType type) {
        paths.forEach(path -> result.add(change(projectPath, path.substring(1), type)));
    }

    /** A change of the file at the path relative to the project, named below the real path of the project. */
    private static FileChange change(String projectPath, String relative, ChangeType type) {
        return new FileChange(projectPath.isEmpty() ? relative : projectPath + "/" + relative, type);
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }
}
