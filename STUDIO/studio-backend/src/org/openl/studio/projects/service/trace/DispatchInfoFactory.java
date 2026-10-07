package org.openl.studio.projects.service.trace;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.studio.projects.model.trace.DispatchInfo;
import org.openl.types.IOpenMethod;

/** Builds a {@link DispatchInfo} badge from a runtime dispatcher: its candidate versions and the chosen one. */
final class DispatchInfoFactory {

    private DispatchInfoFactory() {
    }

    /** Capture the dispatch: the dispatcher's candidate versions and which one it selected. */
    static DispatchInfo of(OpenMethodDispatcher dispatcher, @Nullable Object chosen) {
        List<DispatchInfo.Candidate> candidates = dispatcher.getCandidates().stream()
                .map(method -> new DispatchInfo.Candidate(label(method), method == chosen))
                .toList();
        return new DispatchInfo(candidates);
    }

    /** Label a version by its dimension properties (what makes it distinct), falling back to the rule name. */
    private static String label(IOpenMethod method) {
        if (method instanceof ITablePropertiesMethod propertiesMethod) {
            var properties = propertiesMethod.getMethodProperties();
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
