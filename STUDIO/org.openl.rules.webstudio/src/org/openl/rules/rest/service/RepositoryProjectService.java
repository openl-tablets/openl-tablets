package org.openl.rules.rest.service;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.permission.AclPermission;
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
                                    OpenLProjectService projectService) {
        super(designRepositoryAclService, projectService);
        this.designTimeRepository = designTimeRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Stream<AProject> getProjects0(ProjectCriteriaQuery query) {
        Collection<? extends AProject> projects;
        if (query.getRepositoryId().isPresent()) {
            var repositoryId = query.getRepositoryId().get();
            if (!designRepositoryAclService.isGranted(repositoryId, null, List.of(AclPermission.READ))) {
                return Stream.empty();
            }
            projects = designTimeRepository.getProjects(repositoryId);
        } else {
            projects = designTimeRepository.getProjects();
        }
        return (Stream<AProject>) projects.stream();
    }

    @Nonnull
    @Override
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        var filter = super.buildFilterCriteria(query);
        if (query.getStatus().isPresent()) {
            var status = query.getStatus().get();
            if (status == ProjectStatus.ARCHIVED) {
                filter = filter.and(AProject::isDeleted);
            } else if (status == ProjectStatus.EDITING) {
                filter = filter.and(project -> !project.isDeleted() && project.isModified());
            } else if (status == ProjectStatus.VIEWING_VERSION) {
                filter = filter.and(project -> !project.isDeleted() && !project.isLastVersion());
            } else if (status == ProjectStatus.CLOSED) {
                filter = filter
                        .and(project -> !project.isDeleted() && project.isLastVersion() && !project.isModified());
            }
        } else {
            // doesn't show deleted to keep backward compatibility
            filter = filter.and(project -> !project.isDeleted());
        }
        return filter;
    }

}
