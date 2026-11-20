package org.openl.studio.deployment.model;

/**
 * Where a deployment takes a dependency project from when both a design repository and the Maven repository have it.
 *
 * <p>A project only one of them has comes from that one, whatever is chosen.
 *
 * @author Yury Molchan
 */
public enum DependencySource {
    /** The project of a design repository, the one the workspace resolves. */
    REPOSITORY,
    /** The archive the Maven coordinates of the dependency name. */
    MAVEN
}
