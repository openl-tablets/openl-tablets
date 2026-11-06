package org.openl.studio.projects.service;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.utils.AclPathUtils;

/**
 * The folders the projects of a repository live in, as their access lists are kept.
 *
 * <p>An access list belongs to a folder of a repository, not to a branch: every branch holding a project in the
 * folder shares it. A folder is held while a project of any branch lives in it or below it, whether or not the
 * caller may read that project.
 *
 * <p>An access list of a folder no project holds is what a removed project left behind. It must not pass to the
 * next project created in that folder.
 *
 * @param repositoryId the repository
 * @param held         the folders projects live in, as repository paths
 * @param complete     whether every project of the repository is known
 */
public record ProjectFolders(String repositoryId, NavigableSet<String> held, boolean complete) {

    /** Whether a project lives in a folder. */
    public enum Use {

        /** A project of some branch lives in the folder or below it. */
        HELD,

        /** No project of any branch lives in the folder. */
        FREE,

        /** No known project lives in the folder, but a branch not read yet may hold it. */
        UNKNOWN
    }

    /**
     * Reads the folders the projects of the repository live in, from the projects of every branch.
     *
     * @param designTimeRepository the projects of the repository
     * @param repositoryId         the repository
     */
    public static ProjectFolders of(DesignTimeRepository designTimeRepository, String repositoryId) {
        var listed = designTimeRepository.getProjectsInAllBranches(repositoryId);
        var held = listed.projects()
                .stream()
                .map(project -> AclPathUtils.buildRepositoryPath(repositoryId,
                        AclPathUtils.extractInternalPath(project)))
                .collect(Collectors.toCollection(TreeSet::new));
        return new ProjectFolders(repositoryId, held, listed.complete());
    }

    /**
     * Tells whether a project lives in the folder.
     *
     * <p>A project known to live there holds it even while not every project is known.
     *
     * @param folder the folder, as its access list is kept
     */
    public Use useOf(String folder) {
        var target = AclPathUtils.buildRepositoryPath(repositoryId, folder);
        var below = target + '/';
        // The folders below the target sort right after its path followed by a slash.
        var firstBelow = held.ceiling(below);
        var holdsBelow = firstBelow != null && firstBelow.startsWith(below);
        if (held.contains(target) || holdsBelow) {
            return Use.HELD;
        }
        return complete ? Use.FREE : Use.UNKNOWN;
    }
}
