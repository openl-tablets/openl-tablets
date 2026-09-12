package org.openl.studio.projects.service.tables;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

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
     * Where a table lives: the module holding it, and the project that module belongs to.
     *
     * <p>The project is answered only where the modules were addressed — a screen that has to send a reader to
     * a table needs it, because a table exercised by this project may be written in one it depends on.
     *
     * @param module      name of the module holding the table
     * @param projectId   identifier the Projects API addresses that project by, absent when it has none
     * @param projectName name of that project, absent for the same reason
     */
    public record TableLocation(String module, @Nullable String projectId, @Nullable String projectName) {
    }

    /**
     * A module, the location its workbook sits at, and the project it belongs to.
     *
     * <p>Working the location out reads the file system, and a table read asks about every cell of a table, so
     * it is worked out once here rather than on each question.
     */
    private record ModuleLocation(String name, String uri, @Nullable ProjectAddress project) {

        TableLocation asTableLocation() {
            return new TableLocation(name, project == null ? null : project.id(), project == null ? null
                    : project.name());
        }
    }

    /** How a project is addressed by the Projects API, and what it is called. */
    private record ProjectAddress(String id, String name) {
    }

    private final List<ModuleLocation> modules;

    private TableModules(List<ModuleLocation> modules) {
        this.modules = modules;
    }

    /** Locates the modules that have a workbook to hold tables and a name to be opened by. */
    private static TableModules locate(Stream<Module> modules) {
        return locate(modules, module -> null);
    }

    private static TableModules locate(Stream<Module> modules, Function<Module, ProjectAddress> addressing) {
        return new TableModules(modules
                .filter(module -> module.getName() != null && module.getRulesRootPath() != null)
                .map(module -> new ModuleLocation(module.getName(), module.getRelativeUri(),
                        addressing.apply(module)))
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
        return located(model, module -> null);
    }

    /**
     * The modules of everything the workspace has compiled, each addressed by the project it belongs to.
     *
     * <p>A screen that sends a reader to a table has to name both: the module reads the table, and the project
     * is the one the module belongs to, which for a table of a dependency is not the project being read.
     *
     * @param model                  the compiled project
     * @param projectIdentifierMapper how a project is addressed by the Projects API
     * @return the modules to ask, answering nothing for a project that has not been compiled
     */
    public static TableModules ofWorkspace(ProjectModel model, ProjectIdentifierMapper projectIdentifierMapper) {
        var studio = model.getStudio();
        Function<Module, ProjectAddress> addressing = studio == null ? module -> null
                : addressing(studio, projectIdentifierMapper);
        return located(model, addressing);
    }

    /** The modules the workspace has compiled, addressed as the caller asked for. */
    private static TableModules located(ProjectModel model, Function<Module, ProjectAddress> addressing) {
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager == null) {
            return none();
        }
        return locate(dependencyManager.getDependencyLoaders()
                .stream()
                .filter(loader -> !loader.isProjectLoader())
                .map(IDependencyLoader::getModule)
                .filter(Objects::nonNull), addressing);
    }

    /**
     * How the project of a module is addressed, resolved once per project rather than once per module.
     *
     * <p>A project is looked up by the repository it sits in, which the studio knows by the project's name.
     */
    private static Function<Module, ProjectAddress> addressing(WebStudio studio,
                                                               ProjectIdentifierMapper projectIdentifierMapper) {
        var repositoryOfProject = new HashMap<String, String>();
        studio.getProjects()
                .forEach((repositoryId, descriptors) -> descriptors
                        .forEach(descriptor -> repositoryOfProject.putIfAbsent(descriptor.getName(), repositoryId)));
        Map<String, ProjectAddress> addressOfProject = new HashMap<>();
        return module -> {
            var descriptor = module.getProject();
            if (descriptor == null) {
                return null;
            }
            return addressOfProject.computeIfAbsent(descriptor.getName(),
                    name -> address(studio, projectIdentifierMapper, repositoryOfProject.get(name), descriptor));
        };
    }

    private static @Nullable ProjectAddress address(WebStudio studio,
                                                    ProjectIdentifierMapper projectIdentifierMapper,
                                                    @Nullable String repositoryId,
                                                    ProjectDescriptor descriptor) {
        if (repositoryId == null || descriptor.getProjectFolder() == null) {
            return null;
        }
        var project = studio.getProject(repositoryId, descriptor.getProjectFolder().getFileName().toString());
        return project == null ? null
                : new ProjectAddress(projectIdentifierMapper.map(project).encode(), descriptor.getName());
    }

    /**
     * The module holding the table at the given location.
     *
     * @param tableUri location the engine knows the table at
     * @return name of the module, or {@code null} for a table none of these modules holds
     */
    public @Nullable String moduleOf(@Nullable String tableUri) {
        var location = locationOf(tableUri);
        return location == null ? null : location.module();
    }

    /**
     * Where the table at the given location lives: the module holding it, and the project of that module.
     *
     * @param tableUri location the engine knows the table at
     * @return where it lives, or {@code null} for a table none of these modules holds
     */
    public @Nullable TableLocation locationOf(@Nullable String tableUri) {
        if (tableUri == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> tableUri.startsWith(module.uri()))
                .findFirst()
                .map(ModuleLocation::asTableLocation)
                .orElse(null);
    }
}
