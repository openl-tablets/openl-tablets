package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.serialization.ObjectMapperConfigurationParsingException;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.ExceptionMappingService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.messaging.SocketRunExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.trace.StackViewMode;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.run.ExecutionRunResultRegistry;
import org.openl.studio.projects.service.run.RunExecutorService;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceExportService;
import org.openl.studio.projects.service.trace.TraceHighlightService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.types.IOpenMethod;

class ProjectObjectMapperConfigurationErrorTest {

    private static final String ERROR_CODE = "object.mapper.configuration.failed.message";
    private static final String MISSING_CLASS = "org.example.project.MissingType";

    private final Environment environment = mock(Environment.class);
    private final ProjectIdentifierMapper projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
    private final ProjectModel projectModel = mock(ProjectModel.class);
    private final RulesProject project = mock(RulesProject.class);
    private final WorkspaceProjectService projectService = mock(WorkspaceProjectService.class);
    private final ProjectObjectMapperService objectMapperService =
            new ProjectObjectMapperService(projectService, environment);
    private final ProjectJacksonObjectMapperFactoryBean objectMapperFactory =
            mock(ProjectJacksonObjectMapperFactoryBean.class);

    @BeforeEach
    void setUp() throws ClassNotFoundException {
        var projectHandle = mock(ProjectHandle.class);
        when(projectHandle.awaitCompiled()).thenReturn(projectModel);
        when(projectService.openProject(project, null)).thenReturn(projectHandle);
        var userWorkspace = mock(UserWorkspace.class);
        when(projectService.getUserWorkspace()).thenReturn(userWorkspace);

        var webStudio = mock(WebStudio.class);
        when(projectService.getWebStudio()).thenReturn(webStudio);
        when(webStudio.getCurrentProjectJacksonObjectMapperFactoryBean()).thenReturn(objectMapperFactory);
        failMapperCreationWith(new ClassNotFoundException(MISSING_CLASS));
    }

    @Test
    void testsReportTheMissingRootBindingClass() {
        assertTestsRejectMissingClass();
    }

    @Test
    void mapperConfigurationReturnsTheCreatedMapper() throws ClassNotFoundException {
        var objectMapper = mock(ObjectMapper.class);
        doReturn(objectMapper).when(objectMapperFactory).createJacksonObjectMapper();

        assertSame(objectMapper, objectMapperService.createObjectMapper());
        verify(objectMapperFactory).setEnvironment(environment);
    }

    @Test
    void testsReportTheMissingPropertyNamingStrategyClass() throws ClassNotFoundException {
        failMapperCreationWith(wrappedMissingClass());
        assertTestsRejectMissingClass();
    }

    private void assertTestsRejectMissingClass() {
        var testsExecutorService = mock(TestsExecutorService.class);
        var controller = createTestsController(testsExecutorService);

        assertMissingClass(() -> controller.runAllTests(project, null, null, null));
        verifyNoInteractions(testsExecutorService);
    }

    private ProjectsController createTestsController(TestsExecutorService testsExecutorService) {
        return new ProjectsController(
                projectService,
                testsExecutorService,
                mock(ExecutionTestsResultRegistry.class),
                mock(SocketProjectAllTestsExecutionProgressListenerFactory.class),
                objectMapperService,
                mock(ProjectsMergeConflictsSessionHolder.class),
                projectIdentifierMapper,
                mock(ProjectStatusMapper.class),
                mock(ProjectTablesGraphService.class),
                mock(RepositoryConfigService.class),
                mock(ProjectMetadataService.class),
                mock(ProjectMigrationService.class),
                mock(ProjectRevisionService.class));
    }

    @Test
    void runReportsTheMissingRootBindingClass() {
        assertRunRejectsMissingClass();
    }

    @Test
    void runReportsTheMissingPropertyNamingStrategyClass() throws ClassNotFoundException {
        failMapperCreationWith(wrappedMissingClass());
        assertRunRejectsMissingClass();
    }

    @Test
    void runPreservesMapperConfigurationErrorsWithoutMissingClasses() throws ClassNotFoundException {
        var configurationError = new ObjectMapperConfigurationParsingException("Invalid mapper configuration.");
        failMapperCreationWith(configurationError);
        var runExecutorService = mock(RunExecutorService.class);
        var controller = createRunController(runExecutorService);

        assertSame(configurationError,
                assertThrows(ObjectMapperConfigurationParsingException.class,
                        () -> controller.startRun(project, "table-id", null, null)));
        verifyNoInteractions(runExecutorService);
    }

    private void assertRunRejectsMissingClass() {
        var runExecutorService = mock(RunExecutorService.class);
        var controller = createRunController(runExecutorService);

        assertMissingClass(() -> controller.startRun(project, "table-id", null, null));
        verifyNoInteractions(runExecutorService);
    }

    private ProjectsRunController createRunController(RunExecutorService runExecutorService) {
        var projectId = ProjectIdModel.builder().repository("design").projectName("project").build();
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        var table = mock(IOpenLTable.class);
        when(table.getUri()).thenReturn("table-uri");
        when(projectModel.getTableById("table-id")).thenReturn(table);
        var method = mock(IOpenMethod.class);
        when(projectModel.getMethod("table-uri")).thenReturn(method);
        return new ProjectsRunController(
                projectService,
                runExecutorService,
                mock(ExecutionRunResultRegistry.class),
                mock(SocketRunExecutionProgressListenerFactory.class),
                mock(TableInputParserService.class),
                objectMapperService,
                projectIdentifierMapper);
    }

    @Test
    void traceReportsTheMissingRootBindingClass() {
        assertTraceRejectsMissingClass();
    }

    @Test
    void traceReportsTheMissingPropertyNamingStrategyClass() throws ClassNotFoundException {
        failMapperCreationWith(wrappedMissingClass());
        assertTraceRejectsMissingClass();
    }

    @Test
    void tracePreservesMapperConfigurationErrorsWithoutMissingClasses() throws ClassNotFoundException {
        var configurationError = new ObjectMapperConfigurationParsingException("Invalid mapper configuration.");
        failMapperCreationWith(configurationError);
        var traceDebugService = mock(TraceDebugService.class);
        var controller = createTraceController(traceDebugService);

        assertSame(configurationError,
                assertThrows(ObjectMapperConfigurationParsingException.class,
                        () -> startTrace(controller)));
        verifyNoInteractions(traceDebugService);
    }

    private void assertTraceRejectsMissingClass() {
        var traceDebugService = mock(TraceDebugService.class);
        var controller = createTraceController(traceDebugService);

        assertMissingClass(() -> startTrace(controller));
        verifyNoInteractions(traceDebugService);
    }

    private ProjectsTraceDebugController createTraceController(TraceDebugService traceDebugService) {
        var table = mock(IOpenLTable.class);
        when(table.getUri()).thenReturn("table-uri");
        when(projectModel.getTableById("table-id")).thenReturn(table);
        var method = mock(IOpenMethod.class);
        when(projectModel.getMethod("table-uri")).thenReturn(method);
        return new ProjectsTraceDebugController(
                projectService,
                projectIdentifierMapper,
                traceDebugService,
                mock(DebugSessionRegistry.class),
                mock(SocketDebugListenerFactory.class),
                mock(TraceParameterRegistry.class),
                mock(TraceHighlightService.class),
                mock(TraceExportService.class),
                mock(ProjectTablesGraphService.class),
                objectMapperService);
    }

    private void startTrace(ProjectsTraceDebugController controller) {
        controller.startTrace(project, "table-id", null, null, true, false, false, true,
                true, 20, StackViewMode.FULL, false, null);
    }

    @Test
    void testsPreserveMapperConfigurationErrorsWithoutMissingClasses() throws ClassNotFoundException {
        var configurationError = new ObjectMapperConfigurationParsingException("Invalid mapper configuration.");
        failMapperCreationWith(configurationError);
        var testsExecutorService = mock(TestsExecutorService.class);
        var controller = createTestsController(testsExecutorService);

        assertSame(configurationError,
                assertThrows(ObjectMapperConfigurationParsingException.class,
                        () -> controller.runAllTests(project, null, null, null)));
        verifyNoInteractions(testsExecutorService);
    }

    @Test
    void missingProjectClassMessageExplainsHowToFixTheProject() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        var error = new ExceptionMappingService(messageSource)
                .processException(new ConflictException(ERROR_CODE, MISSING_CLASS));

        assertEquals("openl.error.409." + ERROR_CODE, error.code);
        assertEquals("Failed to prepare the project data because class '" + MISSING_CLASS + "' is not available. " +
                "Correct rules-deploy.xml or add the class to the project classpath.", error.message);
    }

    private static void assertMissingClass(Executable operation) {
        var error = assertThrows(ConflictException.class, operation);

        assertEquals("openl.error.409." + ERROR_CODE, error.getErrorCode());
        assertArrayEquals(new Object[]{MISSING_CLASS}, error.getArgs());
        assertNull(error.getCause());
    }

    private void failMapperCreationWith(Exception failure) throws ClassNotFoundException {
        doThrow(failure).when(objectMapperFactory).createJacksonObjectMapper();
    }

    private static ObjectMapperConfigurationParsingException wrappedMissingClass() {
        return new ObjectMapperConfigurationParsingException(
                "Failed to load the configured class.",
                new ClassNotFoundException(MISSING_CLASS));
    }
}
