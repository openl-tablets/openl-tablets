package org.openl.studio.projects.mcp.controller;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.repository.api.Offset;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageRequest;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.mcp.McpController;
import org.openl.studio.mcp.McpToolNameConstants;
import org.openl.studio.projects.converter.Base64ProjectConverter;
import org.openl.studio.projects.mcp.model.ProjectTag;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.ProjectStatusToSet;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.history.ProjectHistoryItem;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.model.tests.TestsExecutionSummaryResponseMapper;
import org.openl.studio.projects.service.ProjectCriteriaQuery;
import org.openl.studio.projects.service.ProjectTableCriteriaQuery;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tests.ProjectTestsExecutionProgressListener;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.util.StringUtils;

@McpController
@Validated
public class ProjectsMcpToolController {

    private final WorkspaceProjectService projectService;
    private final Base64ProjectConverter base64ProjectConverter;
    private final ProjectHistoryService projectHistoryService;
    private final TestsExecutorService testsExecutorService;

    public ProjectsMcpToolController(WorkspaceProjectService projectService,
                                     Base64ProjectConverter base64ProjectConverter,
                                     ProjectHistoryService projectHistoryService,
                                     TestsExecutorService testsExecutorService) {
        this.projectService = projectService;
        this.base64ProjectConverter = base64ProjectConverter;
        this.projectHistoryService = projectHistoryService;
        this.testsExecutorService = testsExecutorService;
    }

    @Tool(name = McpToolNameConstants.LIST_TOOL_PREFIX + "_projects", description = "List all projects with optional filters (repository, status, tag). Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field (format: 'repository-projectName') to use with other tools. Use this to discover and filter projects before opening them for editing.")
    public PageResponse<ProjectViewModel> listProjects(@ToolParam(description = "Project status to filter by", required = false)
                                                       ProjectStatus status,
                                                       @ToolParam(description = "Design repository identifier")
                                                       String repository,
                                                       @ToolParam(description = "Identifier of the project that the returned projects depend on.", required = false)
                                                       ProjectIdModel dependsOn,
                                                       @ToolParam(description = "Pagination parameters", required = false)
                                                       PageRequest pagination,
                                                       @ToolParam(description = "Project tags to filter by", required = false)
                                                       Set<ProjectTag> tags) {
        var queryBuilder = ProjectCriteriaQuery.builder()
                .repositoryId(repository)
                .status(status)
                .dependsOn(dependsOn);
        if (tags != null) {
            tags.forEach(tag -> queryBuilder.tag(tag.name(), tag.value()));
        }
        Pageable pageable;
        if (pagination != null) {
            pageable = Offset.of(pagination.offset(), pagination.limit());
        } else {
            pageable = Pageable.unpaged();
        }
        return projectService.getProjects(queryBuilder.build(), pageable);
    }

    @Tool(name = McpToolNameConstants.GET_TOOL_PREFIX + "_project", description = "Get comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status. Use this to understand project organization before making changes.")
    public ProjectViewModel getProject(@ToolParam(description = "Project identifier")
                                       String projectId) {
        var project = base64ProjectConverter.convert(projectId);
        return projectService.getProject(project);
    }

    @Tool(name = McpToolNameConstants.UPDATE_TOOL_PREFIX + "_project_status", description = "Updates project status with safety checks for unsaved changes. Unified tool for all project state transitions: opening, closing, saving, or switching branches. Status behavior: OPENED (open for editing), CLOSED (close). Prevents accidental data loss by requiring explicit confirmation when closing EDITING projects. Use cases: 1) Open: {status: 'OPENED'}, 2) Save {status:'OPENED', comment: 'changes'}, 3) Save and close: {status: 'CLOSED', comment: 'changes'}, 4) Save only: {comment: 'intermediate save'}, 5) Switch branch: {branch: 'develop'}")
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

    @Tool(name = McpToolNameConstants.CREATE_TOOL_PREFIX + "_project_branch", description = "Creates a new branch in the project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used. Use this to manage project versions and isolate development work.")
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

    @Tool(name = McpToolNameConstants.LIST_TOOL_PREFIX + "_project_tables", description = "Lists all tables within a specified project, with optional filtering by table name.\nReturns table summaries including names and types.\nUse this to explore project contents before accessing specific tables.")
    public Collection<SummaryTableView> getProjectTables(@ToolParam(description = "Project identifier")
                                                         String projectId,
                                                         @ToolParam(description = "Table name to filter by", required = false)
                                                         String name) {
        var query = ProjectTableCriteriaQuery.builder()
                .name(name)
                .build();
        var project = base64ProjectConverter.convert(projectId);
        return projectService.getTables(project, query);
    }

    @Tool(name = McpToolNameConstants.GET_TOOL_PREFIX + "_project_table", description = "Get detailed information about a specific table/rule. Returns table structure, signature, conditions, actions, dimension properties, and all row data. Use this to understand existing rules before modifying them.")
    public EditableTableView getTable(@ToolParam(description = "Project identifier")
                                      String projectId,
                                      @ToolParam(description = "Table identifier")
                                      String tableId) {
        var project = base64ProjectConverter.convert(projectId);
        return (EditableTableView) projectService.getTable(project, tableId);
    }

    @Tool(name = McpToolNameConstants.CREATE_TOOL_PREFIX + "_project_table", description = "Creates a new table in the specified module and sheet within a project. Provide complete table data including structure, fields, and initial rows. Use this to add new rules or data tables to your project.")
    public SummaryTableView createProjectTable(@ToolParam(description = "Project identifier")
                                               String projectId,
                                               @ToolParam(description = "Name of the module where the table will be created")
                                               @NotBlank
                                               String moduleName,
                                               @ToolParam(description = "Name of the sheet where the table will be created. If not provided, table name will be used as sheet name.")
                                               String sheetName,
                                               @ToolParam(description = "Table data to create")
                                               @NotNull
                                               @Valid
                                               EditableTableView table) throws ProjectException {
        var project = base64ProjectConverter.convert(projectId);
        var request = new CreateNewTableRequest(moduleName, sheetName, table);
        try {
            projectService.createNewTable(project, request);
        } finally {
            projectService.getWebStudio().reset();
        }
        var query = ProjectTableCriteriaQuery.builder().name(((TableView) table).name).build();
        return projectService.getTables(project, query).stream().findFirst().orElse(null);
    }

    @Tool(name = McpToolNameConstants.UPDATE_TOOL_PREFIX + "_project_table", description = "Update table content including conditions, actions, and data rows. CRITICAL: Must send the FULL table structure (not just modified fields). Required workflow: 1) Call get_table() to retrieve complete structure, 2) Modify the returned object (e.g., update rules array, add fields), 3) Pass the ENTIRE modified object to update_table(). Required fields: id, tableType, kind, name, plus type-specific fields (rules for SimpleRules, rows for Spreadsheet, fields for Datatype). Modifies table in memory (requires save_project to persist changes).")
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

    @Tool(name = McpToolNameConstants.APPEND_TOOL_PREFIX + "_project_table", description = "Append new rows/fields to an existing table. Used to add data to Datatype or Data tables without replacing the entire structure. Specify the table type and array of field definitions with names, types, and optional required/defaultValue properties. More efficient than update_table for simple additions. Modifies table in memory (requires save_project to persist changes).")
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

    @Tool(name = McpToolNameConstants.LIST_TOOL_PREFIX + "_project_local_changes", description = "Lists the local change history for a specified project.")
    public List<ProjectHistoryItem> getProjectHistory(@ToolParam(description = "Project identifier")
                                                      String projectId) {
        var project = base64ProjectConverter.convert(projectId);
        projectService.getProjectModel(project); // Ensure project is loaded in WebStudio
        return projectHistoryService.getProjectHistory(projectService.getWebStudio());
    }

    @Tool(name = McpToolNameConstants.RESTORE_TOOL_PREFIX + "_project_local_change", description = "Restores a project to a specified version from its local history.")
    public void restoreProjectVersion(@ToolParam(description = "Project identifier")
                                      String projectId,
                                      @ToolParam(description = "History ID to restore")
                                      String historyId) throws Exception {
        var project = base64ProjectConverter.convert(projectId);
        projectService.getProjectModel(project); // Ensure project is loaded in WebStudio
        projectHistoryService.restore(projectService.getWebStudio(), historyId);
    }

    @Tool(name = McpToolNameConstants.RUN_TOOL_PREFIX + "_project_tests", description = "Runs tests for a specified project, with options to target specific tables and test ranges.\nReturns a summary of test execution results including passed/failed tests.\nUse this to validate project functionality through automated tests.")
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
