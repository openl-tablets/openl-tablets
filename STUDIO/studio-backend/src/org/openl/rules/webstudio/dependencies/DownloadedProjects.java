package org.openl.rules.webstudio.dependencies;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.dependencies.service.MavenCoordinates;

/**
 * The OpenL projects downloaded from the Maven repository a session opened, read-only, each in a repository of its own
 * that bears the id of the project: {@code maven:groupId:artifactId:version}.
 *
 * <p>A project is downloaded on request, whether a project of the workspace depends on it or not, and opened where it
 * is unpacked. It is compiled, run and traced as a project of the workspace is, and nothing writes it. It is kept
 * while it stays the same download; the new download a snapshot gets is opened anew.
 *
 * @author Yury Molchan
 */
public final class DownloadedProjects {

    private final Map<String, Opened> opened = new ConcurrentHashMap<>();

    /**
     * The project the id names, downloaded and opened.
     *
     * @param projectId the id of the project
     * @param resolver  the resolver of the Maven repository, or {@code null} while none is set
     * @param workspace the workspace of the user of the session
     * @return the project, or {@code null} when the id names no downloaded project
     * @throws NotFoundException when no Maven repository is set, or the project cannot be downloaded
     */
    public @Nullable RulesProject open(String projectId,
                                       @Nullable MavenDependencyResolver resolver,
                                       UserWorkspace workspace) {
        var coordinates = MavenCoordinates.ofProjectId(projectId);
        if (coordinates == null) {
            return null;
        }
        var descriptor = descriptorAt(coordinates, resolver);
        return opened.compute(projectId,
                (id, kept) -> kept != null && kept.isOf(descriptor) ? kept : openOf(id, descriptor, workspace))
                .project();
    }

    /**
     * The project the coordinates name, downloaded from the Maven repository.
     *
     * @param coordinates the coordinates of the project
     * @param resolver    the resolver of the Maven repository, or {@code null} while none is set
     * @return the description of the downloaded project
     * @throws NotFoundException when no Maven repository is set, or the project cannot be downloaded
     */
    private static ProjectDescriptor descriptorAt(MavenCoordinates coordinates,
                                                  @Nullable MavenDependencyResolver resolver) {
        if (resolver == null || !resolver.isEnabled()) {
            throw new NotFoundException("dependency.maven.no-repository.message", coordinates.toString());
        }
        var downloaded = resolver.projectAt(coordinates.toString());
        var descriptor = downloaded.project();
        if (descriptor == null) {
            var problem = downloaded.problem();
            throw new NotFoundException("dependency.maven.not-downloaded.message",
                    coordinates.toString(),
                    problem == null ? "" : problem.getSummary());
        }
        return descriptor;
    }

    /** Whether the repository is one a downloaded project is opened in. */
    public boolean holds(String repositoryId) {
        return opened.containsKey(repositoryId);
    }

    /**
     * The project opened in the repository, when the name is its name or the name of its folder.
     *
     * @return the project, or {@code null} when the repository holds no project of the name
     */
    public @Nullable RulesProject project(String repositoryId, String name) {
        var found = openedNamed(repositoryId, name);
        return found == null ? null : found.project();
    }

    /**
     * The description of the project opened in the repository, when the name is its name or the name of its folder.
     *
     * @return the description, or {@code null} when the repository holds no project of the name
     */
    public @Nullable ProjectDescriptor descriptor(String repositoryId, String name) {
        var found = openedNamed(repositoryId, name);
        return found == null ? null : found.descriptor();
    }

    private @Nullable Opened openedNamed(String repositoryId, String name) {
        var found = opened.get(repositoryId);
        return found != null && found.names(name) ? found : null;
    }

    private static Opened openOf(String projectId, ProjectDescriptor descriptor, UserWorkspace workspace) {
        var repository = new DownloadedProjectRepository(projectId,
                descriptor.getProjectFolder().getParent(),
                workspace.getLocalWorkspace().getMetainfoRegistry());
        var project = new DownloadedRulesProject(workspace.getUser(),
                repository,
                descriptor,
                workspace.getProjectsLockEngine());
        project.setProjectId(ProjectId.of(projectId));
        return new Opened(descriptor, project);
    }

    /** A downloaded project, as the session opened it. */
    private record Opened(ProjectDescriptor descriptor, RulesProject project) {

        /** Whether the project is opened from the download the descriptor describes. */
        boolean isOf(ProjectDescriptor download) {
            return descriptor.getProjectFolder().equals(download.getProjectFolder());
        }

        /** Whether the name is the name of the project or of its folder, which tell it apart as a project. */
        boolean names(String name) {
            return name.equals(descriptor.getName())
                    || name.equals(descriptor.getProjectFolder().getFileName().toString());
        }
    }
}
