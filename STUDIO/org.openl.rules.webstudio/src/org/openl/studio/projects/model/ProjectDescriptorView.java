package org.openl.studio.projects.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Editable content of a project's {@code rules.xml} (the project descriptor), returned by
 * {@code GET /projects/{projectId}/descriptor}.
 *
 * <p>This is the source of truth for the fields the Project page edits. The read-only summary
 * ({@code name}, {@code comment}, dependencies) is also available from {@code GET /projects/{projectId}},
 * but the descriptor endpoint owns the editable form.
 *
 * <p>{@code editable} tells the client whether the current user may change the descriptor.
 * {@code contentHash} is a hash of the current {@code rules.xml}; the client echoes it back on save
 * so a concurrent change can be detected.
 *
 * @author Yury Molchan
 */
public record ProjectDescriptorView(
        @Nullable String name,
        @Nullable String comment,
        List<ModuleView> modules,
        List<DependencyView> dependencies,
        List<String> classpath,
        @Nullable OpenApiView openapi,
        List<ExposedMethodView> exposedMethods,
        @Nullable String propertiesFileNameProcessor,
        List<String> propertiesFileNamePatterns,
        boolean editable,
        String contentHash) {

    /**
     * A single {@code <module>}. A module whose {@code rulesRootPath} contains a wildcard expands
     * into several real modules at runtime; such rows are marked {@code wildcard} and are read-only.
     */
    public record ModuleView(
            @Nullable String name,
            @Nullable String rulesRootPath,
            @Nullable MethodFilterView methodFilter,
            boolean compileThisModuleOnly,
            boolean wildcard) {
    }

    /** Module-level method filter: regex patterns matched against full method signatures. */
    public record MethodFilterView(List<String> includes, List<String> excludes) {
    }

    public record DependencyView(@Nullable String name, boolean autoIncluded, @Nullable String mavenArtifact) {
    }

    public record OpenApiView(
            @Nullable String path,
            @Nullable String mode,
            @Nullable String modelModuleName,
            @Nullable String algorithmModuleName) {
    }

    /**
     * One project-level exposed-method rule: a glob pattern matched against method names and whether
     * it includes or excludes. {@code type} is {@code "include"} or {@code "exclude"}.
     */
    public record ExposedMethodView(@Nullable String pattern, String type) {
    }
}
