package org.openl.studio.projects.service;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.modules.CopyModuleRequest;
import org.openl.studio.projects.model.modules.CopyModuleResponse;
import org.openl.studio.projects.model.modules.EditModuleRequest;
import org.openl.studio.projects.model.modules.ModuleView;
import org.openl.studio.projects.model.modules.WildcardModuleView;

/**
 * Service for project module operations: listing, copying, adding, editing, and removing modules.
 *
 * <p>Provides a REST-friendly service layer for module management, replacing
 * the legacy module logic from {@code ProjectBean}.</p>
 *
 * <h3>Thread Safety</h3>
 * <p>Implementations should be thread-safe for concurrent operations on different projects.</p>
 *
 * @see ModuleView
 * @see WildcardModuleView
 * @see CopyModuleRequest
 * @see CopyModuleResponse
 * @see EditModuleRequest
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

    /**
     * Adds a new module to the project descriptor.
     *
     * <p>Replicates the legacy {@code ProjectBean.editModule()} behavior in add-new mode.</p>
     *
     * <p><b>Side effects:</b></p>
     * <ul>
     *   <li>Locks the project</li>
     *   <li>Updates the project descriptor (rules.xml)</li>
     * </ul>
     *
     * @param project the rules project to add the module to
     * @param request module definition (name, path, method filter, compilation settings)
     * @return the created module view
     * @throws org.openl.studio.common.exception.BadRequestException if validation fails (name, path)
     * @throws org.openl.studio.common.exception.ConflictException if module name or path conflicts, project is locked, or save fails
     * @throws org.openl.studio.common.exception.ForbiddenException if insufficient permissions
     */
    @NotNull
    ModuleView addModule(@NotNull RulesProject project, @Valid EditModuleRequest request);

    /**
     * Edits an existing module in the project descriptor.
     *
     * <p>Replicates the legacy {@code ProjectBean.editModule()} behavior in edit mode.
     * Supports renaming, path change, method filter update, and compilation settings.</p>
     *
     * <p><b>Side effects:</b></p>
     * <ul>
     *   <li>Locks the project</li>
     *   <li>Updates the project descriptor (rules.xml)</li>
     *   <li>If renamed: updates OpenAPI module name references</li>
     * </ul>
     *
     * @param project the rules project containing the module
     * @param moduleName current name of the module to edit
     * @param request new module definition (name, path, method filter, compilation settings)
     * @return the updated module view
     * @throws org.openl.studio.common.exception.NotFoundException if module not found
     * @throws org.openl.studio.common.exception.BadRequestException if validation fails (name, path)
     * @throws org.openl.studio.common.exception.ConflictException if name or path conflicts, project is locked, or save fails
     * @throws org.openl.studio.common.exception.ForbiddenException if insufficient permissions
     */
    @NotNull
    ModuleView editModule(@NotNull RulesProject project,
                          @NotBlank String moduleName,
                          @Valid EditModuleRequest request);

    /**
     * Removes a module from the project descriptor and optionally deletes associated files.
     *
     * <p>Replicates the legacy {@code ProjectBean.removeModule()} behavior.</p>
     *
     * <p><b>Side effects:</b></p>
     * <ul>
     *   <li>Locks the project</li>
     *   <li>If {@code !keepFile}: deletes module file(s) — for wildcards, all matched files</li>
     *   <li>Updates the project descriptor (rules.xml) or marks project as modified</li>
     *   <li>Clears OpenAPI module name references if applicable</li>
     * </ul>
     *
     * @param project the rules project containing the module
     * @param moduleName name of the module to remove
     * @param keepFile if true, only remove from descriptor without deleting the file(s)
     * @throws org.openl.studio.common.exception.NotFoundException if module not found
     * @throws org.openl.studio.common.exception.ConflictException if project is locked, delete or save fails
     * @throws org.openl.studio.common.exception.ForbiddenException if insufficient permissions for descriptor or file deletion
     */
    void removeModule(@NotNull RulesProject project,
                      @NotBlank String moduleName,
                      boolean keepFile);
}
