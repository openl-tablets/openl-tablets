package org.openl.rules.webstudio.projects.mcp.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springaicommunity.mcp.annotation.McpTool;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.resolver.Base64ProjectConverter;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.webstudio.projects.mcp.model.AppendProjectTableRequest;
import org.openl.rules.webstudio.projects.mcp.model.CreateProjectBranchRequest;
import org.openl.rules.webstudio.projects.mcp.model.GetProjectRequest;
import org.openl.rules.webstudio.projects.mcp.model.GetProjectTableRequest;
import org.openl.rules.webstudio.projects.mcp.model.ListProjectTablesRequest;
import org.openl.rules.webstudio.projects.mcp.model.ListProjectsRequest;
import org.openl.rules.webstudio.projects.mcp.model.UpdateProjectStatusRequest;
import org.openl.rules.webstudio.projects.mcp.model.UpdateProjectTableRequest;
import org.openl.rules.webstudio.projects.service.ProjectCriteriaQueryFactory;
import org.openl.rules.webstudio.projects.service.ProjectTableCriteriaQueryFactory;
import org.openl.studio.mcp.McpController;

@McpController
public class ProjectsMcpToolController {

    private final WorkspaceProjectService projectService;
    private final ProjectCriteriaQueryFactory projectCriteriaQueryFactory;
    private final ProjectTableCriteriaQueryFactory projectTableCriteriaQueryFactory;
    private final Base64ProjectConverter base64ProjectConverter;

    public ProjectsMcpToolController(WorkspaceProjectService projectService,
                                     ProjectCriteriaQueryFactory projectCriteriaQueryFactory,
                                     ProjectTableCriteriaQueryFactory projectTableCriteriaQueryFactory,
                                     Base64ProjectConverter base64ProjectConverter) {
        this.projectService = projectService;
        this.projectCriteriaQueryFactory = projectCriteriaQueryFactory;
        this.projectTableCriteriaQueryFactory = projectTableCriteriaQueryFactory;
        this.base64ProjectConverter = base64ProjectConverter;
    }

    @McpTool(name = "projects-list-all", description = "Returns all projects in OpenL Tablets matching the specified criteria.\nUse this to discover all available projects based on status, and repository.")
    public List<ProjectViewModel> listProjects(ListProjectsRequest request) {
        var query = projectCriteriaQueryFactory.build(Map.of(), request.status(), request.repository());
        return projectService.getProjects(query);
    }

    @McpTool(name = "projects-get", description = "Gets comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status. Use this to understand project organization before making changes.")
    public ProjectViewModel getProject(GetProjectRequest request) {
        var project = base64ProjectConverter.convert(request.projectId());
        return projectService.getProject(project);
    }

    @McpTool(name = "projects-update-status", description = "Updates project status with safety checks for unsaved changes. Unified tool for all project state transitions: opening, closing, saving, or switching branches. Status behavior: OPENED (open for editing, read-only if locked by another user), EDITING (has unsaved changes, auto-set by OpenL on first edit), VIEWING_VERSION (viewing outdated version after another user saved, need to re-open), CLOSED (closed and unlocked). Prevents accidental data loss by requiring explicit confirmation when closing EDITING projects. Use cases: 1) Open: {status: 'OPENED'}, 2) Save and close: {status: 'CLOSED', comment: 'changes'}, 3) Save only: {comment: 'intermediate save'}, 4) Switch branch: {branch: 'develop'}")
    public void updateProjectStatus(UpdateProjectStatusRequest request) {
        try {
            var project = base64ProjectConverter.convert(request.projectId());
            var updateStatusModel = getProjectStatusUpdateModel(request);
            projectService.updateProjectStatus(project, updateStatusModel);
            if (updateStatusModel.getStatus() != null
                    || updateStatusModel.getBranch().isPresent()
                    || updateStatusModel.getComment().isPresent()
                    || updateStatusModel.getRevision().isPresent()) {
                projectService.getWebStudio().reset();
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    private static ProjectStatusUpdateModel getProjectStatusUpdateModel(UpdateProjectStatusRequest request) {
        var updateStatusModel = new ProjectStatusUpdateModel();
        updateStatusModel.setStatus(switch (request.status()) {
            case OPENED -> ProjectStatus.VIEWING;
            case CLOSED -> ProjectStatus.CLOSED;
        });
        updateStatusModel.setComment(request.comment());
        updateStatusModel.setRevision(request.revision());
        updateStatusModel.setSelectedBranches(request.selectedBranches());
        updateStatusModel.setBranch(request.branch());
        return updateStatusModel;
    }

    @McpTool(name = "projects-create-branch", description = "Creates a new branch in the project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used. Use this to manage project versions and isolate development work.")
    public void createProjectBranch(CreateProjectBranchRequest request) {
        try {
            var project = base64ProjectConverter.convert(request.projectId());
            var createBranchModel = new CreateBranchModel();
            createBranchModel.setBranch(request.branch());
            createBranchModel.setRevision(request.revision());
            projectService.createBranch(project, createBranchModel);
            projectService.getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @McpTool(name = "projects-get-tables", description = "Gets project tables.")
    public Collection<SummaryTableView> getProjectTables(ListProjectTablesRequest request) {
        var query = projectTableCriteriaQueryFactory.build(Map.of(), Set.of(), request.name());
        var project = base64ProjectConverter.convert(request.projectId());
        return projectService.getTables(project, query);
    }

    @McpTool(name = "projects-get-table-body", description = "Gets the full table body by its name within a project.")
    public EditableTableView getTable(GetProjectTableRequest request) {
        var project = base64ProjectConverter.convert(request.projectId());
        return (EditableTableView) projectService.getTable(project, request.tableId());
    }

    @McpTool(name = "projects-update-table-body", description = "Updates the body of a specific table within a project.")
    public void updateTable(UpdateProjectTableRequest request) throws ProjectException {
        try {
            var project = base64ProjectConverter.convert(request.projectId());
            projectService.updateTable(project, request.tableId(), request.editTable());
        } finally {
            projectService.getWebStudio().reset();
        }
    }

    @McpTool(name = "projects-append-table-body", description = "Appends lines to the body of a specific table within a project.")
    public void appendTable(AppendProjectTableRequest request) throws ProjectException {
        try {
            var project = base64ProjectConverter.convert(request.projectId());
            projectService.appendTableLines(project, request.tableId(), request.editTable());
        } finally {
            projectService.getWebStudio().reset();
        }
    }
}
