package org.openl.studio.projects.service.tables;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceDependencyManagerFactory;
import org.openl.studio.projects.service.tables.format.ExcelTableFormatter;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;

/**
 * Formats the Excel files an upload brings into a project with the OpenL table style, while tables are formatted on
 * save.
 *
 * <p>Nothing is known of an uploaded workbook's tables until it is compiled. The project, as the upload leaves it, is
 * staged in a folder of its own, and each module holding an uploaded workbook is compiled there once, with the
 * dependencies of the project resolved as for a project opened in OpenL Studio. The tables of the uploaded workbooks
 * are formatted, and the workbooks saved in the stage.
 *
 * <p>Only {@code .xlsx} files are formatted, and a table gathered from partial tables is left as it is. A workbook that
 * cannot be formatted, for example because its module does not compile, is left as it was uploaded: formatting never
 * fails an upload.
 *
 * @author Yury Molchan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFormatting {

    private final TableFormatting formatting;

    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * Whether an upload of the paths is formatted: while tables are formatted on save, and only for {@code .xlsx}
     * files.
     *
     * @param paths the uploaded files, by their paths relative to the project
     * @return whether any of them is formatted
     */
    public boolean formats(Collection<String> paths) {
        return formatting.onSave() && !excelPaths(paths).isEmpty();
    }

    /**
     * Formats the uploaded workbooks of a project opened in OpenL Studio, compiled as the session compiles it.
     *
     * <p>The upload is already written into the working copy, which stages it until the project is saved. The
     * workbooks are saved there again as a change of their look alone, so the compile goes on answering for them.
     *
     * @param model    the project, compiled whole with the upload
     * @param folder   the folder of the working copy of the project
     * @param uploaded the uploaded files, by their paths relative to the project
     * @throws UncheckedIOException if a workbook cannot be saved: the session holds workbooks no file has
     */
    public void formatOpened(ProjectModel model, Path folder, Collection<String> uploaded) {
        var workbooks = excelPaths(uploaded).stream()
                .map(path -> real(folder.resolve(path)))
                .collect(Collectors.toSet());
        if (!formatting.onSave() || workbooks.isEmpty()) {
            return;
        }
        var formatter = new ExcelTableFormatter();
        Set<XlsWorkbookSourceCodeModule> saved = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var node : model.getAllTableSyntaxNodes()) {
            var source = node.getXlsSheetSourceCodeModule().getWorkbookSource();
            if (workbooks.contains(pathOf(source)) && format(formatter, new TableSyntaxNodeAdapter(node))) {
                saved.add(source);
            }
        }
        try {
            for (var workbook : saved) {
                workbook.saveLook();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The files an upload writes into a project kept in the design repository, each {@code .xlsx} one formatted.
     *
     * <p>The project is staged as it stands with the uploaded files written over it; an upload that replaces a folder
     * leaves in it only what it uploads.
     *
     * @param current  the project as it stands, before the upload
     * @param base     the folder the upload writes into, relative to the project; empty for the project itself
     * @param uploaded the uploaded files, by their paths relative to the project
     * @param replace  whether the upload replaces the folder, removing the files it does not upload
     * @return the uploaded files, in the same order, each {@code .xlsx} one formatted where it could be
     */
    public Map<String, byte[]> formatStaged(AProjectFolder current, String base, Map<String, byte[]> uploaded,
                                            boolean replace) {
        if (!formatting.onSave() || excelPaths(uploaded.keySet()).isEmpty()) {
            return uploaded;
        }
        Path stage = null;
        try {
            stage = FileTool.createTempDirectory("openl-upload-");
            copy(current, stage, file -> !replace || !isUnder(file, base) || uploaded.containsKey(file));
            for (var file : uploaded.entrySet()) {
                write(inStage(stage, file.getKey()), file.getValue());
            }
            var formatted = formatStaged(stage, uploaded.keySet());
            var written = new LinkedHashMap<String, byte[]>(uploaded);
            for (var path : formatted) {
                written.put(path, Files.readAllBytes(inStage(stage, path)));
            }
            return written;
        } catch (IOException | ProjectException | RuntimeException e) {
            log.warn("The uploaded Excel files are written as uploaded.", e);
            return uploaded;
        } finally {
            if (stage != null) {
                FileUtils.deleteQuietly(stage);
            }
        }
    }

    /**
     * Formats the uploaded workbooks of a project staged in a folder, in the folder.
     *
     * @param project  the folder the project is staged in, as the upload leaves it
     * @param uploaded the uploaded files, by their paths relative to the folder
     * @return the paths of the workbooks formatted
     */
    public Set<String> formatStaged(Path project, Collection<String> uploaded) {
        var workbooks = excelPaths(uploaded).stream()
                .collect(Collectors.toMap(path -> real(project.resolve(path)), path -> path));
        if (!formatting.onSave() || workbooks.isEmpty()) {
            return Set.of();
        }
        try {
            var descriptor = ProjectResolver.getInstance().resolve(project);
            var manager = new WebStudioWorkspaceDependencyManagerFactory(getWebStudio())
                    .buildDependencyManager(descriptor);
            try {
                var formatted = new HashSet<String>();
                for (var module : descriptor.getModules()) {
                    var workbook = workbooks.get(real(module.getRulesPath()));
                    if (workbook != null && format(manager.loadDependency(
                            AbstractDependencyManager.buildResolvedDependency(module)).getCompiledOpenClass()
                            .getOpenClassWithErrors().getMetaInfo(), module.getRulesPath())) {
                        formatted.add(workbook);
                    }
                }
                return formatted;
            } finally {
                manager.shutdown();
            }
        } catch (IOException | ProjectResolvingException | OpenLCompilationException | RuntimeException e) {
            log.warn("The uploaded Excel files are written as uploaded.", e);
            return Set.of();
        }
    }

    /**
     * Formats every table of a workbook a module compiled, and saves the workbook once. A table the module takes from
     * another workbook, which it includes, is left as it is.
     */
    private static boolean format(IMetaInfo metaInfo, Path workbookPath) throws IOException {
        if (!(metaInfo instanceof XlsMetaInfo meta) || meta.getXlsModuleNode() == null) {
            return false;
        }
        var formatter = new ExcelTableFormatter();
        XlsWorkbookSourceCodeModule workbook = null;
        for (TableSyntaxNode node : meta.getXlsModuleNode().getXlsTableSyntaxNodes()) {
            var source = node.getXlsSheetSourceCodeModule().getWorkbookSource();
            if (real(workbookPath).equals(pathOf(source)) && format(formatter, new TableSyntaxNodeAdapter(node))) {
                workbook = source;
            }
        }
        if (workbook == null) {
            return false;
        }
        workbook.save();
        return true;
    }

    /**
     * Formats a table in its workbook. A table gathered from partial tables is left as it is, as Reformat leaves it,
     * and so is a table that stands on no grid.
     */
    static boolean format(ExcelTableFormatter formatter, IOpenLTable table) {
        var source = table.getGridTable();
        if (source == null || GridTableUtils.isAssembledFromParts(source)) {
            return false;
        }
        var grid = GridTableUtils.getOriginalTable(source);
        grid.edit();
        try {
            return formatter.format(table, grid);
        } finally {
            grid.stopEditing();
        }
    }

    /** The file a workbook is read from, or {@code null} for a workbook read from no file. */
    private static @Nullable Path pathOf(XlsWorkbookSourceCodeModule source) {
        var file = source.getSourceFile();
        return file == null ? null : real(file.toPath());
    }

    /**
     * The path of a file as the file system resolves it, so two paths of one file are equal: a temporary folder may
     * be reached through a link.
     */
    private static Path real(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    /** The paths of the {@code .xlsx} files among the paths. */
    private static Set<String> excelPaths(Collection<String> paths) {
        return paths.stream()
                .filter(path -> path.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                .collect(Collectors.toSet());
    }

    private static boolean isUnder(String path, String folder) {
        return folder.isEmpty() || path.startsWith(folder + "/");
    }

    /** Copies the files of a project folder the filter keeps into a folder, by their paths relative to the project. */
    private static void copy(AProjectFolder folder, Path into, Predicate<String> keeps)
            throws IOException, ProjectException {
        for (AProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact instanceof AProjectFolder child) {
                copy(child, into, keeps);
            } else if (artefact instanceof AProjectResource resource && keeps.test(resource.getInternalPath())) {
                try (var content = resource.getContent()) {
                    var target = into.resolve(resource.getInternalPath());
                    Files.createDirectories(target.getParent());
                    Files.copy(content, target);
                }
            }
        }
    }

    /**
     * The file of the stage an uploaded path names. A path that leaves the stage is refused, and the upload is then
     * written as it was uploaded.
     */
    private static Path inStage(Path stage, String path) {
        var file = stage.resolve(path).normalize();
        if (!file.startsWith(stage)) {
            throw new IllegalArgumentException("An uploaded file leaves the project: " + path);
        }
        return file;
    }

    private static void write(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }
}
