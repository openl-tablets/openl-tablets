package org.openl.studio.projects.rest.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.trace.debug.DebugCommand;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.DefaultSourceClassifier;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.trace.BreakpointTableView;
import org.openl.studio.projects.model.trace.BreakpointsRequest;
import org.openl.studio.projects.model.trace.DebugFrameVariables;
import org.openl.studio.projects.model.trace.DebugStackView;
import org.openl.studio.projects.model.trace.DebugStatusView;
import org.openl.studio.projects.model.trace.TraceDebugMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.trace.DebugSession;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceDebugStartRequest;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.projects.service.trace.TraceTableHtmlService;
import org.openl.types.IOpenMethod;

/**
 * REST controller for the interactive trace debugger.
 *
 * <p>Drives a single debug session per user: start, step, resume, pause, breakpoints, and
 * stack/variable inspection. Execution runs on a dedicated worker thread and suspends at breakpoints
 * and step points; the controller reads the live stack while the worker is parked.
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Trace (BETA)", description = "Interactive trace debugger API")
@Validated
@RequiredArgsConstructor
public class ProjectsTraceDebugController {

    private static final long STEP_TIMEOUT_MILLIS = 30_000;

    private final WorkspaceProjectService projectService;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final TraceDebugService traceDebugService;
    private final DebugSessionRegistry sessionRegistry;
    private final SocketDebugListenerFactory listenerFactory;
    private final TraceParameterRegistry parameterRegistry;
    private final TraceTableHtmlService traceTableHtmlService;
    private final Environment environment;

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        return null;
    }

    @Operation(summary = "trace.start.summary", description = "trace.start.desc")
    @ApiResponse(responseCode = "200", description = "trace.start.200.desc")
    @PostMapping
    public DebugStackView startTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "trace.param.table-id.desc") String tableId,
            @RequestParam(value = "testRanges", required = false) @Parameter(description = "trace.param.test-ranges.desc") String testRanges,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "trace.param.from-module.desc") String fromModule,
            @RequestParam(value = "stopAtEntry", defaultValue = "true") @Parameter(description = "trace.param.stop-at-entry.desc") boolean stopAtEntry,
            @RequestBody(required = false) @Parameter(description = "trace.param.input-json.desc") String inputJson) {

        parameterRegistry.clear();
        sessionRegistry.clear();

        var projectId = projectIdentifierMapper.map(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.openProject(project, fromModule).awaitCompiled();
        var currentOpenedModule = fromModule != null;

        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        IOpenMethod method = currentOpenedModule
                ? projectModel.getOpenedModuleMethod(table.getUri())
                : projectModel.getMethod(table.getUri());
        if (method == null) {
            throw new NotFoundException("table.message");
        }

        DebugListener listener = listenerFactory.create(user, projectId, tableId);
        var request = new TraceDebugStartRequest(projectModel, table, method, projectId, tableId, testRanges,
                currentOpenedModule, inputJson, configureObjectMapper(), sessionRegistry.breakpoints(), stopAtEntry,
                listener);

        DebugSession session = sessionRegistry.start(traceDebugService.startSession(request));
        session.getDebugger().awaitInitialHalt(STEP_TIMEOUT_MILLIS);
        return stackView(session);
    }

    @Operation(summary = "trace.status.summary", description = "trace.status.desc")
    @ApiResponse(responseCode = "200", description = "trace.status.200.desc")
    @GetMapping("/status")
    public DebugStatusView status(@ProjectId @PathVariable("projectId") RulesProject project) {
        return new DebugStatusView(requireSession(project).getDebugger().status().name());
    }

    @Operation(summary = "trace.stack.summary", description = "trace.stack.desc")
    @ApiResponse(responseCode = "200", description = "trace.stack.200.desc")
    @GetMapping("/stack")
    public DebugStackView stack(@ProjectId @PathVariable("projectId") RulesProject project) {
        return stackView(requireSession(project));
    }

    @Operation(summary = "trace.step.summary", description = "trace.step.desc")
    @ApiResponse(responseCode = "200", description = "trace.step.200.desc")
    @PostMapping("/step")
    public DebugStackView step(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("type") @Parameter(description = "trace.param.step-type.desc") String type) {
        DebugSession session = requireSuspended(project);
        session.getDebugger().command(toCommand(type), STEP_TIMEOUT_MILLIS);
        return stackView(session);
    }

    @Operation(summary = "trace.resume.summary", description = "trace.resume.desc")
    @ApiResponse(responseCode = "202", description = "trace.resume.202.desc")
    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resume(@ProjectId @PathVariable("projectId") RulesProject project) {
        requireSuspended(project).getDebugger().resume();
    }

    @Operation(summary = "trace.pause.summary", description = "trace.pause.desc")
    @ApiResponse(responseCode = "202", description = "trace.pause.202.desc")
    @PostMapping("/pause")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void pause(@ProjectId @PathVariable("projectId") RulesProject project) {
        requireSession(project).getDebugger().pause();
    }

    @Operation(summary = "trace.get-variables.summary", description = "trace.get-variables.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-variables.200.desc")
    @GetMapping("/frames/{index}/variables")
    public DebugFrameVariables variables(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index) {
        DebugSession session = requireSuspended(project);
        DebugFrame frame = session.getDebugger().frameAt(index);
        if (frame == null) {
            throw new NotFoundException("trace.frame.not.found.message");
        }
        return createMapper().freezeVariables(frame, session.getClassLoader());
    }

    @Operation(summary = "trace.get-frame-table.summary", description = "trace.get-frame-table.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-frame-table.200.desc")
    @GetMapping(value = "/frames/{index}/table", produces = MediaType.TEXT_HTML_VALUE)
    public String frameTable(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index,
            @RequestParam(value = "showFormulas", defaultValue = "false") @Parameter(description = "trace.param.show-formulas.desc") boolean showFormulas) {
        DebugSession session = requireSuspended(project);
        DebugFrame frame = session.getDebugger().frameAt(index);
        if (frame == null) {
            throw new NotFoundException("trace.frame.not.found.message");
        }
        ProjectModel projectModel = projectService.openProject(project, null).awaitCompiled();
        return traceTableHtmlService.renderFrameTable(projectModel, frame, showFormulas);
    }

    @Operation(summary = "trace.get-parameter.summary", description = "trace.get-parameter.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-parameter.200.desc")
    @GetMapping("/parameters/{parameterId}")
    public ParameterValue parameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "trace.param.parameter-id.desc") int parameterId) {
        requireSession(project);
        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }
        return createMapper().buildParameterValue(param, false);
    }

    @Operation(summary = "trace.get-breakpoints.summary", description = "trace.get-breakpoints.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-breakpoints.200.desc")
    @GetMapping("/breakpoints")
    public List<String> getBreakpoints(@ProjectId @PathVariable("projectId") RulesProject project) {
        return List.copyOf(sessionRegistry.breakpoints());
    }

    @Operation(summary = "trace.set-breakpoints.summary", description = "trace.set-breakpoints.desc")
    @ApiResponse(responseCode = "204", description = "trace.set-breakpoints.204.desc")
    @PutMapping("/breakpoints")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBreakpoints(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestBody BreakpointsRequest request) {
        sessionRegistry.setBreakpoints(request.safeUris());
    }

    @Operation(summary = "trace.breakpoint-tables.summary", description = "trace.breakpoint-tables.desc")
    @ApiResponse(responseCode = "200", description = "trace.breakpoint-tables.200.desc")
    @GetMapping("/breakpoint-tables")
    public List<BreakpointTableView> breakpointTables(@ProjectId @PathVariable("projectId") RulesProject project) {
        ProjectModel projectModel = projectService.openProject(project, null).awaitCompiled();
        var classifier = new DefaultSourceClassifier();
        // List every rule table that becomes a frame, so a breakpoint can be set on it before it runs.
        return projectModel.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNode::getMember)
                .filter(ExecutableRulesMethod.class::isInstance)
                .map(classifier::describeFrame)
                .filter(Objects::nonNull)
                .map(d -> new BreakpointTableView(d.uri(), d.name(), d.kind().getCode()))
                .sorted(Comparator.comparing(BreakpointTableView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Operation(summary = "trace.cancel.summary", description = "trace.cancel.desc")
    @ApiResponse(responseCode = "204", description = "trace.cancel.204.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTrace(@ProjectId @PathVariable("projectId") RulesProject project) {
        sessionRegistry.clear();
        parameterRegistry.clear();
    }

    private DebugSession requireSession(RulesProject project) {
        DebugSession session = sessionRegistry.find(projectIdentifierMapper.map(project));
        if (session == null) {
            throw new NotFoundException("trace.execution.task.message");
        }
        return session;
    }

    private DebugSession requireSuspended(RulesProject project) {
        DebugSession session = requireSession(project);
        if (session.getDebugger().status() != DebugStatus.SUSPENDED) {
            throw new ConflictException("trace.execution.not.suspended.message");
        }
        return session;
    }

    private DebugStackView stackView(DebugSession session) {
        var debugger = session.getDebugger();
        DebugStatus status = debugger.status();
        Throwable error = debugger.error();
        String message = error == null ? null : error.getMessage();
        return createMapper().toStackView(status, debugger.stack(), message);
    }

    private static DebugCommand toCommand(String type) {
        return switch (type == null ? "" : type.toLowerCase()) {
            case "into" -> DebugCommand.STEP_INTO;
            case "over" -> DebugCommand.STEP_OVER;
            case "out" -> DebugCommand.STEP_OUT;
            case "caller" -> DebugCommand.STEP_TO_CALLER;
            default -> throw new BadRequestException("trace.debug.invalid-step.message");
        };
    }

    private TraceDebugMapper createMapper() {
        var objectMapper = configureObjectMapper();
        return new TraceDebugMapper(objectMapper, getSchemaGenerator(objectMapper), parameterRegistry);
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
