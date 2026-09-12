package org.openl.studio.projects.service.tables;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Says which module of a project a table belongs to.
 *
 * <p>A table is addressed by the location the engine knows it at, and every module covers the workbook it is
 * written in. A screen that sends a reader to a table needs the module as much as the table itself: the editor
 * opens a module, and a table is read through the one that holds it.
 *
 * @author Vladyslav Pikus
 */
public final class TableModules {

    private final List<Module> modules;

    private TableModules(List<Module> modules) {
        this.modules = modules;
    }

    /**
     * The modules of a project, ready to be asked about a table.
     *
     * @param descriptor project descriptor, or {@code null} when the project has none resolved
     * @return the modules to ask, answering nothing for a project without a descriptor
     */
    public static TableModules of(@Nullable ProjectDescriptor descriptor) {
        return new TableModules(descriptor == null ? List.of() : List.copyOf(descriptor.getModules()));
    }

    /**
     * The module holding the table at the given location.
     *
     * @param tableUri location the engine knows the table at
     * @return name of the module, or {@code null} for a table none of these modules holds
     */
    public @Nullable String moduleOf(@Nullable String tableUri) {
        if (tableUri == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> module.getName() != null && module.containsTable(tableUri))
                .findFirst()
                .map(Module::getName)
                .orElse(null);
    }
}
