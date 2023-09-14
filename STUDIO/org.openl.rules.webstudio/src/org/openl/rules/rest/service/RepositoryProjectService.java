package org.openl.rules.rest.service;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.model.tables.TableView;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
            if (!designRepositoryAclService.isGranted(repositoryId, null, List.of(AclPermission.VIEW))) {
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

    @Override
    public void updateProjectStatus(AProject project, ProjectStatusUpdateModel model) {
        throw new UnsupportedOperationException("Project status update is not supported for repository projects");
    }

    @Override
    public void close(AProject project) {
        throw new UnsupportedOperationException("Project close is not supported for repository projects");
    }

    @Override
    public void open(AProject project, boolean openDependencies) {
        throw new UnsupportedOperationException("Project open is not supported for repository projects");
    }

    @Override
    public void createBranch(AProject project, CreateBranchModel model) {
        throw new UnsupportedOperationException("Branch creation is not supported for repository projects");
    }

    @Override
    public Collection<SummaryTableView> getTables(AProject project, ProjectTableCriteriaQuery query) {
        throw new UnsupportedOperationException("Tables are not supported for repository projects");
    }

    @Override
    public TableView getTable(AProject project, String tableId) {
        throw new UnsupportedOperationException("Tables are not supported for repository projects");
    }

    @Override
    public void updateTable(AProject project, String tableId, EditableTableView tableView) {
        throw new UnsupportedOperationException("Tables are not supported for repository projects");
    }
}
