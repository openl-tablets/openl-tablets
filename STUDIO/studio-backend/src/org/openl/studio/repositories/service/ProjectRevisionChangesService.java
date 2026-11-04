package org.openl.studio.repositories.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RevisionChanges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.service.files.FilePaths;
import org.openl.studio.repositories.model.ProjectRevisionChanges;
import org.openl.util.HashingUtils;

/**
 * Tells which files of a project a revision of its history changed.
 *
 * <p>Without a base, a revision is compared with the one it was made on. A repository with commits compares the
 * revision with its parent commit: for a merge, the branch before the merge, so the answer is what the merge brought
 * in. A repository without commits compares it with the previous revision of the project. The first revision of a
 * project has nothing to be compared with, and every file it holds is added.
 *
 * <p>Without a revision, the working copy of the project is compared with the base: the files the user has now
 * against the files the base holds, whatever revision the working copy was opened at. The base is then required. In a
 * repository with commits, a revision is named by its id, a short id or a branch, which names the last revision of
 * the branch.
 *
 * <p>In a repository with commits, a base that is not in the history of the other side, as a revision of another
 * branch is not, comes with the revision both histories parted at. The working copy stands on the revision it was
 * opened at. Two revisions of one line of history, either one first, have none. With the revision both parted at,
 * the files that may conflict in a merge of both sides are told too. They are estimated from the revisions, without
 * merging, so the changes of the working copy not saved yet are not in it.
 *
 * <p>Both revisions are told as the history of the project tells them. Only the files inside the project folder are
 * told, by their paths inside the project, ordered as the Merge Summary orders them. A repository with commits
 * compares two revisions by their trees, so the answer costs about the same however large the files are. A repository
 * without commits reads both revisions of the project and compares the content of each file, and so does a comparison
 * with the working copy, in any repository.
 *
 * <p>Read access is not checked here: the project arrives already resolved for a caller allowed to read it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRevisionChangesService {

    private static final Comparator<ProjectRevisionChanges.ChangedFile> FILE_ORDER = Comparator
            .comparing(ProjectRevisionChanges.ChangedFile::path, ConflictGroup.FILE_ORDER);

    private final DesignTimeRepository designTimeRepository;
    private final ObjectProvider<HistoryRepositoryMapper> historyMappers;

    /**
     * The files of the project the revision changed, or the files the working copy changed since the base.
     *
     * @param project  the project the revisions belong to
     * @param revision the revision whose changes are told, or nothing for the working copy
     * @param base     the earlier revision to compare with: for a revision, nothing stands for the one it was made on
     * @param withConflicts whether the files that may conflict are estimated: the estimate reads the workbooks both
     *                      sides changed, so an answer that leaves them out is not made to wait for it
     * @return the changed files with the revisions compared; the revision is {@code null} for the working copy
     * @throws BadRequestException when neither a revision nor a base is named
     * @throws NotFoundException   when the project has never been published or the repository has no such revision
     * @throws IOException         when the repository cannot be read
     */
    public ProjectRevisionChanges changesOf(RulesProject project,
                                            @Nullable String revision,
                                            @Nullable String base,
                                            boolean withConflicts) throws IOException {
        if (revision == null && base == null) {
            throw new BadRequestException("project.history.changes.base.required.message");
        }
        var asked = revision == null ? base : revision;
        if (project.isLocalOnly()) {
            // Never published, so no repository holds a revision of it.
            throw new NotFoundException("project.revision.message", asked);
        }
        try {
            if (revision == null) {
                return inWorkingCopy(project, base, withConflicts);
            }
            var repository = project.getDesignRepository();
            return repository.supports().branches()
                    ? byTrees((BranchRepository) repository, project.getRealPath(), revision, base, withConflicts)
                    : byContent(project, revision, base);
        } catch (FileNotFoundException e) {
            log.debug("Project '{}' has no revision '{}'.", project.getName(), asked, e);
            throw new NotFoundException("project.revision.message", asked);
        }
    }

    /**
     * The files the working copy of the project changed since the base, by the content of each file. A base of
     * another history is told with the revision both parted at, and the files are the ones the working copy changed
     * since that revision, as a pull request lists them.
     *
     * @throws NotFoundException when the repository has no such revision of the project
     */
    private ProjectRevisionChanges inWorkingCopy(RulesProject project, String base, boolean withConflicts)
            throws IOException {
        var repository = project.getDesignRepository();
        var earlier = revisionNamed(project, base);
        var opened = openedRevisionOf(project);
        var common = opened == null ? null : commonOfWorkingCopy(project, earlier.getVersion(), opened);
        var after = new HashMap<String, String>();
        collect(project.getArtefacts(), after);
        var since = common == null ? earlier : common;
        var files = differences(contentOf(project, project.getBranch(), since.getVersion()), after);
        var conflicts = common == null || !withConflicts
                ? List.<String>of()
                : conflictsOf((BranchRepository) repository, project.getRealPath(), earlier.getVersion(), opened);
        return answer(repository, earlier, null, common, files, conflicts);
    }

    /**
     * The revision the working copy stands on, or {@code null} when it is not known. A project never opened at a
     * revision stands on the last one of its branch; one never saved, on none. Only a repository with commits tells
     * where histories part, so only there it is told.
     */
    private static @Nullable String openedRevisionOf(RulesProject project) {
        if (!project.getDesignRepository().supports().branches()) {
            return null;
        }
        return project.getHistoryVersion() != null ? project.getHistoryVersion() : project.getLastHistoryVersion();
    }

    /**
     * The files under the project folder that may conflict in a merge of two revisions, by their paths inside
     * the project, ordered as the Merge Summary orders them.
     */
    private static List<String> conflictsOf(BranchRepository repository,
                                            String folder,
                                            String first,
                                            String second) throws IOException {
        return repository.findConflicts(folder, first, second)
                .stream()
                .map(path -> FilePaths.inside(folder, path))
                .sorted(ConflictGroup.FILE_ORDER)
                .toList();
    }

    /**
     * The revision a base names, as the history of the project names it. A repository with commits reads a branch or
     * a short id as the commit it names, as it does for two revisions; any other repository knows a revision by its
     * own name only.
     *
     * @throws java.io.FileNotFoundException when a repository with commits has no such revision
     * @throws NotFoundException             when any other repository has no such revision of the project
     */
    private static FileData revisionNamed(RulesProject project, String base) throws IOException {
        var repository = project.getDesignRepository();
        var named = repository.supports().branches()
                ? commitNamed((BranchRepository) repository, project.getRealPath(), base)
                : repository.checkHistory(project.getDesignFolderName(), base);
        if (named == null) {
            throw new NotFoundException("project.revision.message", base);
        }
        return named;
    }

    /**
     * The commit a revision names, a branch or a short id included, as the history of the folder tells it. A revision
     * parts from itself at itself, so it is read without comparing it with anything.
     *
     * @throws java.io.FileNotFoundException when the repository has no such revision
     */
    private static @Nullable FileData commitNamed(BranchRepository repository, String folder, String revision)
            throws IOException {
        return repository.findCommonRevision(folder, revision, revision);
    }

    /**
     * The revision the working copy and the base parted at, when the base is not in the history of the working copy.
     * The working copy stands on the revision it was opened at, so a later revision of its branch parts from it there.
     *
     * @param opened the revision the working copy stands on
     */
    private static @Nullable FileData commonOfWorkingCopy(RulesProject project, String base, String opened)
            throws IOException {
        var repository = (BranchRepository) project.getDesignRepository();
        FileData common;
        try {
            common = repository.findCommonRevision(project.getRealPath(), base, opened);
        } catch (FileNotFoundException e) {
            // The revision the working copy was opened at is gone, as after a forced push: no common revision is known
            log.debug("Revision '{}' of project '{}' is gone.", opened, project.getName(), e);
            return null;
        }
        return common == null || base.equals(common.getVersion()) ? null : common;
    }

    private ProjectRevisionChanges byTrees(BranchRepository repository,
                                           String folder,
                                           String revision,
                                           @Nullable String base,
                                           boolean withConflicts) throws IOException {
        if (base == null) {
            var changes = repository.listChanges(folder, null, revision);
            return answer(repository, changes.from(), changes.to(), null, filesOf(changes, folder), List.of());
        }
        var earlier = commitNamed(repository, folder, base);
        var common = commonOfRevisions(repository, folder, base, revision);
        // A base of another history: the files the revision changed since both parted, as a pull request lists them
        var changes = repository.listChanges(folder, common == null ? base : common.getVersion(), revision);
        var conflicts = common == null || !withConflicts
                ? List.<String>of()
                : conflictsOf(repository, folder, base, revision);
        return answer(repository, earlier, changes.to(), common, filesOf(changes, folder), conflicts);
    }

    /** The files of the project folder a comparison of two revisions found changed, ordered by path. */
    private static List<ProjectRevisionChanges.ChangedFile> filesOf(RevisionChanges changes, String folder) {
        return changes.changes()
                .stream()
                .filter(change -> change.path().startsWith(folder + '/'))
                .map(change -> new ProjectRevisionChanges.ChangedFile(FilePaths.inside(folder, change.path()),
                        ChangeType.of(change.kind())))
                .sorted(FILE_ORDER)
                .toList();
    }

    /**
     * The revision two revisions parted at, when neither of them is in the history of the other one: two revisions
     * of one line of history are compared with each other only.
     */
    private static @Nullable FileData commonOfRevisions(BranchRepository repository,
                                                        String folder,
                                                        String base,
                                                        String revision) throws IOException {
        var common = repository.findCommonRevision(folder, base, revision);
        if (common == null) {
            return null;
        }
        // A revision made on the other one parted from it at that one: both are on one line of history
        var onOneLine = common.getVersion().equals(versionOf(repository, folder, base))
                || common.getVersion().equals(versionOf(repository, folder, revision));
        return onOneLine ? null : common;
    }

    /** The commit id a revision names, a branch or a short id included. */
    private static @Nullable String versionOf(BranchRepository repository, String folder, String revision)
            throws IOException {
        var named = commitNamed(repository, folder, revision);
        return named == null ? null : named.getVersion();
    }

    private ProjectRevisionChanges byContent(RulesProject project, String revision, @Nullable String base)
            throws IOException {
        // The history lists the revisions of the project from the first to the last.
        var history = project.getDesignRepository().listHistory(project.getDesignFolderName());
        var later = indexOf(history, revision);
        var earlier = base != null ? history.get(indexOf(history, base)) : previous(history, later);
        var after = contentOf(project, null, revision);
        var before = earlier == null ? Map.<String, String>of() : contentOf(project, null, earlier.getVersion());
        return answer(project.getDesignRepository(),
                earlier,
                history.get(later),
                null,
                differences(before, after),
                List.of());
    }

    /** The files added, modified and deleted from one content of the project to another, ordered by path. */
    private static List<ProjectRevisionChanges.ChangedFile> differences(Map<String, String> before,
                                                                        Map<String, String> after) {
        var files = new ArrayList<ProjectRevisionChanges.ChangedFile>();
        after.forEach((path, hash) -> {
            var digest = before.get(path);
            if (digest == null) {
                files.add(new ProjectRevisionChanges.ChangedFile(path, ChangeType.ADDED));
            } else if (!digest.equals(hash)) {
                files.add(new ProjectRevisionChanges.ChangedFile(path, ChangeType.MODIFIED));
            }
        });
        before.keySet()
                .stream()
                .filter(path -> !after.containsKey(path))
                .forEach(path -> files.add(new ProjectRevisionChanges.ChangedFile(path, ChangeType.DELETED)));
        files.sort(FILE_ORDER);
        return files;
    }

    /**
     * Where the history holds the given revision.
     *
     * @throws NotFoundException when the history has no such revision
     */
    private static int indexOf(List<FileData> history, String revision) {
        for (var i = 0; i < history.size(); i++) {
            if (revision.equals(history.get(i).getVersion())) {
                return i;
            }
        }
        throw new NotFoundException("project.revision.message", revision);
    }

    /** The revision of the history just before the given one, or {@code null} for the first one. */
    private static @Nullable FileData previous(List<FileData> history, int index) {
        return index == 0 ? null : history.get(index - 1);
    }

    /**
     * The changed files with the revisions compared, told as the history of the repository tells them.
     *
     * @param to     the later revision, or nothing for the working copy
     * @param common    the revision both parted at, or nothing when one is in the history of the other
     * @param conflicts the files that may conflict in a merge of both sides
     */
    private ProjectRevisionChanges answer(Repository repository,
                                          @Nullable FileData from,
                                          @Nullable FileData to,
                                          @Nullable FileData common,
                                          List<ProjectRevisionChanges.ChangedFile> files,
                                          List<String> conflicts) {
        var history = historyMappers.getObject(repository);
        return new ProjectRevisionChanges(from == null ? null : history.revisionOf(from),
                to == null ? null : history.revisionOf(to),
                common == null ? null : history.revisionOf(common),
                files,
                conflicts);
    }

    /**
     * The files of the project at a revision, each by its path inside the project with a digest of its content. A
     * revision that deleted the project holds no file.
     *
     * @param branch the branch the project is read on, or nothing for the repository as it is
     * @throws NotFoundException when the repository no longer lists the project at its path
     */
    private Map<String, String> contentOf(RulesProject project, @Nullable String branch, String revision)
            throws IOException {
        var atRevision = designTimeRepository.getProjectByPath(project.getDesignRepository().getId(),
                branch,
                project.getRealPath(),
                revision);
        if (atRevision == null) {
            throw new NotFoundException("project.revision.message", revision);
        }
        var files = new HashMap<String, String>();
        if (!atRevision.isDeleted()) {
            collect(atRevision.getArtefacts(), files);
        }
        return files;
    }

    /**
     * Adds the digest of every file of the project, by the path of the file inside the project.
     *
     * <p>The path is the one the project writes the file under. A working copy and a revision read from a repository
     * with mapped folders name the project by different folders, so the path a file carries is not cut by either.
     */
    private static void collect(Collection<? extends AProjectArtefact> artefacts, Map<String, String> files)
            throws IOException {
        for (var artefact : artefacts) {
            if (artefact instanceof AProjectFolder folder) {
                collect(folder.getArtefacts(), files);
            } else if (artefact instanceof AProjectResource resource) {
                files.put(resource.getInternalPath(), digestOf(resource));
            }
        }
    }

    private static String digestOf(AProjectResource resource) throws IOException {
        try (var content = resource.getContent()) {
            return HashingUtils.sha256Hex(content);
        } catch (ProjectException e) {
            throw new IOException(e);
        }
    }
}
