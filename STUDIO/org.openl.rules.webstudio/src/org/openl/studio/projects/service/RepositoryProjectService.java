package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * Implementation of project service for repository projects.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class RepositoryProjectService extends AbstractProjectService<AProject> {

    private final DesignTimeRepository designTimeRepository;

    public RepositoryProjectService(DesignTimeRepository designTimeRepository,
                                    @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                    ProjectIdentifierMapper projectIdentifierMapper,
                                    ProjectAccessService projectAccessService) {
        super(designRepositoryAclService, projectIdentifierMapper, projectAccessService);
        this.designTimeRepository = designTimeRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Stream<AProject> getProjects0(ProjectCriteriaQuery query) {
        Collection<? extends AProject> projects;
        if (query.hasRepositoryFilter()) {
            var repositoryIds = query.designRepositoryIds();
            if (repositoryIds.isEmpty()) {
                return Stream.empty();
            }
            return repositoryIds.stream()
                    .filter(repositoryId -> designRepositoryAclService.isGranted(repositoryId, null, List.of(BasePermission.READ)))
                    .map(designTimeRepository::getProjects)
                    .flatMap(Collection::stream)
                    .map(project -> (AProject) project);
        } else {
            projects = designTimeRepository.getProjects();
        }
        return (Stream<AProject>) projects.stream();
    }

    @Nonnull
    @Override
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        var filter = super.buildFilterCriteria(query);
        if (query.hasStatusFilter()) {
            filter = filter.and(project -> query.statuses().contains(statusOfRepositoryProject(project)));
        } else if (!query.includeDeleted()) {
            // doesn't show deleted to keep backward compatibility
            filter = filter.and(project -> !project.isDeleted());
        }
        return filter;
    }

    @Override
    @Nonnull
    protected Predicate<AProject> buildStatusFilterCriteria(ProjectCriteriaQuery query,
                                                            Map<AProject, ProjectStatus> statuses) {
        // Status is already resolved into the scope by buildFilterCriteria, so the base deleted-drop
        // must not be re-applied on top of it.
        return ALL_PROJECTS;
    }

    @Override
    protected Optional<ProjectStatus> statusOf(AProject project) {
        return Optional.of(statusOfRepositoryProject(project));
    }

    private static ProjectStatus statusOfRepositoryProject(AProject project) {
        if (project.isDeleted()) {
            return ProjectStatus.DELETED;
        }
        if (project.isModified()) {
            return ProjectStatus.EDITING;
        }
        if (!project.isLastVersion()) {
            return ProjectStatus.VIEWING_VERSION;
        }
        return ProjectStatus.CLOSED;
    }

}
