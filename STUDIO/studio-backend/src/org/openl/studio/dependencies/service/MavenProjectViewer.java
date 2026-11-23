package org.openl.studio.dependencies.service;

import static org.openl.rules.webstudio.dependencies.MavenDependencyResolver.namedMavenDependenciesOf;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.MavenDependencyResolver;
import org.openl.studio.dependencies.model.MavenProjectSummary;

/**
 * Lists the OpenL projects of the Maven repository the workspace of the client depends on.
 *
 * <p>A listed project is read, read-only, through the Projects API by the id
 * {@code maven:groupId:artifactId:version}.
 *
 * @author Yury Molchan
 */
@Service
public class MavenProjectViewer {

    /** The session of the client, which knows the projects of its workspace and the Maven repository. */
    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * The projects of the Maven repository the workspace depends on, as the projects of a repository are listed: every
     * one that a {@code rules.xml} of an opened project names by its coordinates, or by a name its {@code pom.xml}
     * gives the coordinates of, or that such a project names in turn, and that can be downloaded. A project of another
     * extension than {@code zip}, or with a classifier, is not listed: the coordinates of the list open the plain
     * archive.
     *
     * <p>Nothing is listed while no Maven repository is set.
     */
    public List<MavenProjectSummary> projects() {
        var studio = getWebStudio();
        var resolver = studio.getMavenDependencyResolver();
        if (resolver == null || !resolver.isEnabled()) {
            return List.of();
        }
        var found = new LinkedHashMap<MavenCoordinates, MavenProjectSummary>();
        var queue = new ArrayDeque<ProjectDependencyDescriptor>();
        // A downloaded project names by name the projects the pom.xml of the opened one depends on, as a compilation
        // finds them
        var pomProjects = new HashMap<String, String>();
        for (var project : studio.getAllProjects()) {
            var ofPom = resolver.projectsOfPom(studio.pomOf(project)).coordinatesByName();
            ofPom.forEach(pomProjects::putIfAbsent);
            queue.addAll(namedMavenDependenciesOf(project.getDependencies(), ofPom));
        }
        var seen = new HashSet<String>();
        while (!queue.isEmpty()) {
            var dependency = queue.poll();
            var artifact = MavenDependencyResolver.artifactOf(dependency.getMavenArtifact());
            var project = artifact == null || !seen.add(dependency.getMavenArtifact())
                    ? null
                    : resolver.projectOf(dependency).project();
            if (project != null) {
                if (isPlainArchive(artifact)) {
                    var coordinates = new MavenCoordinates(artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion());
                    found.putIfAbsent(coordinates,
                            new MavenProjectSummary(artifact.getGroupId(),
                                    artifact.getArtifactId(),
                                    artifact.getVersion(),
                                    project.getName()));
                }
                queue.addAll(namedMavenDependenciesOf(project.getDependencies(), pomProjects));
            }
        }
        return found.values()
                .stream()
                .sorted(Comparator.comparing(MavenProjectSummary::name)
                        .thenComparing(MavenProjectSummary::groupId)
                        .thenComparing(MavenProjectSummary::artifactId)
                        .thenComparing(MavenProjectSummary::version))
                .toList();
    }

    /**
     * Whether the artifact is a {@code zip} with no classifier: the project its coordinates open, which a project of
     * another extension or with a classifier is not.
     */
    private static boolean isPlainArchive(Artifact artifact) {
        return MavenDependencyResolver.ZIP.equals(artifact.getExtension()) && artifact.getClassifier().isEmpty();
    }
}
