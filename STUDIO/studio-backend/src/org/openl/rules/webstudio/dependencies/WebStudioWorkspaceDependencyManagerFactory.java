package org.openl.rules.webstudio.dependencies;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;

@RequiredArgsConstructor
@Slf4j
public class WebStudioWorkspaceDependencyManagerFactory {

    private final WebStudio studio;

    public WebStudioWorkspaceRelatedDependencyManager buildDependencyManager(ProjectDescriptor project) {
        var workspace = workspaceOf(project);
        var rootClassLoader = WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader();
        var manager = new WebStudioWorkspaceRelatedDependencyManager(workspace.projects(),
                rootClassLoader,
                false,
                studio.getExternalProperties(),
                studio.isAutoCompile(),
                studio.getMavenDependencyResolver(),
                studio::pomOf);
        manager.addConflicts(workspace.conflicts());
        return manager;
    }

    /**
     * The project and every project it depends on, directly or through another one.
     *
     * @see #workspaceOf(ProjectDescriptor)
     */
    public Set<ProjectDescriptor> resolveWorkspace(ProjectDescriptor project) {
        return workspaceOf(project).projects();
    }

    /**
     * The project and every project it depends on, directly or through another one, and the dependencies left out.
     *
     * <p>A dependency is a project of the session with its name. When the session has none, it is the project
     * downloaded from the Maven repository: the one of its name a {@code pom.xml} the walk went through depends on, as
     * the OpenL Maven Plugin finds it, else the one its Maven coordinates name.
     *
     * <p>One project of a name is compiled: the one the walk reaches first, through the dependencies in the order each
     * project declares them. A dependency that leads to another project of the name, such as another version of a
     * project downloaded from the Maven repository, is left out and told as a conflict.
     *
     * <p>The projects of the session are read once for the whole walk, so every dependency is found in the same
     * reading of them, even when the session reads its projects again meanwhile.
     */
    public Workspace workspaceOf(ProjectDescriptor project) {
        var walk = new Walk(project, studio.getAllProjects());
        walk.follow(project);
        return new Workspace(walk.projects(), walk.conflicts);
    }

    private @Nullable ProjectDescriptor findDependency(ProjectDependencyDescriptor dependency,
                                                       Map<String, ProjectDescriptor> allProjects,
                                                       Map<String, String> pomProjects) {
        var name = dependency.getName();
        if (name == null) {
            // A library, not a project
            return null;
        }
        var found = allProjects.get(name);
        if (found != null) {
            return found;
        }
        var resolver = studio.getMavenDependencyResolver();
        var downloaded = resolver == null
                ? null
                : resolver.projectOf(MavenDependencyResolver.withPomCoordinates(dependency, pomProjects)).project();
        if (downloaded == null) {
            log.warn("Dependency '{}' is not found.", name);
        }
        return downloaded;
    }

    /**
     * Whether both are one project: of the same name, in the same folder. Two projects of one name, such as two
     * versions of a project downloaded from the Maven repository, are different projects.
     */
    public static boolean isSameProject(ProjectDescriptor one, ProjectDescriptor other) {
        return one.getName().equals(other.getName()) && Objects.equals(folderOf(one), folderOf(other));
    }

    /** The folder of the project, as an absolute normalized path, or {@code null} when the project has none. */
    private static @Nullable Path folderOf(ProjectDescriptor project) {
        var folder = project.getProjectFolder();
        return folder == null ? null : folder.toAbsolutePath().normalize();
    }

    /**
     * What a compilation of a project takes in.
     *
     * @param projects  the project first, then every project it depends on
     * @param conflicts the dependencies left out, as another project of their name is compiled
     */
    public record Workspace(Set<ProjectDescriptor> projects, List<DependencyConflict> conflicts) {
    }

    /** A project of the workspace, and how it was asked for: by which project, with which coordinates. */
    private record Taken(ProjectDescriptor project, DependencyConflict.Request request) {
    }

    /** One walk from a project through its dependencies. */
    private final class Walk {
        private final ProjectDescriptor root;
        /** The projects of the session by name; the first of a name wins, as the session lists them. */
        private final Map<String, ProjectDescriptor> allProjects = new HashMap<>();
        /** The projects of the workspace by name, in the order the walk takes them. */
        private final Map<String, Taken> taken = new LinkedHashMap<>();
        /** The coordinates of the projects the {@code pom.xml} files the walk went through depend on, by name. */
        private final Map<String, String> pomProjects = new HashMap<>();
        private final List<DependencyConflict> conflicts = new ArrayList<>();

        private Walk(ProjectDescriptor root, List<ProjectDescriptor> allProjects) {
            this.root = root;
            allProjects.forEach(project -> this.allProjects.putIfAbsent(project.getName(), project));
            taken.put(root.getName(), new Taken(root, new DependencyConflict.Request(null, null)));
        }

        private Set<ProjectDescriptor> projects() {
            var projects = new LinkedHashSet<ProjectDescriptor>();
            taken.values().forEach(project -> projects.add(project.project()));
            return projects;
        }

        private void follow(ProjectDescriptor project) {
            if (project.getDependencies() == null) {
                return;
            }
            var resolver = studio.getMavenDependencyResolver();
            if (resolver != null) {
                resolver.projectsOfPom(studio.pomOf(project)).coordinatesByName().forEach(pomProjects::putIfAbsent);
            }
            for (ProjectDependencyDescriptor dependency : project.getDependencies()) {
                var found = findDependency(dependency, allProjects, pomProjects);
                if (found != null) {
                    take(project, dependency, found);
                }
            }
        }

        /**
         * Takes a dependency in and follows it, unless a project of its name is taken already: the same project, as on
         * a cycle, needs nothing more, and another one is a conflict.
         */
        private void take(ProjectDescriptor requester,
                          ProjectDependencyDescriptor dependency,
                          ProjectDescriptor found) {
            var request = new DependencyConflict.Request(requester.getName(), dependency.getMavenArtifact());
            var kept = taken.get(found.getName());
            if (kept == null) {
                taken.put(found.getName(), new Taken(found, request));
                follow(found);
            } else if (!isSameProject(kept.project(), found)) {
                conflicts.add(new DependencyConflict(found.getName(), root.getName(), kept.request(), request));
            }
        }
    }
}
