package org.openl.studio.projects.service.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
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
}
