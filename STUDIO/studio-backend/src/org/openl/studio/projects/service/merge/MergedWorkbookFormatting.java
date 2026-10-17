package org.openl.studio.projects.service.merge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergedWorkbook;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.projects.service.tables.UploadFormatting;

/**
 * Formats the workbooks a merge combines as an upload formats the workbooks it brings into a project: with the OpenL
 * table style, while tables are formatted on save.
 *
 * <p>Only the workbooks of the merged project are formatted, staged with the project the merge changes. Each module
 * holding such a workbook is compiled once. A workbook that cannot be formatted, for example because its module does
 * not compile, is left as the merge combined it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class MergedWorkbookFormatting {

    private final UploadFormatting uploadFormatting;

    /**
     * Formats the workbooks a merge combined before they are committed.
     *
     * @param project   the project the merge changes, as it stands before the merge
     * @param workbooks the workbooks the merge combined, by their paths from the root of the repository
     * @param studio    the session the merge was started in
     * @return the workbooks, by the same paths and in the same order, those of the project formatted
     */
    Map<String, byte[]> format(AProjectFolder project, Map<String, byte[]> workbooks, WebStudio studio) {
        return format(project, project.getRealPath(), workbooks, studio);
    }

    /** Formats the workbooks of a project found at the given path from the root of the repository. */
    private Map<String, byte[]> format(AProjectFolder project,
                                       String projectPath,
                                       Map<String, byte[]> workbooks,
                                       WebStudio studio) {
        var prefix = projectPath + "/";
        var ofProject = new LinkedHashMap<String, byte[]>();
        workbooks.forEach((path, workbook) -> {
            if (path.startsWith(prefix)) {
                ofProject.put(path.substring(prefix.length()), workbook);
            }
        });
        if (!uploadFormatting.formats(ofProject.keySet())) {
            return workbooks;
        }
        var formatted = new LinkedHashMap<>(workbooks);
        uploadFormatting.formatStaged(project, "", ofProject, false, studio)
                .forEach((path, workbook) -> formatted.put(prefix + path, workbook));
        return formatted;
    }

    /**
     * Formats the workbooks a committed merge combined, and commits the formatted ones on the branch the merge changed.
     *
     * <p>The workbooks are formatted after the merge, so the repository is not held while the project compiles. The
     * commit is based on the merge: a branch that took another commit since is merged with it. A formatting that fails
     * leaves the merge as it was committed, and the failure is logged.
     *
     * @param project    the merged project
     * @param repository the repository the merge was committed to
     * @param outcome    what the merge changed in the target branch
     * @param merge      who merged which branch into which, and in which session
     * @return what the merge and the formatting changed together, or the outcome itself when nothing was formatted
     */
    MergeOutcome formatCommitted(AProjectFolder project,
                                 Repository repository,
                                 MergeOutcome outcome,
                                 CommittedMerge merge) {
        var projectPath = project.getRealPath();
        var prefix = projectPath + "/";
        var paths = outcome.workbooks()
                .stream()
                .map(MergedWorkbook::path)
                .filter(path -> path.startsWith(prefix))
                .toList();
        if (!uploadFormatting.formats(paths)) {
            return outcome;
        }
        try {
            var target = rootOf(repository).forBranch(merge.target());
            var merged = new LinkedHashMap<String, byte[]>();
            for (var path : paths) {
                try (var file = target.readHistory(path, outcome.after())) {
                    merged.put(path, file.getStream().readAllBytes());
                }
            }
            var committed = new AProject(target, projectPath, outcome.after());
            var formatted = changed(merged, format(committed, projectPath, merged, merge.studio()));
            return formatted.isEmpty() ? outcome : commit(target, projectPath, outcome, formatted, merge);
        } catch (IOException | RuntimeException e) {
            log.warn("The workbooks merged from branch '{}' into branch '{}' are left unformatted.",
                    merge.source(),
                    merge.target(),
                    e);
            return outcome;
        }
    }

    /** The workbooks the formatting changed. */
    private static Map<String, byte[]> changed(Map<String, byte[]> merged, Map<String, byte[]> formatted) {
        return formatted.entrySet()
                .stream()
                .filter(workbook -> !Arrays.equals(workbook.getValue(), merged.get(workbook.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static MergeOutcome commit(BranchRepository target,
                                       String projectPath,
                                       MergeOutcome outcome,
                                       Map<String, byte[]> formatted,
                                       CommittedMerge merge) throws IOException {
        var folderData = new FileData();
        folderData.setName(projectPath);
        folderData.setVersion(outcome.after());
        folderData.setAuthor(merge.author());
        folderData.setComment("Format the workbooks merged from branch '%s'".formatted(merge.source()));
        folderData.setBranch(merge.target());
        var files = formatted.entrySet()
                .stream()
                .map(workbook -> new FileItem(workbook.getKey(), new ByteArrayInputStream(workbook.getValue())))
                .toList();
        var saved = target.save(folderData, files, ChangesetType.DIFF);
        var listed = outcome.changes().stream().map(FileChange::path).collect(Collectors.toSet());
        var changes = new ArrayList<>(outcome.changes());
        formatted.keySet()
                .stream()
                .filter(path -> !listed.contains(path))
                .forEach(path -> changes.add(new FileChange(path, FileChange.Kind.MODIFIED)));
        return new MergeOutcome(outcome.before(), saved.getVersion(), changes, outcome.workbooks());
    }

    /**
     * The project as a branch holds it, read by the paths from the root of the repository, as a merge names its
     * files.
     *
     * @param project    the project, on any branch
     * @param repository the repository of the project
     * @param branch     the branch to read the project from
     */
    static AProject onBranch(AProjectFolder project, Repository repository, String branch) throws IOException {
        return new AProject(rootOf(repository).forBranch(branch), project.getRealPath());
    }

    /** The repository behind its wrappers, which names its files by their paths from its root. */
    private static BranchRepository rootOf(Repository repository) {
        var root = repository;
        while (root instanceof RepositoryDelegate || root instanceof FolderMapper) {
            root = root instanceof RepositoryDelegate delegate
                    ? delegate.getOriginal()
                    : ((FolderMapper) root).getDelegate();
        }
        return (BranchRepository) root;
    }

    /**
     * A merge committed by a user.
     *
     * @param source the branch merged in
     * @param target the branch the merge changed
     * @param author who merged
     * @param studio the session the merge was started in
     */
    record CommittedMerge(String source, String target, UserInfo author, WebStudio studio) {
    }
}
