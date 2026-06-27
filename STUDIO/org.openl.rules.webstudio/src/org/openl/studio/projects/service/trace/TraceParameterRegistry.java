package org.openl.studio.projects.service.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Session-scoped registry for storing trace parameters for lazy loading.
 * <p>
 * This registry stores large parameter values that are not included in the initial
 * trace response to reduce payload size. Parameters are registered during trace
 * node mapping and can be retrieved later via their unique ID when the client
 * requests the full value.
 * </p>
 * <p>
 * The registry is session-scoped but keeps parameters per project, so a trace started in one project does not
 * drop the lazy-loadable parameters of another project traced in parallel (multiple browser tabs). IDs are
 * unique within the session, so retrieval is by ID; cleanup is per project.
 * </p>
 */
@Slf4j
@Component
@SessionScope
public class TraceParameterRegistry {

    private record Entry(ProjectIdModel projectId, ParameterWithValueDeclaration param) {
    }

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, Entry> parameters = new ConcurrentHashMap<>();

    /**
     * Registers a parameter for a project and returns its session-unique ID.
     *
     * @param projectId the owning project (for per-project cleanup)
     * @param param     the parameter to register
     * @return unique ID for later retrieval
     */
    public int register(ProjectIdModel projectId, ParameterWithValueDeclaration param) {
        int id = counter.incrementAndGet();
        parameters.put(id, new Entry(projectId, param));
        return id;
    }

    /**
     * Gets a parameter by its ID.
     *
     * @param id the parameter ID
     * @return the parameter, or null if not found
     */
    public ParameterWithValueDeclaration get(int id) {
        Entry entry = parameters.get(id);
        return entry != null ? entry.param() : null;
    }

    /**
     * Clears a single project's registered parameters.
     *
     * @param projectId the project whose parameters to drop
     */
    public void clear(ProjectIdModel projectId) {
        parameters.values().removeIf(entry -> entry.projectId().equals(projectId));
    }

    /**
     * Clears all registered parameters.
     */
    public void clear() {
        parameters.clear();
        counter.set(0);
    }

    /**
     * Drop cached trace parameters when the session workspace is reset: they reference
     * values from a trace computed against the previous compiled state.
     */
    @EventListener
    public void onWorkspaceReset(@NonNull WorkspaceResetEvent event) {
        try {
            clear();
        } catch (Exception e) {
            log.warn("onWorkspaceReset failed", e);
        }
    }
}
