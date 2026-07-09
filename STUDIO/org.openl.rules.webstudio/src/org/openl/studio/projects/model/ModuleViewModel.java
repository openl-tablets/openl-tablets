package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A rules module declared in the project's {@code rules.xml}.
 *
 * @param name the module name
 * @param path the rules root path (may contain wildcards)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleViewModel(String name, String path) {
}
