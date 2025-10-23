package org.openl.rules.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.tables.AppendTableView;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.service.ProjectCriteriaQuery;
import org.openl.rules.rest.service.ProjectTableCriteriaQuery;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.NewBranchValidator;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.TestResultExport;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.projects.tests.executor.ExecutionTestsResultRegistry;
import org.openl.rules.webstudio.projects.tests.executor.TestExecutionStatus;
import org.openl.rules.webstudio.projects.tests.executor.TestsExecutorService;
import org.openl.rules.webstudio.projects.tests.messaging.service.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.rules.webstudio.projects.tests.model.TestsExecutionSummary;
import org.openl.rules.webstudio.projects.tests.model.TestsExecutionSummaryResponseMapper;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Projects REST controller
 *
 * @author Vladyslav Pikus
 */
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects (BETA)", description = "Experimental projects API")
public class ProjectsController {

    private static final String APPLICATION_XLSX_MEDIATYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String TAGS_PREFIX = "tags.";
    private static final String PROPERTIES_PREFIX = "properties.";

    private final WorkspaceProjectService projectService;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;
    private final BeanValidationProvider validationProvider;
    private final TestsExecutorService testsExecutorService;
    private final ExecutionTestsResultRegistry executionTestsResultRegistry;
    private final SocketProjectAllTestsExecutionProgressListenerFactory socketProjectAllTestsExecutionProgressListenerFactory;
    private final Environment environment;

    public ProjectsController(WorkspaceProjectService projectService,
                              Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory,
                              BeanValidationProvider validationProvider,
                              TestsExecutorService testsExecutorService,
                              ExecutionTestsResultRegistry executionTestsResultRegistry,
                              SocketProjectAllTestsExecutionProgressListenerFactory socketProjectAllTestsExecutionProgressListenerFactory,
                              Environment environment) {
        this.projectService = projectService;
        this.newBranchValidatorFactory = newBranchValidatorFactory;
        this.validationProvider = validationProvider;
        this.testsExecutorService = testsExecutorService;
        this.executionTestsResultRegistry = executionTestsResultRegistry;
        this.socketProjectAllTestsExecutionProgressListenerFactory = socketProjectAllTestsExecutionProgressListenerFactory;
        this.environment = environment;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping
    @Operation(summary = "Get projects (BETA)")
    @Parameters({
            @Parameter(name = "status", description = "Project status", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {
                    "LOCAL",
                    "ARCHIVED",
                    "OPENED",
                    "VIEWING_VERSION",
                    "EDITING",
                    "CLOSED"})),
            @Parameter(name = "repository", description = "Repository ID", in = ParameterIn.QUERY),
            @Parameter(name = "tags", description = "Project tags. Must start with `tags.` ", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)})
    public List<ProjectViewModel> getProjects(@Parameter(hidden = true) @RequestParam Map<String, String> params,
                                              @RequestParam(value = "status", required = false) ProjectStatus status,
                                              @RequestParam(value = "repository", required = false) String repository) {

        var queryBuilder = ProjectCriteriaQuery.builder().repositoryId(repository).status(status);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(TAGS_PREFIX.length());
                    queryBuilder.tag(tag, entry.getValue());
                });

        return projectService.getProjects(queryBuilder.build());
    }

    @Hidden
    @GetMapping("/{projectId}")
    public ProjectViewModel getProject(@ProjectId @PathVariable("projectId") RulesProject project) {
        return projectService.getProject(project);
    }

    @PatchMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update project status (BETA)")
    public void updateProjectStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @RequestBody ProjectStatusUpdateModel request) {
        try {
            projectService.updateProjectStatus(project, request);
            if (request.getStatus() != null
                    || request.getBranch().isPresent()
                    || request.getComment().isPresent()
                    || request.getRevision().isPresent()) {
                getWebStudio().reset();
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    @PostMapping("/{projectId}/branches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Create branch (BETA)")
    public void createBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @RequestBody CreateBranchModel request) {
        var repository = project.getDesignRepository();
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        var validator = newBranchValidatorFactory.apply((BranchRepository) repository);
        validationProvider.validate(request.getBranch(), validator);
        try {
            projectService.createBranch(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @GetMapping("/{projectId}/tables")
    @Operation(summary = "Get project tables (BETA)")
    @Parameters({@Parameter(name = "kind", description = "Table kinds", in = ParameterIn.QUERY),
            @Parameter(name = "name", description = "Table name fragment", in = ParameterIn.QUERY),
            @Parameter(name = "properties", description = "Project properties. Must start with `properties.` ", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)})
    public Collection<SummaryTableView> getTables(@ProjectId @PathVariable("projectId") RulesProject project,
                                                  @Parameter(hidden = true) @RequestParam Map<String, String> params,
                                                  @RequestParam(value = "kind", required = false) Set<String> kinds,
                                                  @RequestParam(value = "name", required = false) String name) {

        var queryBuilder = ProjectTableCriteriaQuery.builder().kinds(kinds).name(name);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(PROPERTIES_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(PROPERTIES_PREFIX.length());
                    queryBuilder.property(tag, entry.getValue());
                });

        return projectService.getTables(project, queryBuilder.build());
    }

    @GetMapping("/{projectId}/tables/{tableId}")
    @Operation(summary = "Get project table (BETA)")
    public EditableTableView getTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                      @PathVariable("tableId") String tableId) {
        return (EditableTableView) projectService.getTable(project, tableId);
    }

    @Operation(summary = "Update project table (BETA)")
    @PutMapping("/{projectId}/tables/{tableId}")
    public void updateTable(@ProjectId @PathVariable("projectId") RulesProject project,
                            @PathVariable("tableId") String tableId,
                            @RequestBody EditableTableView editTable) throws ProjectException {
        try {
            projectService.updateTable(project, tableId, editTable);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "Append project table (BETA)")
    @PostMapping("/{projectId}/tables/{tableId}/lines")
    public void appendTable(@ProjectId @PathVariable("projectId") RulesProject project,
                            @PathVariable("tableId") String tableId,
                            @RequestBody AppendTableView editTable) throws ProjectException {
        try {
            projectService.appendTableLines(project, tableId, editTable);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "Run all tests in the project or in a specific table (BETA)")
    @Parameter(name = "fromModule", description = "Module name to run tests from", in = ParameterIn.QUERY)
    @Parameter(name = "tableId", description = "Table ID to run tests for a specific table", in = ParameterIn.QUERY)
    @Parameter(name = "testRanges", description = "Test ranges to run specific tests in the table", in = ParameterIn.QUERY)
    @PostMapping("/{projectId}/tests/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runAllTests(@ProjectId @PathVariable("projectId") RulesProject project,
                            @RequestParam(value = "fromModule", required = false) String fromModule,
                            @RequestParam(value = "tableId", required = false) String tableId,
                            @RequestParam(value = "testRanges", required = false) String testRanges) {
        executionTestsResultRegistry.cancelIfAny();
        var projectId = projectService.resolveProjectId(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.getProjectModel(project, fromModule);
        var currentOpenedModule = fromModule != null;
        CompletableFuture<List<TestUnitsResults>> testTask;
        var mapper = new TestsExecutionSummaryResponseMapper(configureObjectMapper());
        if (StringUtils.isBlank(tableId)) {
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user, projectId, mapper::mapToTestCaseResult);
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            testTask = testsExecutorService.runAll(listener, projectModel, currentOpenedModule);
        } else {
            var table = projectModel.getTableById(tableId);
            if (table == null) {
                throw new NotFoundException("table.message");
            }
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user, projectId, tableId, mapper::mapToTestCaseResult);
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            if (StringUtils.isBlank(testRanges) && !OpenLTableUtils.isTestTable(table)) {
                testTask = testsExecutorService.runAllForTable(listener, projectModel, table, currentOpenedModule);
            } else {
                testTask = testsExecutorService.runSingle(listener, projectModel, table, testRanges, currentOpenedModule);
            }
        }
        executionTestsResultRegistry.setTask(projectId, testTask);
    }

    @Operation(summary = "Get latest tests execution summary (BETA)")
    @ApiResponse(responseCode = "404", description = "No tests execution task found or the task is not completed yet")
    @ApiResponse(responseCode = "409", description = "Tests execution is not completed yet")
    @ApiResponse(responseCode = "406", description = "Requested media type is not acceptable")
    @ApiResponse(
            responseCode = "200",
            description = "Tests execution summary retrieved successfully",
            headers = {
                    @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc")

            },
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestsExecutionSummary.class)),
                    @Content(mediaType = APPLICATION_XLSX_MEDIATYPE, schema = @Schema(type = "string", format = "binary"))
            }
    )
    @GetMapping(value = "/{projectId}/tests/summary", produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE})
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getTestsSummary(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @Parameter(required = true, schema = @Schema(allowableValues = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE})) @RequestHeader(name = HttpHeaders.ACCEPT) String acceptMediaType) throws IOException {
        var projectId = projectService.resolveProjectId(project);
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
            var mapper = new TestsExecutionSummaryResponseMapper(configureObjectMapper());
            return ResponseEntity.ok(mapper.mapExecutionSummary(executionResults));
        } else if (acceptMediaType.equalsIgnoreCase(APPLICATION_XLSX_MEDIATYPE)) {
            var output = new ByteArrayOutputStream();
            new TestResultExport().export(output, -1, executionResults.toArray(new TestUnitsResults[0]));
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
            var objectMapperFactory = WebStudioUtils.getWebStudio().getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new ConflictException("object.mapper.configuration.failed.message");
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Parameter(description = "Project ID", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    public @interface ProjectId {

    }

}
