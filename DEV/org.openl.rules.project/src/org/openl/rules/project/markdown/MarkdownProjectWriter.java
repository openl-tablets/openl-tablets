package org.openl.rules.project.markdown;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.excel.grid.MarkdownModuleWriter;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;

/**
 * Writes a copy of a project in which every module read from a workbook is a module folder of Markdown files, a table
 * to a file.
 *
 * <p>A module folder stands where its workbook stood, at the path of the workbook without its extension, and the
 * {@code rules.xml} of the copy names the folders, a module path matching several workbooks module by module. A file
 * name pattern of the project reads a folder as it read the workbook. Every other file of the project is copied as it
 * is.
 *
 * <p>A workbook holding a table no Markdown file can hold stays in the copy as it is, and the result tells which table
 * and why, so the copy compiles to the same rules. So do a workbook another module includes and a workbook including
 * the workbook of another module, whose tables the module and the include read once, and a workbook whose folder would
 * take the place of a folder of the project.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarkdownProjectWriter {

    /**
     * Writes the copy of a project.
     *
     * @param factory the engine factory of the project, set up as the project is compiled — its class loader, its
     *                workspace, its external parameters — and not in the execution mode, which drops the tables
     * @param target  the folder of the copy, replaced when it exists; it may lie inside the project
     * @return the modules written as folders and the modules kept as workbooks
     * @throws Exception when the project cannot be compiled or a file cannot be written
     */
    public static Written write(SimpleProjectEngineFactory<?> factory, Path target) throws Exception {
        var project = factory.getProjectDescriptor();
        if (Files.exists(target)) {
            FileUtils.delete(target);
        }
        FileUtils.copy(project.getProjectFolder().toFile(), target.toFile());
        factory.getCompiledOpenClass();
        var includes = includesOf(factory, project);
        var converted = new ArrayList<Converted>();
        var kept = new ArrayList<Kept>();
        for (var module : project.getModules()) {
            if (module.getRulesRootPath() != null && FileTypeHelper.isExcelFile(module.getRulesRootPath())) {
                switch (write(factory, module, target, includes)) {
                    case Converted folder -> converted.add(folder);
                    case Kept workbook -> kept.add(workbook);
                }
            }
        }
        if (!converted.isEmpty()) {
            writeDescriptor(project, converted, target);
        }
        return new Written(List.copyOf(converted), List.copyOf(kept));
    }

    /** The workbooks each module includes besides its own, by the file of the module. */
    private static Map<Path, Set<Path>> includesOf(SimpleProjectEngineFactory<?> factory, ProjectDescriptor project)
            throws Exception {
        var includes = new HashMap<Path, Set<Path>>();
        for (var module : project.getModules()) {
            var own = fileOf(module);
            var included = new HashSet<Path>();
            for (var workbook : workbooksOf(factory, module)) {
                var file = fileOf(workbook);
                if (file != null && !file.equals(own) && FileTypeHelper.isExcelFile(file.toString())) {
                    included.add(file);
                }
            }
            includes.put(own, included);
        }
        return includes;
    }

    /**
     * Why a module stays a workbook for its includes: a workbook another module includes, or a workbook including the
     * workbook of another module. A module and an include read the tables of one workbook once, which they do not when
     * one of them reads a folder.
     */
    private static Optional<String> includedOrIncluding(Module module, Map<Path, Set<Path>> includes) {
        var own = fileOf(module);
        if (includes.values().stream().anyMatch(included -> included.contains(own))) {
            return Optional.of("Another module includes the workbook");
        }
        return includes.getOrDefault(own, Set.of())
                .stream()
                .filter(includes::containsKey)
                .findFirst()
                .map(file -> "The workbook includes %s, the workbook of another module".formatted(file.getFileName()));
    }

    private static Path fileOf(Module module) {
        return module.getRulesPath().toAbsolutePath().normalize();
    }

    /** Writes a module into its folder, or keeps its workbook, telling why. */
    private static Outcome write(SimpleProjectEngineFactory<?> factory, Module module, Path target,
                                 Map<Path, Set<Path>> includes) throws Exception {
        var path = module.getRulesRootPath();
        var moduleFolder = target.resolve(FileUtils.removeExtension(path));
        var workbook = workbookOf(factory, module);
        if (workbook == null) {
            return new Kept(module.getName(), path, List.of("The module was compiled from no workbook"));
        }
        if (Files.exists(moduleFolder)) {
            return new Kept(module.getName(), path, List.of("The project holds the folder %s already"
                    .formatted(pathIn(target, moduleFolder))));
        }
        var byIncludes = includedOrIncluding(module, includes);
        if (byIncludes.isPresent()) {
            return new Kept(module.getName(), path, List.of(byIncludes.get()));
        }
        var written = MarkdownModuleWriter.write(workbook, moduleFolder);
        if (!written.leftOut().isEmpty()) {
            FileUtils.delete(moduleFolder);
            return new Kept(module.getName(), path, written.leftOut()
                    .stream()
                    .map(table -> "%s: %s".formatted(table.header(), table.reason()))
                    .toList());
        }
        var workbookFile = target.resolve(path);
        var workbookBytes = Files.size(workbookFile);
        Files.delete(workbookFile);
        var markdownBytes = 0L;
        for (var file : written.files()) {
            markdownBytes += Files.size(file);
        }
        return new Converted(module.getName(), path, pathIn(target, moduleFolder), written.files().size(),
                workbookBytes, markdownBytes);
    }

    private static String pathIn(Path folder, Path path) {
        return folder.relativize(path).toString().replace(File.separatorChar, '/');
    }

    /** The workbook a module was compiled from, as the compiler read it. */
    private static @Nullable WorkbookSyntaxNode workbookOf(SimpleProjectEngineFactory<?> factory, Module module)
            throws ProjectResolvingException, OpenLCompilationException {
        var file = fileOf(module);
        return workbooksOf(factory, module).stream()
                .filter(workbook -> file.equals(fileOf(workbook)))
                .findFirst()
                .orElse(null);
    }

    /** The workbooks of a compiled module: its own and the ones it includes. */
    private static List<WorkbookSyntaxNode> workbooksOf(SimpleProjectEngineFactory<?> factory, Module module)
            throws ProjectResolvingException, OpenLCompilationException {
        var compiled = factory.getDependencyManager()
                .loadDependency(AbstractDependencyManager.buildResolvedDependency(module))
                .getCompiledOpenClass()
                .getOpenClassWithErrors();
        return compiled.getMetaInfo() instanceof XlsMetaInfo metaInfo
                ? List.of(metaInfo.getXlsModuleNode().getWorkbookSyntaxNodes())
                : List.of();
    }

    private static @Nullable Path fileOf(WorkbookSyntaxNode workbook) {
        return Optional.ofNullable(workbook.getWorkbookSourceCodeModule())
                .map(XlsWorkbookSourceCodeModule::getSourceFile)
                .map(source -> source.toPath().toAbsolutePath().normalize())
                .orElse(null);
    }

    /** Writes the {@code rules.xml} of the copy: the modules of the project as it was resolved, by their new paths. */
    private static void writeDescriptor(ProjectDescriptor project, List<Converted> converted, Path target)
            throws IOException {
        var declared = Optional.ofNullable(ProjectDescriptor.read(project.getProjectFolder()))
                .orElseGet(ProjectDescriptor::new);
        if (declared.getName() == null) {
            declared.setName(project.getName());
        }
        var folders = new HashMap<String, String>();
        converted.forEach(module -> folders.put(module.workbook(), module.folder()));
        var modules = new ArrayList<Module>();
        for (var module : project.getModules()) {
            var copy = new Module();
            copy.setName(module.getName());
            copy.setRulesRootPath(folders.getOrDefault(module.getRulesRootPath(), module.getRulesRootPath()));
            copy.setMethodFilter(module.getMethodFilter());
            copy.setWebstudioConfiguration(module.getWebstudioConfiguration());
            modules.add(copy);
        }
        declared.setModules(modules);
        Files.write(target.resolve(ProjectDescriptor.FILE_NAME), declared.toBytes());
    }

    /**
     * The modules of a project written as folders, and the modules kept as workbooks.
     *
     * @param converted the modules written as folders
     * @param kept      the modules kept as workbooks
     */
    public record Written(List<Converted> converted, List<Kept> kept) {
    }

    /** What became of a module read from a workbook. */
    private sealed interface Outcome permits Converted, Kept {
    }

    /**
     * A module written as a folder of Markdown files.
     *
     * @param name          the name of the module
     * @param workbook      the path of its workbook in the project
     * @param folder        the path of its folder in the copy
     * @param files         how many files the folder holds
     * @param workbookBytes the size of the workbook
     * @param markdownBytes the size of the files of the folder
     */
    public record Converted(String name, String workbook, String folder, int files, long workbookBytes,
                            long markdownBytes) implements Outcome {
    }

    /**
     * A module kept as a workbook.
     *
     * @param name    the name of the module
     * @param path    the path of its workbook in the project
     * @param reasons why it stays a workbook: the tables a Markdown file cannot hold, each with the reason
     */
    public record Kept(String name, String path, List<String> reasons) implements Outcome {
    }
}
