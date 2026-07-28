package org.openl.studio.projects.service.files;

import java.util.Collection;

import lombok.RequiredArgsConstructor;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.ConflictException;

/**
 * Rejects a repository-mount modification of a path that belongs to a project locked for editing
 * by another user. The lock owner and paths outside any project are not restricted.
 *
 * <p>The check is advisory: the modification itself is not serialized, matching direct repository
 * commits. Projects are matched by their repository paths on the mount's branch; a project that
 * exists only on another branch is not seen and stays unchecked.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
class ProjectLockGuard {

    private final DesignTimeRepository designTimeRepository;
    private final LockEngine lockEngine;
    private final String repoId;
    private final String branch;
    private final String userName;

    /**
     * Verifies that no project affected by one of the paths is locked by another user.
     *
     * @throws ConflictException when an affected project is locked by another user
     */
    void requireUnlocked(Collection<String> paths) {
        for (AProject project : designTimeRepository.getProjects(repoId)) {
            var realPath = project.getRealPath();
            if (paths.stream().anyMatch(path -> affects(path, realPath))) {
                requireNotLockedByAnotherUser(project, realPath);
            }
        }
    }

    private void requireNotLockedByAnotherUser(AProject project, String realPath) {
        var lockInfo = lockEngine.getLockInfo(repoId, branch, realPath);
        if (lockInfo.isLocked() && !lockInfo.getLockedBy().equals(userName)) {
            throw new ConflictException("file.project.locked.message",
                    project.getBusinessName(), lockInfo.getLockedBy());
        }
    }

    /**
     * A path affects a project when it lies inside the project, or the project lies inside the
     * path — a modified folder affects its whole subtree. The empty path denotes the repository
     * root and affects every project.
     */
    private static boolean affects(String path, String projectPath) {
        return path.isEmpty() || isInside(path, projectPath) || isInside(projectPath, path);
    }

    private static boolean isInside(String path, String folder) {
        return path.equals(folder) || path.startsWith(folder + "/");
    }
}
