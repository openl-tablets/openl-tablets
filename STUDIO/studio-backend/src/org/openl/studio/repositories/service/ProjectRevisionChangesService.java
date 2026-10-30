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
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
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
 * <p>Both revisions are told as the history of the project tells them. Only the files inside the project folder are
 * told, by their paths inside the project, ordered as the Merge Summary orders them. A repository with commits compares trees, so the answer costs about the same however large the
 * files are. A repository without commits reads both revisions of the project and compares the content of each file.
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
     * The files of the project the revision changed.
     *
     * @param project  the project the revisions belong to
     * @param revision the revision whose changes are told
     * @param base     the earlier revision to compare it with, or nothing for the one it was made on
     * @return the changed files with the revisions compared
     * @throws NotFoundException when the project has never been published or the repository has no such revision
     * @throws IOException       when the repository cannot be read
     */
    public ProjectRevisionChanges changesOf(RulesProject project, String revision, @Nullable String base)
            throws IOException {
        if (project.isLocalOnly()) {
            // Never published, so no repository holds a revision of it.
            throw new NotFoundException("project.revision.message", revision);
        }
        try {
            var repository = project.getDesignRepository();
            return repository.supports().branches()
                    ? byTrees((BranchRepository) repository, project.getRealPath(), revision, base)
                    : byContent(project, revision, base);
        } catch (FileNotFoundException e) {
            log.debug("Project '{}' has no revision '{}'.", project.getName(), revision, e);
            throw new NotFoundException("project.revision.message", revision);
        }
    }

    private ProjectRevisionChanges byTrees(BranchRepository repository,
                                           String folder,
                                           String revision,
                                           @Nullable String base) throws IOException {
        var changes = repository.listChanges(folder, base, revision);
        var files = changes.changes()
                .stream()
                .filter(change -> change.path().startsWith(folder + '/'))
                .map(change -> new ProjectRevisionChanges.ChangedFile(FilePaths.inside(folder, change.path()),
                        ChangeType.of(change.kind())))
                .sorted(FILE_ORDER)
                .toList();
        return answer(repository, changes.from(), changes.to(), files);
    }

    private ProjectRevisionChanges byContent(RulesProject project, String revision, @Nullable String base)
            throws IOException {
        // The history lists the revisions of the project from the first to the last.
        var history = project.getDesignRepository().listHistory(project.getDesignFolderName());
        var later = indexOf(history, revision);
        var earlier = base != null ? history.get(indexOf(history, base)) : previous(history, later);
        var after = contentOf(project, revision);
        var before = earlier == null ? Map.<String, String>of() : contentOf(project, earlier.getVersion());
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
        return answer(project.getDesignRepository(), earlier, history.get(later), files);
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

    /** The changed files with the two revisions, told as the history of the repository tells them. */
    private ProjectRevisionChanges answer(Repository repository,
                                          @Nullable FileData from,
                                          FileData to,
                                          List<ProjectRevisionChanges.ChangedFile> files) {
        var history = historyMappers.getObject(repository);
        return new ProjectRevisionChanges(from == null ? null : history.revisionOf(from), history.revisionOf(to), files);
    }

    /**
     * The files of the project at a revision, each by its path inside the project with a digest of its content. A
     * revision that deleted the project holds no file.
     *
     * @throws NotFoundException when the repository no longer lists the project at its path
     */
    private Map<String, String> contentOf(RulesProject project, String revision) throws IOException {
        var atRevision = designTimeRepository.getProjectByPath(project.getDesignRepository().getId(),
                null,
                project.getRealPath(),
                revision);
        if (atRevision == null) {
            throw new NotFoundException("project.revision.message", revision);
        }
        var files = new HashMap<String, String>();
        if (!atRevision.isDeleted()) {
            collect(atRevision, atRevision.getArtefacts(), files);
        }
        return files;
    }

    private static void collect(AProject project,
                                Collection<? extends AProjectArtefact> artefacts,
                                Map<String, String> files) throws IOException {
        for (var artefact : artefacts) {
            if (artefact instanceof AProjectFolder folder) {
                collect(project, folder.getArtefacts(), files);
            } else if (artefact instanceof AProjectResource resource) {
                // An artefact carries the path of the project before its own.
                files.put(FilePaths.inside(project.getArtefactPath().getStringValue(),
                        resource.getArtefactPath().getStringValue()), digestOf(resource));
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
