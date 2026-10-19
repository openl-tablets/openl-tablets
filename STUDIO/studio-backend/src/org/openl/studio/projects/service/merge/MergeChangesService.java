package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergedWorkbook;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.FileChangeView;
import org.openl.studio.projects.model.merge.MergeScopeView;
import org.openl.studio.projects.model.merge.MergeSummaryView;
import org.openl.studio.projects.model.merge.MergedChanges;
import org.openl.studio.projects.model.merge.ProjectChangesView;
import org.openl.studio.projects.model.merge.ProjectScopeView;
import org.openl.studio.projects.model.merge.SheetChangeView;
import org.openl.studio.projects.model.merge.SheetOrigin;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.service.files.FilePaths;

/**
 * Tells which projects and files a merge brings into the target branch: before the merge, and once it is committed.
 *
 * <p>A merge changes whole branches, so it changes every project the other branch changed, not only the project it
 * was asked for. Each changed file is told by its path from the root of the repository, under the project whose
 * folder holds it. The files outside any project form a group of their own. The project asked for comes first, the
 * other projects follow by name, and the files outside any project come last.
 *
 * <p>A project the user may not read is counted, without its name or its files. A file outside any project is
 * shown only when the user may read it.
 *
 * <p>What a merge brings in is told beside the merge, never in its place. A failure to tell it is logged and leaves
 * the answer about the merge as it is: a check still tells whether the branches can be merged, and a committed
 * merge stays a success, or the user would merge again what is already merged (EPBDS-16541).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MergeChangesService {

    /** The project asked for first, then the other projects by name: the files outside any project have none. */
    private static final Comparator<ChangedProject> PROJECT_ORDER = Comparator
            .comparing((ChangedProject changed) -> !changed.current())
            .thenComparing(ChangedProject::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    /**
     * What a merge of one branch into another would bring in, or nothing when it cannot be told.
     *
     * <p>A project the source branch holds and the target branch does not is added, and one only the target branch
     * holds is deleted. The files both branches changed are named: the merge combines them or reports them as
     * conflicts, which is known only once it has run.
     *
     * @param project    the project the merge is asked for
     * @param repository the repository of the project
     * @param source     the branch to merge in
     * @param target     the branch the merge changes
     */
    public @Nullable MergeScopeView scopeOf(RulesProject project,
                                            BranchRepository repository,
                                            String source,
                                            String target) {
        try {
            return scope(project, repository, source, target);
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot tell what a merge of branch '{}' into branch '{}' brings in.", source, target, e);
            return null;
        }
    }

    private MergeScopeView scope(RulesProject project,
                                 BranchRepository repository,
                                 String source,
                                 String target) throws IOException {
        var scope = repository.getMergeScope(source, target);
        var repositoryId = project.getDesignRepository().getId();
        var changed = group(project,
                scope.changes(),
                designTimeRepository.getBranchProjects(repositoryId, target),
                designTimeRepository.getBranchProjects(repositoryId, source));
        var projects = changed.readable()
                .stream()
                .map(group -> ProjectScopeView.builder()
                        .id(group.id())
                        .name(group.name())
                        .path(group.path())
                        .current(group.current())
                        .status(group.status())
                        .changedFiles(group.files().size())
                        .changedInBoth(group.files()
                                .stream()
                                .map(FileChange::path)
                                .filter(scope.changedInBoth()::contains)
                                .sorted(ConflictGroup.FILE_ORDER)
                                .toList())
                        .build())
                .toList();
        return new MergeScopeView(projects, changed.hidden());
    }

    /**
     * The projects the target branch holds before a merge, for what the merge changes to be told against them, or
     * nothing when they cannot be read. The merge goes on all the same: the failure is only logged, and the merge then
     * tells nothing of what it changed.
     */
    public @Nullable List<AProject> projectsBefore(String repositoryId, String targetBranch) {
        try {
            return designTimeRepository.getBranchProjects(repositoryId, targetBranch);
        } catch (RuntimeException e) {
            log.warn("Cannot read the projects of branch '{}' before the merge.", targetBranch, e);
            return null;
        }
    }

    /**
     * What a committed merge changed, kept until the client reads it, or nothing when it cannot be told.
     *
     * <p>The projects of the target branch are taken when the merge is committed: the ones it held before the merge
     * are given, and the ones it holds after it are read from the project index, which has published the merge by
     * then. So whether the merge added or deleted a project as a whole does not change with what the branch receives
     * later. Only the projects holding a changed file are kept.
     *
     * @param repositoryId the repository the merge changed
     * @param before       the projects the target branch held before the merge, or {@code null} when they could not
     *                     be read, which leaves nothing to tell
     */
    public @Nullable MergedChanges changesOf(String repositoryId,
                                             String sourceBranch,
                                             String targetBranch,
                                             MergeOutcome outcome,
                                             @Nullable Collection<AProject> before) {
        if (before == null) {
            return null;
        }
        try {
            return changes(repositoryId, sourceBranch, targetBranch, outcome, before);
        } catch (RuntimeException e) {
            log.warn("Cannot tell what the merge of branch '{}' into branch '{}' changed.",
                    sourceBranch,
                    targetBranch,
                    e);
            return null;
        }
    }

    private MergedChanges changes(String repositoryId,
                                  String sourceBranch,
                                  String targetBranch,
                                  MergeOutcome outcome,
                                  Collection<AProject> before) {
        var after = designTimeRepository.getBranchProjects(repositoryId, targetBranch);
        var projectsByFolder = projectsByFolder(before, after);
        var changedFolders = outcome.changes()
                .stream()
                .map(change -> folderOf(change.path(), projectsByFolder))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new MergedChanges(sourceBranch,
                targetBranch,
                outcome,
                holding(before, changedFolders),
                holding(after, changedFolders));
    }

    /**
     * What a committed merge changed in the target branch, or nothing when it cannot be told.
     *
     * <p>A project the target branch holds after the merge and did not hold before is added, and one it no longer
     * holds is deleted. A workbook combined from both branches without a decision of the user lists the sheets
     * either branch changed, each with the branch it was taken from.
     *
     * @param project the project the merge was asked for
     * @param changes what the merge changed
     */
    public @Nullable MergeSummaryView summaryOf(RulesProject project, MergedChanges changes) {
        try {
            return summary(project, changes);
        } catch (RuntimeException e) {
            log.warn("Cannot tell what the merge of branch '{}' into branch '{}' changed.",
                    changes.sourceBranch(),
                    changes.targetBranch(),
                    e);
            return null;
        }
    }

    private MergeSummaryView summary(RulesProject project, MergedChanges changes) {
        var outcome = changes.outcome();
        var changed = group(project, outcome.changes(), changes.projectsBefore(), changes.projectsAfter());
        var workbooks = outcome.workbooks()
                .stream()
                .collect(Collectors.toMap(MergedWorkbook::path, Function.identity()));
        var projects = changed.readable()
                .stream()
                .map(group -> ProjectChangesView.builder()
                        .id(group.id())
                        .name(group.name())
                        .path(group.path())
                        .current(group.current())
                        .status(group.status())
                        .files(group.files()
                                .stream()
                                .sorted(Comparator.comparing(FileChange::path, ConflictGroup.FILE_ORDER))
                                .map(file -> new FileChangeView(file.path(),
                                        typeOf(file.kind()),
                                        sheetsOf(workbooks.get(file.path()))))
                                .toList())
                        .build())
                .toList();
        return new MergeSummaryView(changes.sourceBranch(),
                changes.targetBranch(),
                outcome.before(),
                outcome.after(),
                projects,
                changed.hidden());
    }

    /**
     * Groups the changed files by the project whose folder holds them, and leaves out what the user may not read.
     * The files of a group keep the order they came in: each view orders the files it lists.
     *
     * @param before the projects the target branch holds before the merge
     * @param after  the projects it holds after the merge, or the source branch holds before it
     */
    private Changed group(RulesProject current,
                          List<FileChange> changes,
                          Collection<AProject> before,
                          Collection<AProject> after) {
        var projectsByFolder = projectsByFolder(before, after);
        var foldersBefore = foldersOf(before);
        var foldersAfter = foldersOf(after);

        var filesByFolder = new LinkedHashMap<@Nullable String, List<FileChange>>();
        changes.forEach(change -> filesByFolder
                .computeIfAbsent(folderOf(change.path(), projectsByFolder), folder -> new ArrayList<>())
                .add(change));
        var changedProjects = filesByFolder.keySet()
                .stream()
                .filter(Objects::nonNull)
                .map(projectsByFolder::get)
                .toList();
        // One question for every changed project, rather than one per project.
        var readable = designRepositoryAclService.filterGranted(changedProjects, List.of(BasePermission.READ));

        var repositoryId = current.getDesignRepository().getId();
        var groups = new ArrayList<ChangedProject>();
        filesByFolder.forEach((folder, files) -> {
            if (folder == null) {
                var outside = readableOf(repositoryId, files);
                if (!outside.isEmpty()) {
                    groups.add(new ChangedProject(null, null, null, false, null, outside));
                }
            } else {
                var project = projectsByFolder.get(folder);
                if (readable.contains(project)) {
                    groups.add(new ChangedProject(project.getProjectId(),
                            project.getBusinessName(),
                            folder,
                            folder.equals(current.getRealPath()),
                            statusOf(foldersBefore.contains(folder), foldersAfter.contains(folder)),
                            files));
                }
            }
        });
        groups.sort(PROJECT_ORDER);
        return new Changed(groups, changedProjects.size() - readable.size());
    }

    /**
     * The files outside any project the user may read. Each file is asked under its own rights, as the repository
     * asks when the file is read: a file of a folder the user may read can be denied on its own.
     */
    private List<FileChange> readableOf(String repositoryId, List<FileChange> files) {
        return files.stream()
                .filter(file -> designRepositoryAclService.isGranted(repositoryId,
                        file.path(),
                        List.of(BasePermission.READ)))
                .toList();
    }

    private static Map<String, AProject> projectsByFolder(Collection<AProject> before, Collection<AProject> after) {
        var projectsByFolder = new HashMap<String, AProject>();
        before.forEach(project -> projectsByFolder.put(project.getRealPath(), project));
        after.forEach(project -> projectsByFolder.put(project.getRealPath(), project));
        return projectsByFolder;
    }

    private static Set<String> foldersOf(Collection<AProject> projects) {
        return projects.stream().map(AProject::getRealPath).collect(Collectors.toSet());
    }

    private static List<AProject> holding(Collection<AProject> projects, Set<String> folders) {
        return projects.stream().filter(project -> folders.contains(project.getRealPath())).toList();
    }

    /**
     * The folder of the project holding the file: the nearest folder above it that is a project folder.
     *
     * @return the folder, or {@code null} when no project holds the file
     */
    private static @Nullable String folderOf(String path, Map<String, AProject> projectsByFolder) {
        for (var folder = FilePaths.parent(path); !folder.isEmpty(); folder = FilePaths.parent(folder)) {
            if (projectsByFolder.containsKey(folder)) {
                return folder;
            }
        }
        return null;
    }

    private static ChangeType statusOf(boolean before, boolean after) {
        if (!before) {
            return ChangeType.ADDED;
        }
        return after ? ChangeType.MODIFIED : ChangeType.DELETED;
    }

    private static ChangeType typeOf(FileChange.Kind kind) {
        return switch (kind) {
            case ADDED -> ChangeType.ADDED;
            case MODIFIED -> ChangeType.MODIFIED;
            case DELETED -> ChangeType.DELETED;
        };
    }

    private static List<SheetChangeView> sheetsOf(@Nullable MergedWorkbook workbook) {
        if (workbook == null) {
            return List.of();
        }
        var sheets = new ArrayList<SheetChangeView>();
        workbook.targetSheets().forEach(sheet -> sheets.add(new SheetChangeView(sheet, SheetOrigin.TARGET)));
        workbook.sourceSheets().forEach(sheet -> sheets.add(new SheetChangeView(sheet, SheetOrigin.SOURCE)));
        workbook.bothSheets().forEach(sheet -> sheets.add(new SheetChangeView(sheet, SheetOrigin.BOTH)));
        return sheets;
    }

    /**
     * The changed files of one project, or of no project: then it has no id, no name, no path and no status.
     *
     * @param current whether it is the project the merge is asked for
     * @param status  what the merge does to the project
     */
    private record ChangedProject(@Nullable ProjectId id,
                                  @Nullable String name,
                                  @Nullable String path,
                                  boolean current,
                                  @Nullable ChangeType status,
                                  List<FileChange> files) {
    }

    /**
     * The changed projects the user may read, and how many the user may not.
     */
    private record Changed(List<ChangedProject> readable, int hidden) {
    }
}
