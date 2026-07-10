package org.openl.studio.projects.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The {@code rules.xml} exposed-methods filter. Glob patterns select which methods a deployed service
 * exposes; an empty filter exposes every method.
 *
 * @param includes patterns of exposed methods
 * @param excludes patterns of hidden methods
 */
public record ExposedMethodsViewModel(
        @Parameter(description = "Glob patterns of exposed methods") List<String> includes,
        @Parameter(description = "Glob patterns of hidden methods") List<String> excludes) {
}
