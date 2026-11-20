package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * A project the {@code rules.xml} of another project depends on.
 *
 * <p>A dependency is declared by name, and the name may lead nowhere: the project it refers to can be
 * absent from the workspace, or out of reach for this user. Such a dependency is still part of what the
 * project declares, so it is kept with no project of its own rather than dropped.
 *
 * <p>A dependency is transitive when another dependency declares it instead of the project itself. Compiling
 * the project needs both kinds, so both are reported and the flag tells them apart.
 *
 * <p>A dependency may also name the Maven coordinates of the project's archive. OpenL Studio compiles the archive
 * downloaded from the Maven repository while the user has the project closed or the workspace has none.
 *
 * @param name          the name the dependency is declared by
 * @param project       the project it refers to, {@code null} when the workspace has no such project
 * @param transitive    whether the project reaches this dependency through another one instead of declaring it
 * @param mavenArtifact the Maven coordinates the dependency names, {@code null} when it names none
 * @param conflicts     the other dependencies of the name, which ask for other coordinates and are left out: one project
 *                      of a name is compiled
 */
public record ProjectDependency(String name,
                                @Nullable RulesProject project,
                                boolean transitive,
                                @Nullable String mavenArtifact,
                                List<Request> conflicts) {

    public ProjectDependency(String name,
                             @Nullable RulesProject project,
                             boolean transitive,
                             @Nullable String mavenArtifact) {
        this(name, project, transitive, mavenArtifact, List.of());
    }

    /** The same dependency, told of one more dependency of its name that is left out. */
    public ProjectDependency withConflict(Request conflict) {
        var all = new ArrayList<>(conflicts);
        all.add(conflict);
        return new ProjectDependency(name, project, transitive, mavenArtifact, List.copyOf(all));
    }

    /**
     * A dependency of a project, as the project declares it.
     *
     * @param requestedBy   the project declaring it
     * @param mavenArtifact the Maven coordinates it names, {@code null} when it names none
     */
    public record Request(String requestedBy, @Nullable String mavenArtifact) {
    }
}
