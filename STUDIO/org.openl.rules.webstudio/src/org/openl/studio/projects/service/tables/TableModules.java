package org.openl.studio.projects.service.tables;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;

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

    /**
     * A module and the location its workbook sits at, which every table of that module is addressed under.
     *
     * <p>Working the location out reads the file system, and a table read asks about every cell of a table, so
     * it is worked out once here rather than on each question.
     */
    private record ModuleLocation(String name, String uri) {
    }

    private final List<ModuleLocation> modules;

    private TableModules(List<ModuleLocation> modules) {
        this.modules = modules;
    }

    /** Locates the modules that have a workbook to hold tables and a name to be opened by. */
    private static TableModules locate(Stream<Module> modules) {
        return new TableModules(modules
                .filter(module -> module.getName() != null && module.getRulesRootPath() != null)
                .map(module -> new ModuleLocation(module.getName(), module.getRelativeUri()))
                .toList());
    }

    /**
     * The modules of a project, ready to be asked about a table.
     *
     * @param descriptor project descriptor, or {@code null} when the project has none resolved
     * @return the modules to ask, answering nothing for a project without a descriptor
     */
    public static TableModules of(@Nullable ProjectDescriptor descriptor) {
        return descriptor == null ? none() : locate(descriptor.getModules().stream());
    }

    /** No modules to ask: a table read outside a project, which has no module to be opened through. */
    public static TableModules none() {
        return new TableModules(List.of());
    }

    /**
     * The given modules, ready to be asked about a table.
     *
     * @param modules modules of a project, or of everything the workspace has compiled
     * @return the modules to ask
     */
    public static TableModules of(Collection<Module> modules) {
        return locate(modules.stream());
    }

    /**
     * The modules of everything the workspace has compiled: the project being read and the projects it depends
     * on.
     *
     * <p>A table of a dependency belongs to that project's module, and a screen sending a reader to it has to
     * name that module rather than one of this project's.
     *
     * @param model the compiled project
     * @return the modules to ask, answering nothing for a project that has not been compiled
     */
    public static TableModules ofWorkspace(ProjectModel model) {
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager == null) {
            return none();
        }
        return locate(dependencyManager.getDependencyLoaders()
                .stream()
                .filter(loader -> !loader.isProjectLoader())
                .map(IDependencyLoader::getModule)
                .filter(Objects::nonNull));
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
                .filter(module -> tableUri.startsWith(module.uri()))
                .findFirst()
                .map(ModuleLocation::name)
                .orElse(null);
    }
}
