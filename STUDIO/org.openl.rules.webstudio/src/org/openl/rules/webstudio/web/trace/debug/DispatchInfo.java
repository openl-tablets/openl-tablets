package org.openl.rules.webstudio.web.trace.debug;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

/**
 * Records that a frame's table was selected at runtime from a group of versions overloaded by dimension
 * properties (a dispatcher). The chosen version is the frame itself; the candidates explain what it was
 * chosen from, so the dispatch is shown as a badge on the selected version rather than as an extra tree level.
 *
 * @param candidates the overloaded versions, each labelled by its dimension properties, with the chosen one flagged
 */
public record DispatchInfo(List<Candidate> candidates) {

    /**
     * One overloaded version of the dispatched rule.
     *
     * @param label  the version's dimension properties (for example {@code effectiveDate: 01/01/2020}), or its name
     * @param chosen whether the dispatcher selected this version for the current runtime context
     */
    public record Candidate(String label, boolean chosen) {
    }

    /** Capture the dispatch: the dispatcher's candidate versions and which one it selected. */
    static DispatchInfo of(OpenMethodDispatcher dispatcher, @Nullable Object chosen) {
        List<Candidate> candidates = dispatcher.getCandidates().stream()
                .map(method -> new Candidate(label(method), method == chosen))
                .toList();
        return new DispatchInfo(candidates);
    }

    /** Label a version by its dimension properties (what makes it distinct), falling back to the rule name. */
    private static String label(IOpenMethod method) {
        if (method instanceof ITablePropertiesMethod propertiesMethod) {
            ITableProperties properties = propertiesMethod.getMethodProperties();
            if (properties != null) {
                Map<String, Object> dimensions = properties.getAllDimensionalProperties();
                if (dimensions != null && !dimensions.isEmpty()) {
                    return dimensions.keySet().stream()
                            .map(name -> name + ": " + properties.getPropertyValueAsString(name))
                            .collect(Collectors.joining(", "));
                }
            }
        }
        return method.getName();
    }
}
