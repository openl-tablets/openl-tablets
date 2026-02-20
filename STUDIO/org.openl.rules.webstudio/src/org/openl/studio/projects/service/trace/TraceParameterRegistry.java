package org.openl.studio.projects.service.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;

/**
 * Session-scoped registry for storing trace parameters for lazy loading.
 * <p>
 * This registry stores large parameter values that are not included in the initial
 * trace response to reduce payload size. Parameters are registered during trace
 * node mapping and can be retrieved later via their unique ID when the client
 * requests the full value.
 * </p>
 * <p>
 * The registry is session-scoped, meaning each user session has its own isolated
 * parameter storage. Parameters are cleared when a new trace execution starts or
 * when the trace is explicitly released.
 * </p>
 */
@Component
@SessionScope
public class TraceParameterRegistry {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, ParameterWithValueDeclaration> parameters = new ConcurrentHashMap<>();

    /**
     * Registers a parameter and returns its unique ID.
     *
     * @param param the parameter to register
     * @return unique ID for later retrieval
     */
    public int register(ParameterWithValueDeclaration param) {
        int id = counter.incrementAndGet();
        parameters.put(id, param);
        return id;
    }

    /**
     * Gets a parameter by its ID.
     *
     * @param id the parameter ID
     * @return the parameter, or null if not found
     */
    public ParameterWithValueDeclaration get(int id) {
        return parameters.get(id);
    }

    /**
     * Clears all registered parameters.
     */
    public void clear() {
        parameters.clear();
        counter.set(0);
    }
}
