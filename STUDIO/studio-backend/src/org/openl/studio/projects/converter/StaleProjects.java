package org.openl.studio.projects.converter;

import java.util.Optional;
import java.util.function.Function;

import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.StaleIds;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.ContentLocation;
import org.openl.studio.common.exception.GoneException;

/**
 * Serves a request that names a project by an id no project has any more.
 *
 * <p>A project whose folder moved is served under the id it has now, which the {@code Content-Location} header of
 * the answer carries when the address named the old id. A deleted project answers {@code 410 Gone}.
 *
 * @author Yury Molchan
 */
final class StaleProjects {

    private StaleProjects() {
    }

    /**
     * The project an old id leads to.
     *
     * @param repository the design repositories, which know what became of an id
     * @param id         the id a request names a project by
     * @param projectOf  reads the project an id names now, as the caller may see it
     * @return the project, or nothing when nothing is known of the id or the project cannot be read
     * @throws GoneException when the project was deleted
     */
    static <T> Optional<T> find(DesignTimeRepository repository,
                                ProjectId id,
                                Function<ProjectId, Optional<T>> projectOf) {
        return repository.getProjectIdChange(id).flatMap(change -> switch (change) {
            case StaleIds.Moved<ProjectId>(var current) -> servedAs(id, current, projectOf);
            case StaleIds.Deleted<ProjectId> deleted -> throw new GoneException("project.deleted.message");
        });
    }

    /**
     * The project an old id of a moved project leads to. An id of a deleted project leads to nothing here.
     *
     * @see #find(DesignTimeRepository, ProjectId, Function)
     */
    static <T> Optional<T> findMoved(DesignTimeRepository repository,
                                     ProjectId id,
                                     Function<ProjectId, Optional<T>> projectOf) {
        return repository.getProjectIdChange(id)
                .flatMap(change -> change instanceof StaleIds.Moved<ProjectId>(var current)
                        ? servedAs(id, current, projectOf)
                        : Optional.empty());
    }

    private static <T> Optional<T> servedAs(ProjectId stale,
                                            ProjectId current,
                                            Function<ProjectId, Optional<T>> projectOf) {
        return projectOf.apply(current).map(project -> {
            ContentLocation.servedAs(stale.value(), current.value());
            return project;
        });
    }
}
