package org.openl.studio.projects.converter;

import java.util.List;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Strategy that resolves a project identity (either a project ID or a project name) to candidate {@link RulesProject}s
 * within a {@link UserWorkspace}. Strategies are chained by {@link ProjectIdentityConverter}; the first strategy that
 * returns a non-empty list is authoritative. Multiple results indicate ambiguity and are reported as a conflict by the
 * converter.
 *
 * @author Vladyslav Pikus
 */
@FunctionalInterface
public interface ProjectResolveStrategy {

    /**
     * Resolve the given identity within the workspace.
     *
     * @param workspace user workspace to search in
     * @param identity  identity string — a project ID or a project name
     * @return matching projects, possibly empty
     */
    List<RulesProject> resolve(UserWorkspace workspace, String identity);
}
