package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * A rules module as the project's {@code rules.xml} declares it.
 *
 * <p>One entry per declaration, so the list is as long as the file is. A declaration whose path is a
 * pattern stands for the files it matches and carries them as its own modules; every other declaration
 * is a single module and carries none.
 *
 * @param name    the module name, or the name the pattern gives the modules it matches
 * @param path    the rules root path, which may be a pattern
 * @param modules the modules the pattern matched, empty when it matched none, absent when the path is
 *                not a pattern
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleViewModel(
        @Parameter(description = "Module name") String name,
        @Parameter(description = "Rules root path. May be a pattern") String path,
        @Parameter(description = "Modules the pattern matched. Absent when the path is not a pattern") List<ModuleViewModel> modules) {

    /** A module the project declares by its own path. */
    public static ModuleViewModel module(String name, String path) {
        return new ModuleViewModel(name, path, null);
    }

    /** A declaration whose path is a pattern, standing for the modules it matched. */
    public static ModuleViewModel pattern(String name, String path, List<ModuleViewModel> matched) {
        return new ModuleViewModel(name, path, List.copyOf(matched));
    }
}
