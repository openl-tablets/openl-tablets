package org.openl.studio.projects.service.tables;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.util.StringTool;

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
     * A module, the location its workbook sits at, its path in the project, and the project it belongs to.
     *
     * <p>Working the location out reads the file system, and a table read asks about every cell of a table, so
     * it is worked out once here rather than on each question.
     */
    private record ModuleLocation(String name, String uri, String path, @Nullable ProjectAddress project) {

        /** Whether a file is the workbook of the module, or a file under the folder of a module written as one. */
        boolean holds(String file) {
            return file.equals(uri) || uri.endsWith("/") && file.startsWith(uri);
        }

        TableLocation asTableLocation() {
            return new TableLocation(name, project == null ? null : project.id(), project == null ? null
                    : project.name());
        }
    }

    /** How a project is addressed by the Projects API, and what it is called. */
    private record ProjectAddress(String id, String name) {
    }

    private final List<ModuleLocation> modules;

    /** Where a table the compiler built, and no workbook holds, is read: the module it is reached from. */
    private final Function<String, @Nullable TableLocation> generated;

    /** The id of the table at a location, as the compilation the modules were located through names it. */
    private final Function<String, @Nullable String> ids;

    private TableModules(List<ModuleLocation> modules,
                         Function<String, @Nullable TableLocation> generated,
                         Function<String, @Nullable String> ids) {
        this.modules = modules;
        this.generated = generated;
        this.ids = ids;
    }

    /** Locates the modules that have a workbook to hold tables and a name to be opened by. */
    private static TableModules locate(Stream<Module> modules) {
        return new TableModules(locations(modules, module -> null), tableUri -> null, tableUri -> null);
    }

    private static List<ModuleLocation> locations(Stream<Module> modules,
                                                  Function<Module, ProjectAddress> addressing) {
        return modules
                .filter(module -> module.getName() != null && module.getRulesRootPath() != null)
                .map(module -> new ModuleLocation(module.getName(), module.getRelativeUri(),
                        module.getRulesRootPath(), addressing.apply(module)))
                .toList();
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
        return new TableModules(List.of(), tableUri -> null, tableUri -> null);
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
     * @param model the compiled project
     * @return the modules to ask, answering nothing for a project that has not been compiled
     */
    public static TableModules ofWorkspaceAddressed(ProjectModel model) {
        var studio = model.getStudio();
        Function<Module, ProjectAddress> addressing = studio == null ? module -> null
                : addressing(studio);
        return located(model, addressing);
    }

    /** The modules the workspace has compiled, addressed as the caller asked for. */
    private static TableModules located(ProjectModel model, Function<Module, ProjectAddress> addressing) {
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager == null) {
            return none();
        }
        var opened = Optional.ofNullable(model.getModuleInfo())
                .flatMap(module -> locations(Stream.of(module), addressing).stream().findFirst())
                .map(ModuleLocation::asTableLocation);
        return new TableModules(locations(dependencyManager.getDependencyLoaders()
                .stream()
                .filter(loader -> !loader.isProjectLoader())
                .map(IDependencyLoader::getModule)
                .filter(Objects::nonNull), addressing),
                opened.map(location -> generatedIn(model, location)).orElse(tableUri -> null),
                idsIn(model));
    }

    /**
     * The ids of the tables the workspace has compiled, by the location the engine knows each table at.
     *
     * <p>The tables are indexed by their locations once, on the first question: a table read cell by cell, and the
     * data model of a graph, ask about many. A location no table has exactly is looked for as
     * {@link ProjectModel#getNode} looks for it, once.
     */
    private static Function<String, @Nullable String> idsIn(ProjectModel model) {
        var index = new AtomicReference<Map<String, TableSyntaxNode>>();
        var answered = new ConcurrentHashMap<String, Optional<String>>();
        return tableUri -> answered.computeIfAbsent(tableUri, uri -> {
            var located = index.updateAndGet(tables -> tables != null ? tables : byLocation(model)).get(uri);
            return Optional.ofNullable(located != null ? located : model.getNode(uri)).map(TableSyntaxNode::getId);
        }).orElse(null);
    }

    /** The tables the workspace has compiled by their locations, the tables of the module open first. */
    private static Map<String, TableSyntaxNode> byLocation(ProjectModel model) {
        var tables = new HashMap<String, TableSyntaxNode>();
        Stream.concat(Arrays.stream(model.getTableSyntaxNodes()), model.getAllTableSyntaxNodes().stream())
                .forEach(table -> tables.putIfAbsent(table.getUri(), table));
        return tables;
    }

    /**
     * The module open, for a table the compiler built for it.
     *
     * <p>Only a location in a workbook the engine built itself is looked for among those tables, each once: a table
     * read cell by cell asks again and again about the same ones. The compiler builds such a table when a word
     * leading to it is read, which is before its location is asked about.
     */
    private static Function<String, @Nullable TableLocation> generatedIn(ProjectModel model, TableLocation opened) {
        var answered = new ConcurrentHashMap<String, Boolean>();
        return tableUri -> ProjectModel.inCompilersWorkbook(tableUri)
                && answered.computeIfAbsent(tableUri, model::holdsTable) ? opened : null;
    }

    /**
     * How the project of a module is addressed, resolved once per project rather than once per module.
     *
     * <p>A project is looked up by the repository it sits in, which the studio knows by the project's name.
     */
    private static Function<Module, ProjectAddress> addressing(WebStudio studio) {
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
                    name -> address(studio, repositoryOfProject.get(name), descriptor));
        };
    }

    private static @Nullable ProjectAddress address(WebStudio studio,
                                                    @Nullable String repositoryId,
                                                    ProjectDescriptor descriptor) {
        if (repositoryId == null || descriptor.getProjectFolder() == null) {
            return null;
        }
        var project = studio.projectOf(repositoryId, descriptor);
        return project == null ? null
                : new ProjectAddress(project.getProjectId().value(), descriptor.getName());
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
     * The id of the compiled table at the given location.
     *
     * @param tableUri location the engine knows the table at
     * @return the id of the table, or {@code null} for a location no compiled table stands at
     */
    public @Nullable String tableIdOf(String tableUri) {
        return ids.apply(tableUri);
    }

    /**
     * The path in its project of the file at the given location: a workbook of one of these modules, or a file of
     * a module written as a folder.
     *
     * @param location location the engine knows a place of the file at, such as a line of it
     * @return path of the file, or {@code null} for a file none of these modules holds
     */
    public @Nullable String fileOf(String location) {
        var query = location.indexOf('?');
        var uri = query < 0 ? location : location.substring(0, query);
        return modules.stream()
                .filter(module -> module.holds(uri))
                .findFirst()
                .map(module -> inFolder(module.path(), StringTool.decodeURL(uri.substring(module.uri().length()))))
                .orElse(null);
    }

    private static String inFolder(String folder, String path) {
        return path.isEmpty() || folder.endsWith("/") ? folder + path : folder + "/" + path;
    }

    /**
     * Where the table at the given location lives: the module holding it, and the project of that module.
     *
     * <p>A table the compiler built to choose between the versions of an overloaded rule is written in no
     * workbook. It lives in the module open, which it is reached from, as long as the modules were located through
     * that module's compilation.
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
                .orElseGet(() -> generated.apply(tableUri));
    }
}
