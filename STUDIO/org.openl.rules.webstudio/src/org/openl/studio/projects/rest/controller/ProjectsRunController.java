package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.export.RulesResultExport;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.messaging.SocketRunExecutionProgressListenerFactory;
import org.openl.studio.projects.model.run.RunExecutionResult;
import org.openl.studio.projects.model.run.RunExecutionResultMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.run.ExecutionRunResultRegistry;
import org.openl.studio.projects.service.run.RunExecutorService;
import org.openl.studio.projects.service.trace.TableInputParserService;

/**
 * REST controller for executing regular (non-test) methods.
 * <p>
 * For test table execution, use the Tests API ({@code ProjectsController}) instead.
 * </p>
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/run", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Run (BETA)", description = "Experimental run execution API")
@Validated
public class ProjectsRunController {

    private static final String APPLICATION_XLSX_MEDIATYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final WorkspaceProjectService projectService;
    private final RunExecutorService runExecutorService;
    private final ExecutionRunResultRegistry runResultRegistry;
    private final SocketRunExecutionProgressListenerFactory listenerFactory;
    private final TableInputParserService inputParserService;
    private final Environment environment;

    public ProjectsRunController(WorkspaceProjectService projectService,
                                 RunExecutorService runExecutorService,
                                 ExecutionRunResultRegistry runResultRegistry,
                                 SocketRunExecutionProgressListenerFactory listenerFactory,
                                 TableInputParserService inputParserService,
                                 Environment environment) {
        this.projectService = projectService;
        this.runExecutorService = runExecutorService;
        this.runResultRegistry = runResultRegistry;
        this.listenerFactory = listenerFactory;
        this.inputParserService = inputParserService;
        this.environment = environment;
    }

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        return null;
    }

    @Operation(summary = "run.start.summary", description = "run.start.desc",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Object.class))))
    @ApiResponse(responseCode = "202", description = "run.start.202.desc")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startRun(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "run.param.table-id.desc") String tableId,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "run.param.from-module.desc") String fromModule,
            @Parameter(description = "run.param.input-json.desc") @RequestBody(required = false) String inputJson) {

        runResultRegistry.cancelIfAny();

        var projectId = projectService.resolveProjectId(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.getProjectModel(project, fromModule);
        var currentOpenedModule = fromModule != null;

        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }

        String uri = table.getUri();
        var method = currentOpenedModule
                ? projectModel.getOpenedModuleMethod(uri)
                : projectModel.getMethod(uri);

        if (method instanceof TestSuiteMethod) {
            throw new BadRequestException("run.test-table.not.supported.message");
        }

        var listener = listenerFactory.create(user, projectId, tableId);
        listener.onStatusChanged(ExecutionStatus.PENDING);

        var parseResult = inputParserService.parseInput(inputJson, method, configureObjectMapper());

        var runTask = runExecutorService.runMethod(
                listener, projectModel, table, parseResult.params(), parseResult.runtimeContext(),
                currentOpenedModule);

        runResultRegistry.setTask(projectId, tableId, runTask);
    }

    @Operation(summary = "run.get-result.summary", description = "run.get-result.desc")
    @ApiResponse(responseCode = "404", description = "run.execution.task.message")
    @ApiResponse(responseCode = "409", description = "run.execution.not.completed.message")
    @ApiResponse(responseCode = "406", description = "Requested media type is not acceptable")
    @ApiResponse(
            responseCode = "200",
            description = "run.get-result.200.desc",
            headers = {
                    @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc")
            },
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RunExecutionResult.class)),
                    @Content(mediaType = APPLICATION_XLSX_MEDIATYPE, schema = @Schema(type = "string", format = "binary"))
            }
    )
    @GetMapping(value = "/result", produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE})
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getResult(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(required = true, schema = @Schema(allowableValues = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE}))
            @RequestHeader(name = HttpHeaders.ACCEPT)
            String acceptMediaType) throws IOException {

        var projectId = projectService.resolveProjectId(project);
        if (!runResultRegistry.hasTask(projectId)) {
            throw new NotFoundException("run.execution.task.message");
        }
        if (!runResultRegistry.isDone(projectId)) {
            throw new ConflictException("run.execution.not.completed.message");
        }
        var results = runResultRegistry.getResultIfDone(projectId);
        if (results == null) {
            throw new NotFoundException("run.execution.task.message");
        }

        if (acceptMediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            var objectMapper = configureObjectMapper();
            var schemaGenerator = getSchemaGenerator(objectMapper);
            var sprNamingStrategy = extractSpreadsheetNamingStrategy();
            var mapper = new RunExecutionResultMapper(objectMapper, schemaGenerator, sprNamingStrategy);
            return ResponseEntity.ok(mapper.mapResult(results));
        } else if (acceptMediaType.equalsIgnoreCase(APPLICATION_XLSX_MEDIATYPE)) {
            var output = new ByteArrayOutputStream();
            new RulesResultExport().export(output, Integer.MAX_VALUE, results);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue("run-result.xlsx"))
                    .header(HttpHeaders.CONTENT_TYPE, APPLICATION_XLSX_MEDIATYPE)
                    .body(output.toByteArray());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    @Operation(summary = "run.cancel.summary", description = "run.cancel.desc")
    @ApiResponse(responseCode = "204", description = "run.cancel.204.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRun(@ProjectId @PathVariable("projectId") RulesProject project) {
        runResultRegistry.clear();
    }

    private SpreadsheetResultBeanPropertyNamingStrategy extractSpreadsheetNamingStrategy() {
        var studio = projectService.getWebStudio();
        var model = studio.getModel();
        PropertyNamingStrategy propertyNamingStrategy = ProjectJacksonObjectMapperFactoryBean
                .extractPropertyNamingStrategy(studio.getCurrentProjectRulesDeploy(),
                        model.getCompiledOpenClass().getClassLoader());
        return propertyNamingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy spr ? spr : null;
    }

    private ObjectMapper configureObjectMapper() {
        try {
            var objectMapperFactory = projectService.getWebStudio()
                    .getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new ConflictException("object.mapper.configuration.failed.message");
        }
    }
}
