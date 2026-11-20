package org.openl.studio.deployment.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.openl.message.OpenLMessage;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.dependencies.MavenDependencyResolver;
import org.openl.rules.webstudio.web.repository.DeploymentExtras;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.deployment.model.DependencySource;
import org.openl.studio.projects.service.ProjectDependency;
import org.openl.studio.projects.service.ProjectDependencyResolver;

/**
 * Works out what a deployment of a project holds: the dependency projects, each taken from a design repository or
 * from the Maven repository, and the libraries the projects name by Maven coordinates.
 *
 * <p>The dependencies are followed from project to project. A dependency project both sources have comes from the one
 * chosen; one only a source has comes from that source. A project downloaded from the Maven repository carries its
 * libraries already, as the OpenL Maven Plugin packages them; the libraries of the other projects are downloaded.
 *
 * <p>A project that is itself downloaded from the Maven repository, opened read-only, is deployed as it is unpacked,
 * with the libraries it carries, and its dependencies are followed as those of any project.
 *
 * <p>Nothing is deployed when something the deployment needs cannot be downloaded: the deployment would not run.
 * While no Maven repository is set, the plan holds the projects of the design repositories only, as before.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class MavenDeploymentPlanner {

    private final ProjectDependencyResolver dependencyResolver;
    private final MavenDependencyResolver mavenResolver;

    /**
     * The plan of a deployment of the project.
     *
     * @throws ConflictException when a library or a dependency project the deployment needs cannot be downloaded
     */
    public Plan plan(RulesProject project, DependencySource source) {
        var walk = new Walk(source, dependencyResolver.getDependencies(project));
        walk.seen.add(project.getBusinessName());
        if (project.isReadOnly()) {
            // Downloaded from the Maven repository: deployed as it is unpacked, with the libraries it carries
            walk.mavenProjects.put(project.getBusinessName(), folderOf(project));
        } else {
            walk.repositoryProjects.add(project);
        }
        walk.follow(walk.rootDependencies);
        var libraries = new LinkedHashMap<String, List<Path>>();
        for (var deployed : walk.repositoryProjects) {
            var found = mavenResolver.librariesNamed(dependencyResolver.getLibraries(deployed));
            walk.problems.addAll(found.messages().stream().map(OpenLMessage::getSummary).toList());
            if (!found.files().isEmpty()) {
                libraries.put(deployed.getBusinessName(), found.files());
            }
        }
        if (!walk.problems.isEmpty()) {
            throw new ConflictException("project.deploy.maven.message", String.join(" ", walk.problems));
        }
        return new Plan(walk.repositoryProjects, new DeploymentExtras(libraries, walk.mavenProjects));
    }

    /** The folder the project is opened in. */
    private static Path folderOf(RulesProject project) {
        return project.getLocalRepository().getRoot().resolve(project.getFolderPath());
    }

    /** One plan being worked out. */
    private final class Walk {
        private final DependencySource source;
        private final List<ProjectDependency> rootDependencies;
        /** The projects of the design repositories the walk from the deployed project resolves, by name. */
        private final Map<String, RulesProject> resolved = new HashMap<>();
        private final Set<String> seen = new HashSet<>();
        private final List<RulesProject> repositoryProjects = new ArrayList<>();
        private final Map<String, Path> mavenProjects = new LinkedHashMap<>();
        private final List<String> problems = new ArrayList<>();

        private Walk(DependencySource source, List<ProjectDependency> rootDependencies) {
            this.source = source;
            this.rootDependencies = rootDependencies;
            rootDependencies.stream()
                    .filter(dependency -> dependency.project() != null)
                    .forEach(dependency -> resolved.putIfAbsent(dependency.name(), dependency.project()));
        }

        /**
         * Takes in what a project of a design repository declares itself, in the order it declares it, each with what
         * it depends on before the next one: the order a compilation reaches them in, so the project of a name the
         * deployment holds is the one the project is compiled with.
         */
        private void follow(Collection<ProjectDependency> dependencies) {
            takeAll(dependencies.stream().filter(dependency -> !dependency.transitive()).toList());
        }

        private void takeAll(List<ProjectDependency> dependencies) {
            for (var dependency : dependencies) {
                if (seen.add(dependency.name())) {
                    visit(dependency);
                }
            }
        }

        private void visit(ProjectDependency dependency) {
            var repositoryProject = dependency.project() != null
                    ? dependency.project()
                    : resolved.get(dependency.name());
            var coordinates = dependency.mavenArtifact();
            if (coordinates != null && mavenResolver.isEnabled()
                    && (source == DependencySource.MAVEN || repositoryProject == null)) {
                download(dependency.name(), coordinates);
            } else if (repositoryProject != null) {
                repositoryProjects.add(repositoryProject);
                follow(dependencyResolver.getDependencies(repositoryProject));
            }
            // A dependency no source has is left to the deployment to report, as before
        }

        private void download(String name, String coordinates) {
            var descriptor = new ProjectDependencyDescriptor();
            descriptor.setName(name);
            descriptor.setMavenArtifact(coordinates);
            var downloaded = mavenResolver.projectOf(descriptor);
            var project = downloaded.project();
            if (project == null) {
                problems.add(downloaded.problem() == null ? coordinates : downloaded.problem().getSummary());
            } else {
                mavenProjects.put(name, project.getProjectFolder());
                takeAll(dependenciesOf(project));
            }
        }

        /** The dependency projects a downloaded project declares, each as the workspace would resolve it. */
        private List<ProjectDependency> dependenciesOf(ProjectDescriptor project) {
            var declared = project.getDependencies();
            if (declared == null) {
                return List.of();
            }
            return declared.stream()
                    .filter(dependency -> dependency.getName() != null)
                    .map(dependency -> new ProjectDependency(dependency.getName(),
                            resolved.get(dependency.getName()),
                            true,
                            dependency.getMavenArtifact()))
                    .toList();
        }
    }

    /**
     * What a deployment of a project holds.
     *
     * @param repositoryProjects the projects taken from the design repositories: the deployed project first, unless it
     *                           is downloaded from the Maven repository, then the dependency projects
     * @param extras             what is taken from the Maven repository, the downloaded deployed project included
     */
    public record Plan(List<RulesProject> repositoryProjects, DeploymentExtras extras) {
    }
}
