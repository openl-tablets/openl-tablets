package org.openl.studio.projects.service;

import jakarta.annotation.Nullable;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * A project the {@code rules.xml} of another project depends on.
 *
 * <p>A dependency is declared by name, and the name may lead nowhere: the project it refers to can be
 * absent from the workspace, or out of reach for this user. Such a dependency is still part of what the
 * project declares, so it is kept with no project of its own rather than dropped.
 *
 * @param name    the name the dependency is declared by
 * @param project the project it refers to, {@code null} when the workspace has no such project
 */
public record ProjectDependency(String name, @Nullable RulesProject project) {
}
