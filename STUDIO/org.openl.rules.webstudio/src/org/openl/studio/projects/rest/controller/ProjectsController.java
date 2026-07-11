package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.TestResultExport;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.ProjectsPageResponse;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableIdView;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tests.TestExecutionSummaryQuery;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.model.tests.TestsExecutionSummaryResponseMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectCriteriaQuery;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectTableCriteriaQuery;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.graph.GraphDirection;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.TestExecutionStatus;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.rest.resolver.PaginationDefault;
import org.openl.util.StringUtils;

/**
 * Projects REST controller
 *
 * @author Vladyslav Pikus
 */
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects (BETA)", description = "Experimental projects API")
@Validated
@RequiredArgsConstructor
public class ProjectsController {

    private static final String TAGS_PREFIX = "tags.";
    private static final String PROPERTIES_PREFIX = "properties.";
    private static final String APPLICATION_XLSX_MEDIATYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final WorkspaceProjectService projectService;
    private final TestsExecutorService testsExecutorService;
    private final ExecutionTestsResultRegistry executionTestsResultRegistry;
    private final SocketProjectAllTestsExecutionProgressListenerFactory socketProjectAllTestsExecutionProgressListenerFactory;
    private final Environment environment;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final ProjectStatusMapper projectStatusMapper;
    private final ProjectTablesGraphService graphService;

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        return null;
    }

    @GetMapping
    @Operation(summary = "projects.list.summary")
    @Parameters({
            @Parameter(name = "status", description = "projects.list.param.status.desc", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {
                    "LOCAL",
                    "DELETED",
                    "OPENED",
                    "VIEWING_VERSION",
                    "EDITING",
                    "CLOSED"})),
            @Parameter(name = "repository", description = "projects.list.param.repository.desc", in = ParameterIn.QUERY),
            @Parameter(name = "dependsOn", description = "projects.list.param.depends-on.desc", in = ParameterIn.QUERY),
            @Parameter(name = "name", description = "projects.list.param.name.desc", in = ParameterIn.QUERY),
            @Parameter(name = "author", description = "projects.list.param.author.desc", in = ParameterIn.QUERY),
            @Parameter(name = "branch", description = "projects.list.param.branch.desc", in = ParameterIn.QUERY),
            @Parameter(name = "sort", description = "projects.list.param.sort.desc", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"name", "status", "updated"})),
            @Parameter(
                    name = "include",
                    description = "projects.list.param.include.desc",
                    in = ParameterIn.QUERY,
                    style = ParameterStyle.FORM,
                    explode = Explode.TRUE,
                    array = @ArraySchema(schema = @Schema(implementation = ProjectInclude.class))),
            @Parameter(name = "tags", description = "projects.list.param.tags.desc", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)
    })
    @JsonView(GenericView.Full.class)
    public ProjectsPageResponse getProjects(@Parameter(hidden = true) @RequestParam MultiValueMap<String, String> params,
                                                      @RequestParam(value = "status", required = false) List<ProjectStatus> statuses,
                                                      @RequestParam(value = "repository", required = false) List<String> repositories,
                                                      @RequestParam(value = "dependsOn", required = false) String dependsOn,
                                                      @RequestParam(value = "name", required = false) String name,
                                                      @RequestParam(value = "author", required = false) String author,
                                                      @RequestParam(value = "branch", required = false) String branch,
                                                      @RequestParam(value = "sort", required = false) @Nullable String sort,
                                                      @RequestParam(value = "include", required = false) List<ProjectInclude> includes,
                                                      @PaginationDefault Pageable page) {
        var queryBuilder = ProjectCriteriaQuery.builder()
                .repositoryIds(repositories)
                .statuses(statuses)
                .name(name)
                .author(author)
                .branch(branch)
                .sort(sort)
                .includes(ProjectInclude.normalize(includes));

        if (StringUtils.isNotEmpty(dependsOn)) {
            queryBuilder.dependsOn(ProjectIdModel.decode(dependsOn));
        }

        var tagValues = new LinkedHashMap<String, Set<String>>();
        params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(TAGS_PREFIX.length());
                    var values = new LinkedHashSet<String>();
                    entry.getValue().stream()
                            .filter(StringUtils::isNotBlank)
                            .forEach(values::add);
                    if (!values.isEmpty()) {
                        tagValues.put(tag, values);
                    }
                });
        queryBuilder.tagValues(tagValues);
        return projectService.getProjects(queryBuilder.build(), page);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "projects.get.summary")
    @JsonView(GenericView.Detailed.class)
    public ProjectViewModel getProject(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @Parameter(description = "projects.get.param.include.desc",
                                               style = ParameterStyle.FORM,
                                               explode = Explode.TRUE,
                                               array = @ArraySchema(schema = @Schema(implementation = ProjectInclude.class)))
                                       @RequestParam(value = "include", required = false) List<ProjectInclude> includes) {
        return projectService.getProject(project, includes);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "projects.delete.summary")
    public void deleteProject(@ProjectId @PathVariable("projectId") RulesProject project,
                              @Parameter(description = "projects.delete.param.comment.desc")
                              @RequestParam(value = "comment", required = false) @Nullable String comment) {
        projectService.delete(project, comment);
    }

    @PatchMapping("/{projectId}")
    @Operation(summary = "projects.status.update.summary")
    public void updateProjectStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @Valid @RequestBody ProjectStatusUpdateModel request) {
        var projectId = projectIdentifierMapper.map(project);
        if (conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
        var normalized = normalize(request);
        try {
            projectService.updateProjectStatus(project, normalized);
            if (normalized.status() != null
                    || normalized.branch() != null
                    || Boolean.TRUE.equals(normalized.save())
                    || normalized.comment() != null
                    || normalized.revision() != null) {
                getWebStudio().reset();
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    @FunctionalInterface
    private interface ProjectAction {
        void run() throws ProjectException;
    }

    /** Run a project mutation, reset the studio on success, and raise a {@link ConflictException} on failure. */
    private void withReset(String failureKey, ProjectAction action) {
        try {
            action.run();
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException(failureKey);
        }
    }

    @DeleteMapping("/{projectId}/lock")
    @Operation(summary = "projects.unlock.summary")
    public void unlockProject(@ProjectId @PathVariable("projectId") RulesProject project) {
        projectService.unlockProject(project);
        getWebStudio().reset();
    }

    @PutMapping("/{projectId}/tags")
    @Operation(summary = "projects.tags.update.summary")
    public void updateTags(@ProjectId @PathVariable("projectId") RulesProject project,
                           @RequestBody Map<String, String> tags) {
        withReset("project.tags.update.failed.message", () -> projectService.updateTags(project, tags));
    }

    @GetMapping("/{projectId}/status")
    @Operation(summary = "projects.status.get.summary", description = "projects.status.get.desc")
    @Deprecated(forRemoval = false)
    @JsonView(GenericView.Detailed.class)
    public ProjectStatusViewModel getStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @Parameter(description = "projects.status.param.branch.desc")
                                            @RequestParam(value = "branch", required = false) String branch) {
        // Read-only by design — the `branch` parameter is asserted against the project's
        // current branch; switching is exposed via PATCH /{projectId} with a
        // ProjectStatusUpdateModel that carries the target branch.
        if (StringUtils.isNotBlank(branch)) {
            if (!project.isSupportsBranches()) {
                throw new ConflictException("project.branch.unsupported.message");
            }
            if (!Objects.equals(branch, project.getBranch())) {
                throw new ConflictException("project.branch.mismatch.message");
            }
        }
        return projectStatusMapper.map(project);
    }

    @PostMapping("/{projectId}/branches")
    @Operation(summary = "projects.branch.create.summary")
    public void createBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @Valid @RequestBody CreateBranchModel request) {
        try {
            projectService.createBranch(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @GetMapping("/{projectId}/branches")
    @Operation(summary = "projects.branch.list.summary")
    public List<ProjectBranchInfo> getBranches(@ProjectId @PathVariable("projectId") RulesProject project) throws ProjectException {
        return projectService.getBranches(project);
    }

    @DeleteMapping("/{projectId}/branches/{*branch}")
    @Operation(summary = "projects.branch.delete.summary", description = "projects.branch.delete.desc")
    public void deleteBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @Parameter(description = "repo.param.branch-name.desc") @PathVariable("branch") String branch,
                             @Parameter(description = "projects.merge.param.force.desc")
                             @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        // Branch names may contain '/' (e.g. "project/user/date"), so the branch is captured as a trailing path
        // segment via {*branch}, which Spring exposes with a leading slash that must be removed.
        var branchName = branch.startsWith("/") ? branch.substring(1) : branch;
        if (StringUtils.isBlank(branchName)) {
            throw new BadRequestException("project.branch.name.empty.message");
        }
        projectService.deleteBranch(project, branchName, force);
        getWebStudio().reset();
    }

    @GetMapping("/{projectId}/tables")
    @Operation(summary = "projects.tables.list.summary")
    @Parameters({
            @Parameter(name = "kind", description = "projects.tables.list.param.kind.desc", in = ParameterIn.QUERY, schema = @Schema(implementation = String.class,
                    allowableValues = {
                            "Rules",
                            "Spreadsheet",
                            "Datatype",
                            "Data",
                            "Test",
                            "TBasic",
                            "Column Match",
                            "Method",
                            "Run",
                            "Constants",
                            "Conditions",
                            "Actions",
                            "Returns",
                            "Environment",
                            "Properties",
                            "Other"
                    })),
            @Parameter(name = "name", description = "projects.tables.list.param.name.desc", in = ParameterIn.QUERY),
            @Parameter(name = "properties", description = "projects.tables.list.param.properties.desc", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)
    })
    public PageResponse<SummaryTableView> getTables(@ProjectId @PathVariable("projectId") RulesProject project,
                                                    @Parameter(hidden = true) @RequestParam Map<String, String> params,
                                                    @RequestParam(value = "kind", required = false) Set<String> kinds,
                                                    @RequestParam(value = "name", required = false) String name,
                                                    @PaginationDefault Pageable page) {

        var queryBuilder = ProjectTableCriteriaQuery.builder().kinds(kinds).name(name);
        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(PROPERTIES_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(PROPERTIES_PREFIX.length());
                    queryBuilder.property(tag, entry.getValue());
                });

        return projectService.getTables(project, queryBuilder.build(), page);
    }

    @Operation(summary = "projects.tables.create.summary")
    @Parameter(name = "projectId", description = "projects.param.project-id.desc", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    @PostMapping("/{projectId}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public SummaryTableView createNewTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                           @Valid @RequestBody CreateNewTableRequest request) throws ProjectException {
        try {
            projectService.createNewTable(project, request);
        } finally {
            getWebStudio().reset();
        }
        var table = (TableView) request.table();
        var query = ProjectTableCriteriaQuery.builder().name(table.name).build();
        return projectService.getTables(project, query, Pageable.unpaged())
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/{projectId}/tables/{tableId}")
    @Operation(summary = "projects.table.get.summary")
    public EditableTableView getTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                      @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                      @RequestParam(value = "raw", defaultValue = "false") @Parameter(description = "projects.table.get.param.raw.desc") boolean raw,
                                      @RequestParam(value = "startRow", required = false) @Min(0) @Parameter(description = "projects.table.get.param.start-row.desc") Integer startRow,
                                      @RequestParam(value = "maxRows", required = false) @Min(1) @Parameter(description = "projects.table.get.param.max-rows.desc") Integer maxRows,
                                      @RequestParam(value = "styles", defaultValue = "false") @Parameter(description = "projects.table.get.param.styles.desc") boolean styles) {
        if (raw) {
            return projectService.getTableRaw(project, tableId, startRow, maxRows, styles);
        }
        return (EditableTableView) projectService.getTable(project, tableId);
    }

    @GetMapping("/{projectId}/tables/graph")
    @Operation(summary = "project.tables.graph.summary", description = "project.tables.graph.desc")
    public List<TableNodeView> getTablesGraph(@ProjectId @PathVariable("projectId") RulesProject project,
                                              @RequestParam(value = "module", required = false) @Parameter(description = "project.tables.graph.module.desc") String module) {
        // a blank `?module=` means the whole project, not a module named "" (which would fail to resolve)
        var moduleName = StringUtils.trimToNull(module);
        var model = projectService.openProject(project, moduleName).awaitCompiled();
        return graphService.buildProjectGraph(model, moduleName != null);
    }

    @GetMapping("/{projectId}/tables/{tableId}/graph")
    @Operation(summary = "project.table.graph.summary", description = "project.table.graph.desc")
    public List<TableNodeView> getTableGraph(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                             @RequestParam(value = "direction", defaultValue = "BOTH") @Parameter(description = "project.table.graph.direction.desc") GraphDirection direction,
                                             @RequestParam(value = "depth", required = false) @Min(1) @Parameter(description = "project.table.graph.depth.desc") Integer depth) {
        var model = projectService.openProject(project).awaitCompiled();
        if (model.getTableById(tableId) == null) {
            throw new NotFoundException("table.message");
        }
        return graphService.buildTableGraph(model, tableId, direction, depth);
    }

    @Operation(summary = "project.table.update.summary", description = "project.table.update.desc")
    @ApiResponse(responseCode = "200", description = "project.table.update.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.update.204.desc")
    @PutMapping("/{projectId}/tables/{tableId}")
    public ResponseEntity<TableIdView> updateTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                                   @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                   @Valid @RequestBody EditableTableView editTable) throws ProjectException {
        try {
            var newTableId = projectService.updateTable(project, tableId, editTable);
            return tableWriteResponse(tableId, newTableId);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "project.table.append.summary", description = "project.table.append.desc")
    @ApiResponse(responseCode = "200", description = "project.table.append.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.append.204.desc")
    @PostMapping("/{projectId}/tables/{tableId}/lines")
    public ResponseEntity<TableIdView> appendTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                                   @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                   @Valid @RequestBody AppendTableView editTable) throws ProjectException {
        try {
            var newTableId = projectService.appendTableLines(project, tableId, editTable);
            return tableWriteResponse(tableId, newTableId);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "project.table.actions.summary", description = "project.table.actions.desc")
    @ApiResponse(responseCode = "200", description = "project.table.actions.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.actions.204.desc")
    @PostMapping("/{projectId}/tables/{tableId}/actions")
    public ResponseEntity<TableIdView> editTableSource(@ProjectId @PathVariable("projectId") RulesProject project,
                                                       @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                       @Valid @RequestBody RawTableSourceAction action) throws ProjectException {
        try {
            var newTableId = projectService.editTableSource(project, tableId, action);
            return tableWriteResponse(tableId, newTableId);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "project.table.delete.summary", description = "project.table.delete.desc")
    @ApiResponse(responseCode = "204", description = "project.table.delete.204.desc")
    @DeleteMapping("/{projectId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId) throws ProjectException {
        try {
            projectService.deleteTable(project, tableId);
            return ResponseEntity.noContent().build();
        } finally {
            getWebStudio().reset();
        }
    }

    /**
     * Builds the response for a table write.
     * <p>
     * When the table ID is unchanged, returns 204 No Content with no headers. When the table was relocated and its ID
     * changed, returns 200 OK with the new ID in the body and a Location header pointing to the table resource under its
     * new ID.
     *
     * @param requestedTableId table ID from the request path
     * @param currentTableId   table ID after the write
     * @return 204 response when the ID is unchanged, otherwise a 200 response carrying the new ID
     */
    private static ResponseEntity<TableIdView> tableWriteResponse(String requestedTableId, String currentTableId) {
        if (currentTableId.equals(requestedTableId)) {
            return ResponseEntity.noContent().build();
        }
        var requestUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString();
        var tableUrl = requestUrl.substring(0, requestUrl.indexOf("/tables/")) + "/tables/" + currentTableId;
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, tableUrl)
                .body(new TableIdView(currentTableId));
    }

    @Operation(summary = "projects.tests.run.summary")
    @Parameter(name = "fromModule", description = "projects.tests.run.param.from-module.desc", in = ParameterIn.QUERY)
    @Parameter(name = "tableId", description = "projects.tests.run.param.table-id.desc", in = ParameterIn.QUERY)
    @Parameter(name = "testRanges", description = "projects.tests.run.param.test-ranges.desc", in = ParameterIn.QUERY)
    @PostMapping("/{projectId}/tests/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runAllTests(@ProjectId @PathVariable("projectId") RulesProject project,
                            @RequestParam(value = "fromModule", required = false) String fromModule,
                            @RequestParam(value = "tableId", required = false) String tableId,
                            @RequestParam(value = "testRanges", required = false) String testRanges) {
        executionTestsResultRegistry.cancelIfAny();
        var projectId = projectIdentifierMapper.map(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.openProject(project, fromModule).awaitCompiled();
        var currentOpenedModule = fromModule != null;
        CompletableFuture<List<TestUnitsResults>> testTask;
        var objectMapper = configureObjectMapper();
        var schemaGenerator = getSchemaGenerator(objectMapper);
        var mapper = new TestsExecutionSummaryResponseMapper(objectMapper, schemaGenerator);
        if (StringUtils.isBlank(tableId)) {
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user,
                    projectId,
                    testCase -> mapper.mapToTestCaseResult(testCase, TestExecutionSummaryQuery.noFilter()));
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            testTask = testsExecutorService.runAll(listener, projectModel, currentOpenedModule);
        } else {
            var table = projectModel.getTableById(tableId);
            if (table == null) {
                throw new NotFoundException("table.message");
            }
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user,
                    projectId,
                    tableId,
                    testCase -> mapper.mapToTestCaseResult(testCase, TestExecutionSummaryQuery.noFilter()));
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            if (StringUtils.isBlank(testRanges) && !OpenLTableUtils.isTestTable(table)) {
                testTask = testsExecutorService.runAllForTable(listener, projectModel, table, currentOpenedModule);
            } else {
                testTask = testsExecutorService.runSingle(listener, projectModel, table, testRanges, currentOpenedModule);
            }
        }
        executionTestsResultRegistry.setTask(projectId, testTask);
    }

    @Operation(summary = "projects.tests.summary.summary")
    @ApiResponse(responseCode = "404", description = "projects.tests.summary.404.desc")
    @ApiResponse(responseCode = "409", description = "projects.tests.summary.409.desc")
    @ApiResponse(responseCode = "406", description = "projects.tests.summary.406.desc")
    @ApiResponse(
            responseCode = "200",
            description = "projects.tests.summary.200.desc",
            headers = {
                    @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc")

            },
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestsExecutionSummary.class)),
                    @Content(mediaType = APPLICATION_XLSX_MEDIATYPE, schema = @Schema(type = "string", format = "binary"))
            }
    )
    @GetMapping(value = "/{projectId}/tests/summary", produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE})
    public ResponseEntity<?> getTestsSummary(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @RequestParam(value = "failuresOnly", defaultValue = "false")
                                             @Parameter(description = "projects.tests.summary.param.failures-only.desc")
                                             boolean failuresOnly,
                                             @RequestParam(value = "failures", defaultValue = "5")
                                             @Parameter(description = "projects.tests.summary.param.failures.desc")
                                             @Min(1)
                                             int failures,
                                             @PaginationDefault Pageable page,
                                             @Parameter(required = true, schema = @Schema(allowableValues = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE}))
                                             @RequestHeader(name = HttpHeaders.ACCEPT)
                                             String acceptMediaType) throws IOException {
        var projectId = projectIdentifierMapper.map(project);
        if (!executionTestsResultRegistry.hasTask(projectId)) {
            throw new NotFoundException("tests.execution.task.message");
        }
        if (!executionTestsResultRegistry.isDone(projectId)) {
            throw new ConflictException("tests.execution.not.completed.message");
        }
        var executionResults = executionTestsResultRegistry.getResultIfDone(projectId);
        if (executionResults == null) {
            throw new NotFoundException("tests.execution.task.message");
        }

        if (acceptMediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            var objectMapper = configureObjectMapper();
            var schemaGenerator = getSchemaGenerator(objectMapper);
            var mapper = new TestsExecutionSummaryResponseMapper(objectMapper, schemaGenerator);
            var query = new TestExecutionSummaryQuery(failuresOnly, failures);
            return ResponseEntity.ok(mapper.mapExecutionSummary(executionResults, query, page));
        } else if (acceptMediaType.equalsIgnoreCase(APPLICATION_XLSX_MEDIATYPE)) {
            var output = new ByteArrayOutputStream();
            new TestResultExport().export(output, page.getPageSize(), executionResults.toArray(new TestUnitsResults[0]));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue("test-results.xlsx"))
                    .header(HttpHeaders.CONTENT_TYPE, APPLICATION_XLSX_MEDIATYPE)
                    .body(output.toByteArray());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    private ObjectMapper configureObjectMapper() {
        try {
            var objectMapperFactory = projectService.getWebStudio().getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new ConflictException("object.mapper.configuration.failed.message");
        }
    }

    /**
     * Trim incoming string fields and convert whitespace-only values to {@code null} so
     * downstream service logic can rely on null-vs-non-null checks instead of repeatedly
     * calling {@code isNotBlank} / {@code trimToNull}.
     */
    private static ProjectStatusUpdateModel normalize(ProjectStatusUpdateModel raw) {
        return ProjectStatusUpdateModel.builder()
                .status(raw.status())
                .branch(StringUtils.trimToNull(raw.branch()))
                .revision(StringUtils.trimToNull(raw.revision()))
                .comment(StringUtils.trimToNull(raw.comment()))
                .save(raw.save())
                .discardChanges(raw.discardChanges())
                .selectedBranches(raw.selectedBranches())
                .openDependencies(raw.openDependencies())
                .build();
    }

}
