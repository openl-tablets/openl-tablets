package org.openl.rules.webstudio.projects.mcp.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;

import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.resolver.Base64ProjectConverter;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.webstudio.projects.mcp.model.RunProjectTestsRequest;
import org.openl.rules.webstudio.projects.tests.executor.ProjectTestsExecutionProgressListener;
import org.openl.rules.webstudio.projects.tests.executor.TestsExecutorService;
import org.openl.rules.webstudio.projects.tests.model.TestsExecutionSummary;
import org.openl.rules.webstudio.projects.tests.model.TestsExecutionSummaryResponseMapper;
import org.openl.studio.mcp.McpController;
import org.openl.util.StringUtils;

/**
 * MCP Tool Controller for project tests execution and summary retrieval.
 * Provides tools to schedule test runs and get test execution summaries in JSON format.
 */
@McpController
public class ProjectsTestsMcpToolController {

    private final WorkspaceProjectService projectService;
    private final Base64ProjectConverter base64ProjectConverter;
    private final TestsExecutorService testsExecutorService;

    public ProjectsTestsMcpToolController(WorkspaceProjectService projectService,
                                          Base64ProjectConverter base64ProjectConverter,
                                          TestsExecutorService testsExecutorService) {
        this.projectService = projectService;
        this.base64ProjectConverter = base64ProjectConverter;
        this.testsExecutorService = testsExecutorService;
    }

    @Tool(
            name = "projects-tests-run",
            description = "Run project tests and get execution summary"
    )
    public TestsExecutionSummary runProjectTests(RunProjectTestsRequest request) throws ExecutionException, InterruptedException {
        var project = base64ProjectConverter.convert(request.projectId());

        var projectModel = projectService.getProjectModel(project, request.fromModule());
        var currentOpenedModule = request.fromModule() != null;
        CompletableFuture<List<TestUnitsResults>> testTask;
        if (StringUtils.isBlank(request.tableId())) {
            testTask = testsExecutorService.runAll(ProjectTestsExecutionProgressListener.NOP, projectModel, currentOpenedModule);
        } else {
            var table = projectModel.getTableById(request.tableId());
            if (table == null) {
                throw new NotFoundException("table.message");
            }
            if (StringUtils.isBlank(request.testRanges()) && !OpenLTableUtils.isTestTable(table)) {
                testTask = testsExecutorService.runAllForTable(ProjectTestsExecutionProgressListener.NOP,
                        projectModel,
                        table,
                        currentOpenedModule);
            } else {
                testTask = testsExecutorService.runSingle(ProjectTestsExecutionProgressListener.NOP,
                        projectModel,
                        table,
                        request.testRanges(),
                        currentOpenedModule);
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
