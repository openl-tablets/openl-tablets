package org.openl.studio.projects.converter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.GoneException;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Resolves the project a request names by its id.
 *
 * <p>The id is looked up among the projects of the user's workspace, which holds only the projects the user may read.
 * A project listed before is reused while the design repository still holds it and its opened state is the one the
 * workspace records; otherwise the workspace lists its projects again.
 *
 * <p>An id no project has may have named a project before. A project whose folder moved is served under the id it
 * has now, and a deleted project answers {@code 410 Gone}, while the server runs (see {@link StaleProjects}).
 *
 * <p>Only an id names a project. An id of an earlier version of OpenL Studio and a project name are refused as an
 * unknown id is.
 *
 * @author Yury Molchan
 * @see ProjectIds
 */
@Component
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class ProjectIdConverter implements Converter<String, RulesProject> {

    @Qualifier("designRepositoryAclService")
    private final RepositoryAclService designRepositoryAclService;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @Override
    @Nonnull
    public RulesProject convert(String id) {
        return resolve(ProjectId.of(id));
    }

    /**
     * The project the id names, which the user may read.
     *
     * @throws NotFoundException when no project of the workspace has the id
     * @throws GoneException     when the project the id named was deleted
     * @throws SecurityException when the user may not read the project
     */
    public RulesProject resolve(ProjectId id) {
        var project = find(id);
        if (project == null) {
            throw new NotFoundException("project.identifier.message");
        }
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new SecurityException();
        }
        return project;
    }

    /**
     * The project the id names, or {@code null} when the workspace has none.
     *
     * <p>A text that is not an id names no project, and is answered without listing the projects again.
     *
     * @throws GoneException when the project the id named was deleted
     */
    public @Nullable RulesProject find(ProjectId id) {
        if (!ProjectIds.isId(id.value())) {
            return null;
        }
        var workspace = getUserWorkspace();
        var listed = workspace.getProjectById(id, false);
        if (listed.isPresent() && isCurrent(workspace, id, listed.get())) {
            return listed.get();
        }
        var designRepository = workspace.getDesignTimeRepository();
        Function<ProjectId, Optional<RulesProject>> listedProject = current -> workspace.getProjectById(current, false);
        // An id of a moved project leads to the project listed under its current id without listing every project
        // again; a project granted or added since the last listing is not listed yet.
        var moved = listed.isEmpty()
                ? StaleProjects.findMoved(designRepository, id, listedProject)
                : Optional.<RulesProject>empty();
        return moved.or(() -> workspace.getProjectById(id, true))
                .or(() -> StaleProjects.find(designRepository, id, listedProject))
                .orElse(null);
    }

    /**
     * Whether the listed project is still the one the repositories and the workspace hold.
     */
    private static boolean isCurrent(UserWorkspace workspace, ProjectId id, RulesProject project) {
        if (project.isLocalOnly()) {
            return true;
        }
        if (workspace.getDesignTimeRepository().getProjectById(id).isEmpty()) {
            return false;
        }
        var repositoryId = project.getDesignRepository().getId();
        var metainfo = workspace.getLocalWorkspace().getMetainfoRegistry().get(project.getBusinessName());
        var isOpened = metainfo != null && repositoryId.equals(metainfo.repositoryId());
        return project.isOpened() == isOpened;
    }
}
