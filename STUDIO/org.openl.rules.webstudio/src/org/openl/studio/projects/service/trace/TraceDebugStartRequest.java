package org.openl.studio.projects.service.trace;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.types.IOpenMethod;

/**
 * Everything needed to start a debug session, assembled by the controller from the request.
 *
 * @param projectModel        compiled project model to run against
 * @param table               the table to trace
 * @param method              the resolved method for the table
 * @param projectId           project identifier
 * @param tableId             table identifier
 * @param testRanges          test indices for a test suite, or {@code null} for all
 * @param currentOpenedModule run against the opened module instead of the whole project
 * @param inputJson           input parameters for a regular method, or {@code null}
 * @param objectMapper        project-configured mapper for parsing inputs
 * @param breakpoints         initial breakpoints by table URI
 * @param stopAtEntry         suspend at the first frame instead of running to the first breakpoint
 * @param listener            status sink for WebSocket notifications
 */
public record TraceDebugStartRequest(
        ProjectModel projectModel,
        IOpenLTable table,
        IOpenMethod method,
        ProjectIdModel projectId,
        String tableId,
        @Nullable String testRanges,
        boolean currentOpenedModule,
        @Nullable String inputJson,
        ObjectMapper objectMapper,
        Set<String> breakpoints,
        boolean stopAtEntry,
        DebugListener listener
) {
}
