package org.openl.studio.projects.service.tables;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.excel.grid.MarkdownModuleWriter;
import org.openl.excel.parser.markdown.MarkdownGrid;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.util.FileTypeHelper;

/**
 * The files of the tables of a Markdown module, a table to a file.
 *
 * <p>A new table of a module folder is a new file named after the table, in the folder its sheet names: the module
 * folder itself, by its name or by none, or a folder in it, made when it does not exist yet. The first table of a new
 * module goes the same way. A name taken by another file gets a number, {@code premium1.md}. A table set up for the
 * whole module — the Environment, the Constants — is written in the file of its fixed name at the root of the folder,
 * which the module may hold once. A Properties table is a table like any other: one of a category stands in the folder
 * of the category.
 *
 * <p>A table is deleted with its file. A module folder holds at least one file, so the last table of a module stays.
 *
 * <p>A file is created and deleted through the files of the project, so the project is marked modified as by any file
 * written.
 */
@Service
@RequiredArgsConstructor
public class MarkdownTableFiles {

    private static final String EXTENSION = ".md";
    private static final Map<TableKind, String> MODULE_TABLES = Map.of(TableKind.ENVIRONMENT, "Environment",
            TableKind.CONSTANTS, "Constants");

    private final ProjectFilesService projectFilesService;
    private final ProjectFileRootFactory projectFileRootFactory;

    /**
     * Returns the folder of a module written as a folder of Markdown files, or {@code null} for any other module.
     */
    public static @Nullable Path folderOf(@Nullable Module module) {
        if (module == null || module.getRulesRootPath() == null || module.getProject() == null) {
            return null;
        }
        var path = module.getRulesPath().normalize();
        return Files.isDirectory(path) ? path : null;
    }

    /**
     * Opens the sheet of a new file of a Markdown module, for a table to be written on. The save creates the file.
     *
     * @param projectModel the compiled module
     * @param folder       the folder of the module
     * @param sheetName    the sheet the table goes to: the folder of its file
     * @param tableName    the name of the table
     * @param kind         the kind of the table
     * @return the sheet to write the table on
     * @throws BadRequestException when the module holds the file of a table set up for the whole module already
     */
    public MarkdownGrid newFile(ProjectModel projectModel,
                                Path folder,
                                @Nullable String sheetName,
                                String tableName,
                                @Nullable TableKind kind) {
        var file = fileFor(folder, sheetName, tableName, kind);
        var moduleNode = Objects.requireNonNull(projectModel.getXlsModuleNode(), "The module is not compiled");
        var sibling = Arrays.stream(moduleNode.getWorkbookSyntaxNodes())
                .map(WorkbookSyntaxNode::getWorkbookSourceCodeModule)
                .filter(workbook -> isIn(workbook, folder))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The module folder holds no compiled file"));
        var project = projectModel.getProject();
        var path = inProject(projectModel.getModuleInfo(), file);
        return MarkdownGrid.createIn(sibling, folder, file, text -> create(project, path, text));
    }

    /**
     * Returns the text of the file of a table, laid out as a table is sent to be written.
     *
     * @param table the cells of the table, a merged range in its first cell
     * @throws BadRequestException when a merged range reaches past the table or overlaps another
     * @throws org.openl.excel.parser.markdown.MarkdownWriteException when the table has no Markdown form, such as a
     *                                                                cell holding an Excel formula
     */
    public static String textOf(RawTableView table) {
        var text = new String[1];
        var grid = MarkdownGrid.create("table.md", written -> text[0] = written);
        var merges = new ArrayList<GridRegion>();
        for (var row = 0; row < table.source.size(); row++) {
            var cells = table.source.get(row);
            for (var column = 0; column < cells.size(); column++) {
                var cell = cells.get(column);
                if (cell != null && !Boolean.TRUE.equals(cell.covered())) {
                    if (cell.formula() != null) {
                        // Written as its formula, as a table written whole into a workbook is: the grid refuses it
                        grid.setCellFormula(column, row, cell.formula());
                    } else {
                        grid.setCellValue(column, row, cell.value());
                    }
                    mergeOf(table, row, column, cell, merges).ifPresent(grid::addMergedRegion);
                }
            }
        }
        try {
            grid.save();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return text[0];
    }

    /**
     * Returns the range a cell is merged over, or nothing for a cell of its own.
     *
     * @param merges the ranges merged so far, which the range joins
     */
    private static Optional<GridRegion> mergeOf(RawTableView table,
                                                int row,
                                                int column,
                                                RawTableCell cell,
                                                List<GridRegion> merges) {
        var rowspan = cell.rowspan() == null ? 1 : cell.rowspan();
        var colspan = cell.colspan() == null ? 1 : cell.colspan();
        if (rowspan == 1 && colspan == 1) {
            return Optional.empty();
        }
        // Compared as the room left, so an oversized span cannot overflow past the check
        var inTable = rowspan <= table.getHeight() - row && colspan <= table.getWidth() - column;
        var range = new GridRegion(row, column, row + Math.min(rowspan, table.getHeight()) - 1,
                column + Math.min(colspan, table.getWidth()) - 1);
        if (!inTable || merges.stream().anyMatch(merged -> GridRegionUtils.intersects(merged, range))) {
            throw new BadRequestException("table.new-module.merge-range.message", new Object[]{row, column});
        }
        merges.add(range);
        return Optional.of(range);
    }

    /**
     * Returns the path of the file of the first table of a new module, as {@link #newFile} names it.
     *
     * @param folder    the project-relative folder of the module
     * @param sheetName the sheet the table goes to: the folder of its file
     * @throws BadRequestException when the sheet names a path
     */
    public static String firstFileOf(String folder,
                                     @Nullable String sheetName,
                                     String tableName,
                                     @Nullable TableKind kind) {
        var moduleTable = kind == null ? null : MODULE_TABLES.get(kind);
        if (moduleTable != null) {
            return folder + "/" + moduleTable + EXTENSION;
        }
        var sheetFolder = folderNamed(sheetName);
        var folderName = folder.substring(folder.lastIndexOf('/') + 1);
        return MarkdownModuleWriter.namesTheFolder(folderName, sheetFolder)
                ? folder + "/" + tableName + EXTENSION
                : folder + "/" + sheetFolder + "/" + tableName + EXTENSION;
    }

    /**
     * Deletes the file of a table of a Markdown module.
     *
     * @param module the module of the table
     * @throws BadRequestException when the table is the last of its module
     */
    public void delete(RulesProject project, @Nullable Module module, IOpenLTable table) {
        var known = Objects.requireNonNull(module, "The module is not known");
        var folder = folderOf(known);
        if (folder == null || markdownFiles(folder) < 2) {
            throw new BadRequestException("table.markdown.last-table.message", new Object[]{known.getName()});
        }
        projectFilesService.deleteResource(projectFileRootFactory.of(project), inProject(known, fileOf(table)));
    }

    private void create(RulesProject project, String path, String text) {
        projectFilesService.createResource(projectFileRootFactory.of(project),
                path,
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                true);
    }

    static Path fileFor(Path folder, @Nullable String sheetName, String tableName, @Nullable TableKind kind) {
        var moduleTable = kind == null ? null : MODULE_TABLES.get(kind);
        if (moduleTable != null) {
            var file = folder.resolve(moduleTable + EXTENSION);
            if (Files.exists(file)) {
                throw new BadRequestException("table.markdown.module-table.message", new Object[]{file.getFileName()});
            }
            return file;
        }
        return MarkdownModuleWriter.fileOf(folder, folderNamed(sheetName), tableName);
    }

    /**
     * Returns the name of the folder a sheet names, see {@link MarkdownModuleWriter#folderNameOf}.
     *
     * @return the name, empty for no sheet
     * @throws BadRequestException when the sheet names a path
     */
    private static String folderNamed(@Nullable String sheetName) {
        if (sheetName != null && (sheetName.contains("/") || sheetName.contains("\\") || sheetName.contains(".."))) {
            throw new BadRequestException("table.sheet-name.invalid.message", new Object[]{sheetName});
        }
        return MarkdownModuleWriter.folderNameOf(sheetName);
    }

    private static long markdownFiles(Path folder) {
        try (var files = Files.find(folder, Integer.MAX_VALUE,
                (path, attributes) -> attributes.isRegularFile() && FileTypeHelper.isMarkdownFile(path.toString()))) {
            return files.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path fileOf(IOpenLTable table) {
        var file = Optional.ofNullable(table.getSyntaxNode().getXlsSheetSourceCodeModule())
                .map(XlsSheetSourceCodeModule::getWorkbookSource)
                .map(XlsWorkbookSourceCodeModule::getSourceFile)
                .orElseThrow(() -> new IllegalStateException("The table is read from no file"));
        return file.toPath().toAbsolutePath().normalize();
    }

    private static boolean isIn(XlsWorkbookSourceCodeModule workbook, Path folder) {
        var file = workbook.getSourceFile();
        return file != null && file.toPath().toAbsolutePath().normalize().startsWith(folder);
    }

    private static String inProject(@Nullable Module module, Path file) {
        var projectFolder = Objects.requireNonNull(module, "The module is not known").getProject().getProjectFolder();
        return projectFolder.toAbsolutePath().normalize().relativize(file).toString().replace('\\', '/');
    }
}
