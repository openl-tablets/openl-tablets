package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.projects.messaging.SocketRunExecutionProgressListenerFactory;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.run.ExecutionRunResultRegistry;
import org.openl.studio.projects.service.run.RunExecutorService;
import org.openl.studio.projects.service.trace.TableInputParserService;

/**
 * Reading the result of a run: an accepted request while the run goes on, whatever the client asked for.
 */
class ProjectsRunControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TABLE_ID = "abc123";

    private final RulesProject project = mock(RulesProject.class);
    private final ProjectIdModel projectId = ProjectIdModel.builder().repository("design").projectName("Test").build();
    private final ExecutionRunResultRegistry registry = new ExecutionRunResultRegistry();
    private ProjectsRunController controller;

    @BeforeEach
    void init() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        when(projectIdentifierMapper.map(project)).thenReturn(projectId);
        controller = new ProjectsRunController(mock(WorkspaceProjectService.class),
                mock(RunExecutorService.class),
                registry,
                mock(SocketRunExecutionProgressListenerFactory.class),
                mock(TableInputParserService.class),
                mock(ProjectObjectMapperService.class),
                projectIdentifierMapper);
    }

    /**
     * A run still going on has nothing to report: the request is accepted rather than refused, so the screen
     * asking after the result raises no error while it waits.
     */
    @Test
    void getResult_whileTheRunIsStillGoingOn() throws Exception {
        registry.setTask(projectId, TABLE_ID, new CompletableFuture<>());

        var answer = controller.getResult(project, true, false, false, JSON);

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(ResultNotReadyView.ResultState.NOT_READY, ((ResultNotReadyView) answer.getBody()).status());
    }

    @Test
    void getResult_workbookWhileTheRunIsStillGoingOn() throws Exception {
        registry.setTask(projectId, TABLE_ID, new CompletableFuture<>());

        var answer = controller.getResult(project, true, false, false, XLSX);

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertNull(answer.getBody(), "a client that asked for a workbook is told by the status alone");
    }

    @Test
    void getResult_withoutARun() {
        assertThrows(NotFoundException.class, () -> controller.getResult(project, true, false, false, JSON));
    }

    @Test
    void getResult_inAMediaTypeItDoesNotProduce() throws Exception {
        registry.setTask(projectId, TABLE_ID, CompletableFuture.completedFuture(mock(TestUnitsResults.class)));

        var answer = controller.getResult(project, true, false, false, MediaType.TEXT_PLAIN_VALUE);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, answer.getStatusCode());
    }
}
