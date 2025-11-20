package org.openl.studio.projects.mcp.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.mcp.McpController;
import org.openl.studio.projects.converter.Base64ProjectConverter;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ProjectStatusToSet;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.history.ProjectHistoryItem;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.model.tests.TestsExecutionSummaryResponseMapper;
import org.openl.studio.projects.service.ProjectCriteriaQueryFactory;
import org.openl.studio.projects.service.ProjectTableCriteriaQueryFactory;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tests.ProjectTestsExecutionProgressListener;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.util.StringUtils;

@McpController
public class ProjectsMcpToolController {

    private static final String TOOL_PREFIX = "projects";

    private final WorkspaceProjectService projectService;
    private final ProjectCriteriaQueryFactory projectCriteriaQueryFactory;
    private final ProjectTableCriteriaQueryFactory projectTableCriteriaQueryFactory;
    private final Base64ProjectConverter base64ProjectConverter;
    private final ProjectHistoryService projectHistoryService;
    private final TestsExecutorService testsExecutorService;

    public ProjectsMcpToolController(WorkspaceProjectService projectService,
                                     ProjectCriteriaQueryFactory projectCriteriaQueryFactory,
                                     ProjectTableCriteriaQueryFactory projectTableCriteriaQueryFactory,
                                     Base64ProjectConverter base64ProjectConverter,
                                     ProjectHistoryService projectHistoryService,
                                     TestsExecutorService testsExecutorService) {
        this.projectService = projectService;
        this.projectCriteriaQueryFactory = projectCriteriaQueryFactory;
        this.projectTableCriteriaQueryFactory = projectTableCriteriaQueryFactory;
        this.base64ProjectConverter = base64ProjectConverter;
        this.projectHistoryService = projectHistoryService;
        this.testsExecutorService = testsExecutorService;
    }

    @Tool(name = TOOL_PREFIX + "-list", description = "Returns all projects from specified design repository, filtered by additional criteria.\nReturns project names, their statuses, and associated design repositories.\nUse this to discover all available projects before accessing specific project details.")
    public List<ProjectViewModel> listProjects(@ToolParam(description = "Project status to filter by", required = false)
                                               ProjectStatus status,
                                               @ToolParam(description = "Design repository identifier")
                                               String repository) {
        var query = projectCriteriaQueryFactory.build(Map.of(), status, repository);
        return projectService.getProjects(query);
    }

    @Tool(name = TOOL_PREFIX + "-get-details", description = "Gets comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status. Use this to understand project organization before making changes.")
    public ProjectViewModel getProject(@ToolParam(description = "Project identifier")
                                       String projectId) {
        var project = base64ProjectConverter.convert(projectId);
        return projectService.getProject(project);
    }

    @Tool(name = TOOL_PREFIX + "-change-status", description = "Updates project status with safety checks for unsaved changes. Unified tool for all project state transitions: opening, closing, saving, or switching branches. Status behavior: OPENED (open for editing), CLOSED (close). Prevents accidental data loss by requiring explicit confirmation when closing EDITING projects. Use cases: 1) Open: {status: 'OPENED'}, 2) Save and close: {status: 'CLOSED', comment: 'changes'}, 3) Save only: {comment: 'intermediate save'}, 4) Switch branch: {branch: 'develop'}")
    public void updateProjectStatus(@ToolParam(description = "Project identifier")
                                    String projectId,
                                    @ToolParam(description = "Project status to set", required = false)
                                    ProjectStatusToSet status,
                                    @ToolParam(description = "Branch name to switching to. Supported only if project repository supports branch feature.", required = false)
                                    String branch,
                                    @ToolParam(description = "Revision to switch to", required = false)
                                    String revision,
                                    @ToolParam(description = "Comment", required = false)
                                    String comment) {
        try {
            var project = base64ProjectConverter.convert(projectId);
            var updateStatusModel = new ProjectStatusUpdateModel();
            updateStatusModel.setStatus(switch (status) {
                case OPENED -> ProjectStatus.VIEWING;
                case CLOSED -> ProjectStatus.CLOSED;
            });
            updateStatusModel.setComment(comment);
            updateStatusModel.setRevision(revision);
            updateStatusModel.setBranch(branch);
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

    @Tool(name = TOOL_PREFIX + "-branch-create", description = "Creates a new branch in the project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used. Use this to manage project versions and isolate development work.")
    public void createProjectBranch(@ToolParam(description = "Project identifier")
                                    String projectId,
                                    @ToolParam(description = "New branch name to create")
                                    String branch,
                                    @ToolParam(description = "Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used.", required = false)
                                    String revision) {
        try {
            var project = base64ProjectConverter.convert(projectId);
            var createBranchModel = new CreateBranchModel();
            createBranchModel.setBranch(branch);
            createBranchModel.setRevision(revision);
            projectService.createBranch(project, createBranchModel);
            projectService.getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @Tool(name = TOOL_PREFIX + "-list-tables", description = "Lists all tables within a specified project, with optional filtering by table name.\nReturns table summaries including names and types.\nUse this to explore project contents before accessing specific tables.")
    public Collection<SummaryTableView> getProjectTables(@ToolParam(description = "Project identifier")
                                                         String projectId,
                                                         @ToolParam(description = "Table name to filter by", required = false)
                                                         String name) {
        var query = projectTableCriteriaQueryFactory.build(Map.of(), Set.of(), name);
        var project = base64ProjectConverter.convert(projectId);
        return projectService.getTables(project, query);
    }

    @Tool(name = TOOL_PREFIX + "-table-body", description = "Retrieves the full editable content of a specific table within a project.\nReturns all rows and columns of the table for viewing or editing.\nUse this to access and modify the detailed data of a particular table.")
    public EditableTableView getTable(@ToolParam(description = "Project identifier")
                                      String projectId,
                                      @ToolParam(description = "Table identifier")
                                      String tableId) {
        var project = base64ProjectConverter.convert(projectId);
        return (EditableTableView) projectService.getTable(project, tableId);
    }

    @Tool(name = TOOL_PREFIX + "-table-replace-body", description = "Replaces the entire content of a specific table within a project with new data.\nAccepts full table data to overwrite existing content.\nUse this to completely update or reset a table's data.")
    public void updateTable(@ToolParam(description = "Project identifier")
                            String projectId,
                            @ToolParam(description = "Table identifier")
                            String tableId,
                            @ToolParam(description = "Editable table data")
                            EditableTableView editTable) throws ProjectException {
        try {
            var project = base64ProjectConverter.convert(projectId);
            projectService.updateTable(project, tableId, editTable);
        } finally {
            projectService.getWebStudio().reset();
        }
    }

    @Tool(name = TOOL_PREFIX + "-table-append-rows", description = "Appends new rows to the end of a specific table within a project.\nAccepts partial table data containing only the new rows to be added.\nUse this to incrementally add data to an existing table without modifying its current content.")
    public void appendTable(@ToolParam(description = "Project identifier")
                            String projectId,
                            @ToolParam(description = "Table identifier")
                            String tableId,
                            @ToolParam(description = "Appendable table data")
                            AppendTableView editTable) throws ProjectException {
        try {
            var project = base64ProjectConverter.convert(projectId);
            projectService.appendTableLines(project, tableId, editTable);
        } finally {
            projectService.getWebStudio().reset();
        }
    }

    @Tool(name = TOOL_PREFIX + "-list-local-history", description = "Retrieves local history for a specified project.")
    public List<ProjectHistoryItem> getProjectHistory(@ToolParam(description = "Project identifier")
                                                      String projectId) {
        var project = base64ProjectConverter.convert(projectId);
        projectService.getProjectModel(project); // Ensure project is loaded in WebStudio
        return projectHistoryService.getProjectHistory(projectService.getWebStudio());
    }

    @Tool(name = TOOL_PREFIX + "-restore-version", description = "Restores a project to a specified version from its local history.")
    public void restoreProjectVersion(@ToolParam(description = "Project identifier")
                                      String projectId,
                                      @ToolParam(description = "History ID to restore")
                                      String historyId) throws Exception {
        var project = base64ProjectConverter.convert(projectId);
        projectService.getProjectModel(project); // Ensure project is loaded in WebStudio
        projectHistoryService.restore(projectService.getWebStudio(), historyId);
    }

    @Tool(name = TOOL_PREFIX + "-run-tests", description = "Runs tests for a specified project, with options to target specific tables and test ranges.\nReturns a summary of test execution results including passed/failed tests.\nUse this to validate project functionality through automated tests.")
    public TestsExecutionSummary runProjectTests(@ToolParam(description = "Project identifier")
                                                 String projectId,
                                                 @ToolParam(description = "Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run.", required = false)
                                                 String tableId,
                                                 @ToolParam(description = "Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run.", required = false)
                                                 String testRanges) throws ExecutionException, InterruptedException {
        var project = base64ProjectConverter.convert(projectId);

        var projectModel = projectService.getProjectModel(project);
        CompletableFuture<List<TestUnitsResults>> testTask;
        if (StringUtils.isBlank(tableId)) {
            testTask = testsExecutorService.runAll(ProjectTestsExecutionProgressListener.NOP, projectModel, false);
        } else {
            var table = projectModel.getTableById(tableId);
            if (table == null) {
                throw new NotFoundException("table.message");
            }
            if (StringUtils.isBlank(testRanges) && !OpenLTableUtils.isTestTable(table)) {
                testTask = testsExecutorService.runAllForTable(ProjectTestsExecutionProgressListener.NOP,
                        projectModel,
                        table,
                        false);
            } else {
                testTask = testsExecutorService.runSingle(ProjectTestsExecutionProgressListener.NOP,
                        projectModel,
                        table,
                        testRanges,
                        false);
            }
        }

        var mapper = new TestsExecutionSummaryResponseMapper(configureObjectMapper());
        return mapper.mapExecutionSummary(testTask.get());
    }

    /**
     * Configures ObjectMapper for JSON serialization of test results.
     * Uses the project's Jackson ObjectMapper factory if available.
     *
     * @return Configured ObjectMapper instance
     */
    private ObjectMapper configureObjectMapper() {
        try {
            var objectMapperFactory = projectService.getWebStudio().getCurrentProjectJacksonObjectMapperFactoryBean();
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (Exception e) {
            // Fallback to default ObjectMapper if factory configuration fails
            return new ObjectMapper();
        }
    }

}
