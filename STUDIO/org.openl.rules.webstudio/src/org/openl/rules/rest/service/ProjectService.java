package org.openl.rules.rest.service;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.model.tables.TableView;

/**
 * Project service API
 *
 * @author Vladyslav Pikus
 */
public interface ProjectService<T extends AProject> {

    /**
     * Get projects by criteria query
     *
     * @param query criteria query
     * @return list of projects
     */
    @Nonnull
    List<ProjectViewModel> getProjects(@Nonnull ProjectCriteriaQuery query);

    void updateProjectStatus(@Nonnull T project, @Nonnull ProjectStatusUpdateModel model) throws ProjectException;

    void close(@Nonnull T project) throws ProjectException;

    void open(@Nonnull T project, boolean openDependencies) throws ProjectException;

    void createBranch(@Nonnull T project, @Nonnull CreateBranchModel model) throws ProjectException;

    Collection<SummaryTableView> getTables(@Nonnull T project, @Nonnull ProjectTableCriteriaQuery query) throws ProjectException;

    TableView getTable(@Nonnull T project, @Nonnull String tableId) throws ProjectException;

    void updateTable(@Nonnull T project, @Nonnull String tableId, @Nonnull EditableTableView tableView) throws ProjectException;
}
