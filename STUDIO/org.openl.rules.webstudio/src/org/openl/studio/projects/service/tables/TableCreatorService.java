package org.openl.studio.projects.service.tables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.table.xls.formatters.FormatConstants;
import org.openl.rules.table.xls.writers.XlsCellArrayWriter;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.write.RawTableWriter;
import org.openl.studio.projects.service.tables.write.TableWriter;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableCreatorService {

    private static final int TABLE_START_ROW = 1;
    private static final int TABLE_START_COLUMN = 1;
    /** What OpenL reads a properties section by. */
    private static final String PROPERTIES_SECTION = "properties";

    private final TableWritersFactory tableWritersFactory;
    private final TableWriterExecutor tableWriterExecutor;
    private final ProjectFilesService projectFilesService;
    private final ProjectFileRootFactory projectFileRootFactory;
    private final SystemPropertiesService systemPropertiesService;

    /**
     * Creates a table in an existing module.
     *
     * <p>The table is stamped as created once it is written, so its author and creation date are recorded the same
     * way for a table written here and for one copied.
     *
     * @return identifier of the table at its written position
     */
    public String createTable(CreateNewTableRequest createTableRequest, ProjectModel projectModel) {
        var table = (TableView) createTableRequest.table();
        requireTableName(table.name);

        var gridModel = sheetGridModel(projectModel, resolveSheetName(createTableRequest));
        var tableWriter = tableWritersFactory.getNewTableWriter(table, gridModel);
        if (carriesProperties(table.kind)) {
            tableWriter.stampWith(systemPropertiesService.onCreate());
        }
        return tableWriterExecutor.executeWrite(tableWriter, createTableRequest.table());
    }

    /**
     * Creates a module containing the supplied raw OpenL table.
     *
     * <p>The module is registered explicitly in {@code rules.xml} only when its path is not already covered by a
     * wildcard declaration. A descriptor-free simple project is converted to an explicit descriptor so its existing
     * root modules remain available together with a module created in a nested folder. A failure to update the
     * descriptor removes the newly created workbook.
     *
     * @param rawTable the table of the request, already narrowed to a raw source by the caller
     */
    public void createModuleWithTable(RulesProject project,
                                      ProjectDescriptor resolvedDescriptor,
                                      CreateNewTableRequest createTableRequest,
                                      RawTableView rawTable) throws ProjectException {
        createModule(project, resolvedDescriptor, createTableRequest.moduleName(), createTableRequest.modulePath(),
                createWorkbook(createTableRequest, rawTable));
    }

    /**
     * Creates an empty module with a single blank sheet and registers it.
     *
     * <p>The module holds no table yet; the caller writes one into the sheet after the project is recompiled.
     * Registration follows the same rules as a module created with a table, and a failure to register removes the
     * workbook.
     *
     * @param moduleName name of the module to create
     * @param modulePath project-relative path of the module workbook
     * @param sheetName  name of the sheet the module holds
     */
    public void createEmptyModule(RulesProject project,
                                  ProjectDescriptor resolvedDescriptor,
                                  String moduleName,
                                  String modulePath,
                                  String sheetName) throws ProjectException {
        createModule(project, resolvedDescriptor, moduleName, modulePath, createEmptyWorkbook(sheetName));
    }

    /** Writes the module workbook to the project and registers it, removing the workbook when registration fails. */
    private void createModule(RulesProject project,
                              ProjectDescriptor resolvedDescriptor,
                              String moduleName,
                              String modulePath,
                              byte[] workbook) throws ProjectException {
        var root = projectFileRootFactory.of(project);
        projectFilesService.createResource(root, modulePath, new ByteArrayInputStream(workbook), true);
        try {
            registerModule(project, root, resolvedDescriptor, moduleName, modulePath);
        } catch (RuntimeException | ProjectException e) {
            rollbackModule(root, modulePath);
            throw e;
        }
    }

    private byte[] createEmptyWorkbook(String sheetName) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            createSheet(workbook, sheetName);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("table.new-module.workbook.message");
        }
    }

    /**
     * Rejects a table with no name.
     *
     * <p>A name is required: it is what the table header declares, and a table saved without one cannot be found
     * again by the caller that created it.
     */
    public void requireTableName(@Nullable String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new BadRequestException("table.name.required.message");
        }
    }

    /**
     * Creates a sheet, reporting a name Excel rejects as a client error.
     *
     * <p>A sheet name may not contain {@code / \ * ? [ ] :} or start or end with an apostrophe. Without this the
     * rejection surfaces as an unhandled runtime failure.
     */
    private static Sheet createSheet(Workbook workbook, String sheetName) {
        try {
            return workbook.createSheet(sheetName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("table.sheet-name.invalid.message", new Object[]{sheetName});
        }
    }

    /** The sheet the table goes to: the one the request names, or one named after the table itself. */
    private static String resolveSheetName(CreateNewTableRequest request) {
        return Optional.ofNullable(request.sheetName())
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> ((TableView) request.table()).name);
    }

    private byte[] createWorkbook(CreateNewTableRequest request, RawTableView table) {
        var stamped = withStampedProperties(table);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = createSheet(workbook, resolveSheetName(request));
            var dateStyle = dateStyle(workbook);
            for (var rowIndex = 0; rowIndex < stamped.source.size(); rowIndex++) {
                var sourceRow = stamped.source.get(rowIndex);
                // The same matrix reaches an existing module through RawTableWriter, which rejects a missing or
                // blank row there. A new module holds the table on its own, so the rule has to hold here too.
                RawTableWriter.requireWritableRow(sourceRow);
                var row = sheet.createRow(TABLE_START_ROW + rowIndex);
                for (var columnIndex = 0; columnIndex < sourceRow.size(); columnIndex++) {
                    var sourceCell = sourceRow.get(columnIndex);
                    if (sourceCell != null && !Boolean.TRUE.equals(sourceCell.covered())) {
                        writeCell(row.createCell(TABLE_START_COLUMN + columnIndex), sourceCell.value(), dateStyle);
                        mergeCell(sheet, stamped, rowIndex, columnIndex, sourceCell);
                    }
                }
            }
            requireNoBlankLine(sheet, stamped.getHeight(), stamped.getWidth());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("table.new-module.workbook.message");
        } catch (IllegalArgumentException e) {
            // What the payload asks for is more than a cell can hold — Excel caps a cell's text length. The request
            // is at fault, so it is answered as such rather than as a server failure.
            log.warn("Cannot lay out the table in a new workbook.", e);
            throw new BadRequestException("table.new-module.workbook.message");
        }
    }

    /**
     * The table's rows with the properties OpenL Studio records about a table it creates written under the header.
     *
     * <p>A table written into an existing module is stamped by the writer that puts it there; a table that arrives
     * with its own module is laid out here, so the same rows are written here.
     *
     * <p>The header is widened to the properties section when the table is narrower than it: OpenL reads a table as
     * the rectangle its header spans, so a header narrower than the rows below would cut them off. A free-form
     * table is left alone — OpenL reads none of its rows as properties.
     */
    private RawTableView withStampedProperties(RawTableView table) {
        var stamped = systemPropertiesService.onCreate();
        if (stamped.isEmpty() || !carriesProperties(table.kind) || !takesAPropertiesSection(table)) {
            return table;
        }
        var width = Math.max(table.getWidth(), TableBuilder.PROPERTIES_MIN_WIDTH);
        var rows = new ArrayList<List<RawTableCell>>(table.source.size() + stamped.size());
        rows.add(headerSpanning(width, table.source.getFirst()));
        int written = 0;
        for (var property : stamped.entrySet()) {
            rows.add(propertyRow(property, written++ == 0 ? stamped.size() : 0, width));
        }
        rows.addAll(table.source.subList(1, table.source.size()));
        return RawTableView.builder().kind(table.kind).name(table.name).source(rows).build();
    }

    /**
     * Whether a properties section can be written under the table's header.
     *
     * <p>There has to be a header to write under, and a table that carries a properties section already keeps the one
     * it has: OpenL reads the first section alone, so a second one would leave the first one's rows in the body.
     */
    private static boolean takesAPropertiesSection(RawTableView table) {
        return table.source.size() > 1
                && firstCell(table.source.getFirst()) != null
                && !PROPERTIES_SECTION.equals(String.valueOf(firstCell(table.source.get(1))));
    }

    /** The value the row opens with, or {@code null} while the row opens with nothing. */
    private static @Nullable Object firstCell(@Nullable List<RawTableCell> row) {
        var cell = row == null || row.isEmpty() ? null : row.getFirst();
        return cell == null ? null : cell.value();
    }

    /**
     * The header row rewritten as one cell spanning the table's width, the way OpenL Studio writes a header.
     *
     * <p>A header that already declares a wider span keeps it, so a span reaching past the table is still refused
     * rather than quietly narrowed to fit.
     */
    private static List<RawTableCell> headerSpanning(int width, List<RawTableCell> header) {
        var declared = header.getFirst();
        var span = Math.max(width, Optional.ofNullable(declared.colspan()).orElse(1));
        var row = new ArrayList<RawTableCell>(width);
        row.add(RawTableCell.builder().value(declared.value()).colspan(span).build());
        while (row.size() < width) {
            row.add(RawTableCell.COVERED_CELL);
        }
        return row;
    }

    /**
     * Whether OpenL reads a properties section on this kind of table.
     *
     * <p>A free-form block, an Environment table and a Properties table carry none: a row OpenL does not read as a
     * property is read as one of their own rows instead, and the module stops compiling.
     */
    private static boolean carriesProperties(@Nullable TableKind kind) {
        return kind != TableKind.OTHER && kind != TableKind.ENVIRONMENT && kind != TableKind.PROPERTIES;
    }

    /**
     * A cell style a date reads back from.
     *
     * <p>Excel stores a date as a number, and OpenL reads that number as a date only while the cell is formatted as
     * one — a date written without a format comes back as the number it is stored as.
     */
    private static CellStyle dateStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        style.setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
        return style;
    }

    /**
     * One row of the properties section: the marker, the property name, and the value up to the table's right edge.
     *
     * @param markerHeight number of rows the {@code properties} marker spans, or {@code 0} below the first row
     * @param width        width of the table the row belongs to
     */
    private static List<RawTableCell> propertyRow(Map.Entry<String, Object> property, int markerHeight, int width) {
        var marker = markerHeight > 0
                ? RawTableCell.builder().value(PROPERTIES_SECTION).rowspan(markerHeight).build()
                : RawTableCell.COVERED_CELL;
        var row = new ArrayList<RawTableCell>(width);
        row.add(marker);
        row.add(RawTableCell.builder().value(property.getKey()).build());
        row.add(RawTableCell.builder().value(property.getValue()).colspan(width - 2).build());
        while (row.size() < width) {
            row.add(RawTableCell.COVERED_CELL);
        }
        return row;
    }

    /**
     * Rejects a table that reached the sheet with a blank row or a blank column.
     *
     * <p>OpenL reads a table only as far as its first blank line, so a blank row drops the rows below it and a
     * blank column drops the columns beyond it. A payload whose rows are all filled can still leave a column
     * empty across every one of them, and a row of nothing but covered cells reaches the sheet empty unless a
     * merge declared above really spans into it.
     *
     * <p>Checked once the whole table is on the sheet: a line is only blank when no cell and no merge filled it.
     *
     * <p>Answers the same payload the same way as a write into an existing module, which reaches
     * {@link TableWriter#hasBlankLineInside} over a grid instead of over a sheet.
     *
     * @param height number of rows the table declares
     * @param width  number of columns the table declares
     */
    private static void requireNoBlankLine(Sheet sheet, int height, int width) {
        // Read once: a sheet rebuilds its merged regions on every call, and a blank cell asks for them.
        var merges = sheet.getMergedRegions();
        IntPredicate rowFilled = row -> IntStream.range(0, width)
                .anyMatch(column -> carriesContent(sheet, merges, column, row));
        IntPredicate columnFilled = column -> IntStream.range(0, height)
                .anyMatch(row -> carriesContent(sheet, merges, column, row));
        if (TableWriter.hasBlankLineInside(0, height - 1, rowFilled)
                || TableWriter.hasBlankLineInside(0, width - 1, columnFilled)) {
            throw new BadRequestException("table.action.line.all-empty.message");
        }
    }

    /**
     * Tells whether OpenL reads the cell as part of the table.
     *
     * <p>A cell carries content when it holds a value of its own, or when a merge spanning it starts from a cell
     * that holds one. Mirrors what the grid reports for a table already on a sheet, including that a text cell of
     * nothing but whitespace is empty — a table that passed a laxer check here would be read back short.
     *
     * @param merges the sheet's merged regions
     * @param column column of the cell, relative to the table
     * @param row    row of the cell, relative to the table
     */
    private static boolean carriesContent(Sheet sheet, List<CellRangeAddress> merges, int column, int row) {
        var cellRow = TABLE_START_ROW + row;
        var cellColumn = TABLE_START_COLUMN + column;
        if (!isBlank(cellAt(sheet, cellColumn, cellRow))) {
            return true;
        }
        return merges.stream()
                .filter(region -> region.isInRange(cellRow, cellColumn))
                .anyMatch(region -> !isBlank(cellAt(sheet, region.getFirstColumn(), region.getFirstRow())));
    }

    private static @Nullable Cell cellAt(Sheet sheet, int column, int row) {
        var sheetRow = sheet.getRow(row);
        return sheetRow == null ? null : sheetRow.getCell(column);
    }

    /** Tells whether the cell holds nothing OpenL would read, as {@code XlsSheetGridModel} judges it. */
    private static boolean isBlank(@Nullable Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank();
    }

    /**
     * Writes one cell of the matrix, choosing the cell kind the grid's own writers would choose.
     *
     * <p>A text opening with {@code =} is a formula, as {@link XlsSheetGridModel} reads it. Without that the same
     * table would hold a formula in an existing module and the literal text in a new one.
     */
    private static void writeCell(Cell cell, @Nullable Object value, CellStyle dateStyle) {
        switch (value) {
            case null -> cell.setBlank();
            case Boolean booleanValue -> cell.setCellValue(booleanValue);
            case Number numberValue -> cell.setCellValue(numberValue.doubleValue());
            case Collection<?> values -> cell.setCellValue(XlsCellArrayWriter.serialize(values.toArray()));
            case Object[] values -> cell.setCellValue(XlsCellArrayWriter.serialize(values));
            case Date dateValue -> {
                cell.setCellValue(dateValue);
                cell.setCellStyle(dateStyle);
            }
            case String text when text.startsWith("=") -> writeFormula(cell, text);
            default -> cell.setCellValue(value.toString());
        }
    }

    /**
     * Writes an Excel formula, keeping the text as it stands when Excel cannot parse it.
     *
     * <p>OpenL expressions share the leading {@code =} with Excel formulas, so one Excel rejects is written as the
     * text it is rather than failing the request.
     */
    private static void writeFormula(Cell cell, String formula) {
        try {
            cell.setCellFormula(formula.substring(1));
            PoiExcelHelper.evaluateFormula(cell);
        } catch (RuntimeException e) {
            cell.setBlank();
            cell.setCellValue(formula);
        }
    }

    private static void mergeCell(Sheet sheet, RawTableView table, int row, int column, RawTableCell cell) {
        var rowSpan = Optional.ofNullable(cell.rowspan()).orElse(1);
        var columnSpan = Optional.ofNullable(cell.colspan()).orElse(1);
        if (rowSpan == 1 && columnSpan == 1) {
            return;
        }
        // Compared as remaining space rather than as an end position, so an oversized span cannot overflow past the
        // check.
        if (rowSpan > table.getHeight() - row || columnSpan > table.getWidth() - column) {
            throw new BadRequestException("table.new-module.merge-range.message", new Object[]{row, column});
        }
        try {
            sheet.addMergedRegion(new CellRangeAddress(
                    TABLE_START_ROW + row,
                    TABLE_START_ROW + row + rowSpan - 1,
                    TABLE_START_COLUMN + column,
                    TABLE_START_COLUMN + column + columnSpan - 1));
        } catch (IllegalStateException e) {
            // Overlaps a region declared by an earlier cell.
            throw new BadRequestException("table.new-module.merge-range.message", new Object[]{row, column});
        }
    }

    /**
     * Makes the project aware of the new module, writing rules.xml only when it has to.
     *
     * <p>A wildcard names every module it matches after its file. When that is the name the caller asked for, the
     * module stays auto-discovered and the descriptor is left alone.
     *
     * <p>In every other case the module is declared and the descriptor written. A wildcard matching the file under a
     * different name would lose the requested name, and a file no wildcard matches would not be found at all. An
     * explicit declaration is resolved before wildcards, so it also suppresses the duplicate a wildcard contributes.
     */
    private void registerModule(RulesProject project,
                                FileRoot root,
                                ProjectDescriptor resolvedDescriptor,
                                String moduleName,
                                String modulePath) throws ProjectException {
        var descriptor = readDescriptor(project, root, resolvedDescriptor);
        var module = new Module();
        module.setName(moduleName);
        module.setRulesRootPath(modulePath);
        var descriptorManager = new ProjectDescriptorManager();
        if (descriptorManager.isAlreadyRegistered(descriptor, module)) {
            // The wildcard already names the module exactly as asked, so it stays auto-discovered.
            return;
        }
        descriptorManager.declareModule(descriptor, module);

        var content = new ByteArrayInputStream(descriptor.toBytes());
        if (project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            projectFilesService.updateResource(root, ProjectDescriptor.FILE_NAME, content);
        } else {
            projectFilesService.createResource(root, ProjectDescriptor.FILE_NAME, content, false);
        }
    }

    private ProjectDescriptor readDescriptor(RulesProject project,
                                             FileRoot root,
                                             ProjectDescriptor resolvedDescriptor) throws ProjectException {
        if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            var descriptor = new ProjectDescriptor();
            descriptor.setName(project.getBusinessName());
            descriptor.setModules(new ArrayList<>(resolvedDescriptor.getModules()
                    .stream()
                    .map(TableCreatorService::copyModule)
                    .toList()));
            return descriptor;
        }

        var resource = projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null);
        var content = resource.getContent();
        ProjectDescriptor descriptor;
        try {
            descriptor = ProjectDescriptor.read(content);
        } catch (RuntimeException e) {
            log.warn("Cannot read the project descriptor.", e);
            throw new BadRequestException("table.new-module.descriptor.message");
        } finally {
            IOUtils.closeQuietly(content);
        }
        // Checked outside the block above so the rejection is not caught and relabelled by it.
        if (descriptor == null) {
            throw new BadRequestException("table.new-module.descriptor.message");
        }
        return descriptor;
    }

    private static Module copyModule(Module source) {
        var module = new Module();
        module.setName(source.getName());
        module.setRulesRootPath(source.getRulesRootPath());
        return module;
    }

    private void rollbackModule(FileRoot root, String modulePath) {
        try {
            projectFilesService.deleteResource(root, modulePath);
        } catch (RuntimeException rollbackFailure) {
            log.warn("Failed to remove module '{}' after table creation failed", modulePath, rollbackFailure);
        }
    }

    /**
     * Removes a module a table write created and then failed to fill.
     *
     * <p>Deletes the module workbook and drops its declaration from {@code rules.xml}, so a create or copy that fails
     * after the module was registered leaves no phantom module behind. Best-effort: a failure to revert is logged, not
     * propagated, so it never masks the write failure that triggered the rollback.
     *
     * @param moduleName name of the module to remove
     * @param modulePath project-relative path of the module workbook
     */
    public void deleteModule(RulesProject project, String moduleName, String modulePath) {
        var root = projectFileRootFactory.of(project);
        rollbackModule(root, modulePath);
        try {
            undeclareModule(project, root, moduleName, modulePath);
        } catch (RuntimeException | ProjectException e) {
            log.warn("Failed to revert rules.xml after removing module '{}'", moduleName, e);
        }
    }

    /** Drops the module's declaration from {@code rules.xml}; a no-op when the module was only wildcard-discovered. */
    private void undeclareModule(RulesProject project,
                                 FileRoot root,
                                 String moduleName,
                                 String modulePath) throws ProjectException {
        if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            return;
        }
        var content = projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null).getContent();
        ProjectDescriptor descriptor;
        try {
            descriptor = ProjectDescriptor.read(content);
        } finally {
            IOUtils.closeQuietly(content);
        }
        if (descriptor == null || descriptor.getModules() == null) {
            return;
        }
        var normalizedPath = modulePath == null ? null : modulePath.replace('\\', '/');
        // Remove the exact module the rollback created: match by its path when one was given, so a same-named module
        // at a different path is left alone. Only a pathless module is matched by name.
        boolean removed = descriptor.getModules().removeIf(module -> normalizedPath != null
                ? module.getRulesRootPath() != null
                        && normalizedPath.equalsIgnoreCase(module.getRulesRootPath().replace('\\', '/'))
                : moduleName.equalsIgnoreCase(module.getName()));
        if (removed) {
            projectFilesService.updateResource(root, ProjectDescriptor.FILE_NAME,
                    new ByteArrayInputStream(descriptor.toBytes()));
        }
    }

    /**
     * The grid model of a sheet in the module's first workbook, creating the sheet when it is absent.
     *
     * @param projectModel the compiled module the copy is written to
     * @param sheetName    name of the sheet the copy goes to
     * @return the grid model to write the copy into
     */
    public XlsSheetGridModel sheetGridModel(ProjectModel projectModel, String sheetName) {
        var currentWorkbook = projectModel.getXlsModuleNode().getWorkbookSyntaxNodes()[0]
                .getWorkbookSourceCodeModule();

        var excelWorkbook = currentWorkbook.getWorkbook();
        var sheet = excelWorkbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = createSheet(excelWorkbook, sheetName);
        }

        var sourceCodeModule = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), currentWorkbook);
        return new XlsSheetGridModel(sourceCodeModule);
    }

    /**
     * Persist the workbook a table was written into, dropping the styles the write left unused.
     *
     * @param gridModel the grid model the copy was written into
     */
    public void save(XlsSheetGridModel gridModel) {
        TableWriter.saveWorkbook(gridModel);
    }
}
