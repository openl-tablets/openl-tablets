package org.openl.studio.projects.service;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.modules.CopyModuleRequest;
import org.openl.studio.projects.model.modules.CopyModuleResponse;
import org.openl.studio.projects.model.modules.ModuleView;
import org.openl.studio.projects.model.modules.WildcardModuleView;

/**
 * Service for project module operations: listing and copying modules.
 *
 * <p>Provides a REST-friendly service layer for module management, replacing
 * the legacy copy-module logic from {@code ProjectBean}.</p>
 *
 * <h3>Thread Safety</h3>
 * <p>Implementations should be thread-safe for concurrent operations on different projects.</p>
 *
 * @see ModuleView
 * @see WildcardModuleView
 * @see CopyModuleRequest
 * @see CopyModuleResponse
 */
public interface ProjectModulesService {

    /**
     * Returns all module definitions from the project descriptor.
     *
     * <p>The list reflects the original (unresolved) descriptor structure:
     * <ul>
     *   <li>Regular modules are returned as {@link ModuleView}</li>
     *   <li>Wildcard modules are returned as {@link WildcardModuleView} with their
     *       matched concrete modules in {@link WildcardModuleView#matchedModules}</li>
     * </ul>
     *
     * @param project the rules project to list modules for
     * @return list of module views, never null
     */
    @NotNull
    List<ModuleView> getModules(@NotNull RulesProject project);

    /**
     * Copies an existing module to a new file path within the same project.
     *
     * <p>This method replicates the legacy {@code ProjectBean.copyModule()} behavior with
     * the addition of properties file name pattern validation.</p>
     *
     * <p><b>Side effects:</b></p>
     * <ul>
     *   <li>Creates a new file in the project</li>
     *   <li>Grants CONTRIBUTOR ACL on the new file</li>
     *   <li>If not wildcard-covered: updates the project descriptor (rules.xml)</li>
     *   <li>If wildcard-covered: refreshes the project to discover the new module</li>
     * </ul>
     *
     * @param project the rules project containing the source module
     * @param moduleName name of the source module to copy
     * @param request copy parameters (new module name, new path)
     * @param force if true, skip properties file name pattern validation
     * @return copy result with module name, path, and wildcard status
     * @throws org.openl.studio.common.exception.NotFoundException if source module or source file is not found
     * @throws org.openl.studio.common.exception.BadRequestException if validation fails (name, path, pattern)
     * @throws org.openl.studio.common.exception.ConflictException if module name exists, file exists, project is locked, or copy fails
     * @throws org.openl.studio.common.exception.ForbiddenException if insufficient permissions
     */
    @NotNull
    CopyModuleResponse copyModule(@NotNull RulesProject project,
                                  @NotBlank String moduleName,
                                  @Valid CopyModuleRequest request,
                                  boolean force);
}
