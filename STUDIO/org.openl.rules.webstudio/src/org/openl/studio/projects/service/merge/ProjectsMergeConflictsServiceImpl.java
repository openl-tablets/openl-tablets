package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

@Service
public class ProjectsMergeConflictsServiceImpl implements ProjectsMergeConflictsService {

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo) {
        if (mergeConflictInfo.details() == null || mergeConflictInfo.details().getConflictedFiles().isEmpty()) {
            return List.of();
        }
        List<String> conflicts = new ArrayList<>(mergeConflictInfo.details().getConflictedFiles());
        Map<String, ConflictGroup> groups = new TreeMap<>((p1, p2) -> {
            if (p1.equals(p2)) {
                return 0;
            } else {
                // Put empty project name to be latest.
                if (p1.isEmpty()) {
                    return 1;
                }
                if (p2.isEmpty()) {
                    return -1;
                }
                return p1.compareToIgnoreCase(p2);
            }
        });
        var workspace = getUserWorkspace();
        String repositoryId = mergeConflictInfo.getRepositoryId();
        for (String conflict : conflicts) {
            Optional<RulesProject> projectByPath = workspace.getProjectByPath(repositoryId, conflict);
            String projectName;
            String projectPath;
            if (projectByPath.isPresent()) {
                RulesProject project = projectByPath.get();
                projectName = project.getName();
                projectPath = project.getRealPath();
            } else {
                projectName = "";
                projectPath = "";
            }
            var group = groups.computeIfAbsent(projectName, n -> new ConflictGroup(n, projectPath));
            group.addFile(conflict);
        }

        return new ArrayList<>(groups.values());
    }

    @Override
    public FileItem getConflictFileItem(MergeConflictInfo mergeConflict, String path, ConflictBase side) throws IOException {
        var conflictDetails = mergeConflict.details();
        if (!conflictDetails.getConflictedFiles().contains(path)) {
            throw new NotFoundException("project.merge.conflict.file.not.found", path);
        }
        var repository = getRepository(mergeConflict);
        var realPath = getRealPath(repository, path);
        var commitRev = switch (side) {
            case BASE -> conflictDetails.baseCommit();
            case OURS -> mergeConflict.isExportOperation()
                    ? conflictDetails.theirCommit()
                    : conflictDetails.yourCommit();
            case THEIRS -> mergeConflict.isExportOperation()
                    ? conflictDetails.yourCommit()
                    : conflictDetails.theirCommit();
        };
        var fileItem = repository.readHistory(realPath, commitRev);
        if (fileItem == null) {
            throw new NotFoundException("project.merge.conflict.file.revision.not.found", path, commitRev);
        }
        return fileItem;
    }

    private String getRealPath(Repository repository, String path) throws IOException {
        if (repository.supports().mappedFolders()) {
            return ((FolderMapper) repository).getRealPath(path);
        }
        return path;
    }

    private Repository getRepository(MergeConflictInfo mergeConflict) throws IOException {
        var project = mergeConflict.project();
        if (!mergeConflict.isMerging()) {
            return project.getDesignRepository();
        } else {
            String id = mergeConflict.getRepositoryId();
            return ((BranchRepository) getUserWorkspace().getDesignTimeRepository().getRepository(id))
                    .forBranch(mergeConflict.mergeBranchTo());
        }
    }
}
