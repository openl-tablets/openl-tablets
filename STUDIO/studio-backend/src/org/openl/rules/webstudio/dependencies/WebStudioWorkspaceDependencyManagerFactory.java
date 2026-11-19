package org.openl.rules.webstudio.dependencies;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
                studio.getMavenDependencyResolver());
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
     * <p>A dependency is a project of the session with its name. When the session has none, a dependency with Maven
     * coordinates is the project downloaded from the Maven repository.
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
        return new Workspace(walk.workspace, walk.conflicts);
    }

    private @Nullable ProjectDescriptor findDependency(ProjectDependencyDescriptor dependency,
                                                       List<ProjectDescriptor> allProjects) {
        var name = dependency.getName();
        if (name == null) {
            // A library, not a project
            return null;
        }
        var found = allProjects.stream().filter(pd -> name.equals(pd.getName())).findFirst();
        if (found.isPresent()) {
            return found.get();
        }
        var resolver = studio.getMavenDependencyResolver();
        var downloaded = resolver == null ? null : resolver.projectOf(dependency).project();
        if (downloaded == null) {
            log.warn("Dependency '{}' is not found.", name);
        }
        return downloaded;
    }

    /**
     * The folder of the project, as an absolute normalized path, which tells two projects of one name apart.
     *
     * @return the folder, or {@code null} when the project has none
     */
    public static @Nullable Path folderOf(ProjectDescriptor project) {
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

    /** One walk from a project through its dependencies. */
    private final class Walk {
        private final ProjectDescriptor root;
        private final List<ProjectDescriptor> allProjects;
        private final Set<ProjectDescriptor> workspace = new LinkedHashSet<>();
        /** How each project of the workspace was asked for: by which project, with which coordinates. */
        private final Map<String, DependencyConflict.Request> requests = new HashMap<>();
        private final List<DependencyConflict> conflicts = new ArrayList<>();

        private Walk(ProjectDescriptor root, List<ProjectDescriptor> allProjects) {
            this.root = root;
            this.allProjects = allProjects;
            workspace.add(root);
            requests.put(root.getName(), new DependencyConflict.Request(null, null));
        }

        private void follow(ProjectDescriptor project) {
            if (project.getDependencies() == null) {
                return;
            }
            for (ProjectDependencyDescriptor dependency : project.getDependencies()) {
                var found = findDependency(dependency, allProjects);
                if (found != null) {
                    take(project, dependency, found);
                }
            }
        }

        /**
         * Takes a dependency in and follows it, unless a project of its name is taken already: the same project, as on
         * a cycle, needs nothing more, and another one is a conflict.
         */
        private void take(ProjectDescriptor requester, ProjectDependencyDescriptor dependency, ProjectDescriptor found) {
            var request = new DependencyConflict.Request(requester.getName(), dependency.getMavenArtifact());
            var taken = workspace.stream().filter(found::equals).findFirst().orElse(null);
            if (taken == null) {
                workspace.add(found);
                requests.put(found.getName(), request);
                follow(found);
            } else if (!Objects.equals(folderOf(taken), folderOf(found))) {
                var kept = requests.get(taken.getName());
                conflicts.add(new DependencyConflict(found.getName(), root.getName(), kept, request));
            }
        }
    }
}
