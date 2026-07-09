package org.openl.studio.projects.model;

import java.util.List;

/**
 * The {@code rules.xml} exposed-methods filter. Glob patterns select which methods a deployed service
 * exposes; an empty filter exposes every method.
 *
 * @param includes patterns of exposed methods
 * @param excludes patterns of hidden methods
 */
public record ExposedMethodsViewModel(List<String> includes, List<String> excludes) {
}
