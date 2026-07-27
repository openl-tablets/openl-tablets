package org.openl.studio.projects.service.tables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.write.RawTableWriter;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableCreatorService {

    private static final int TABLE_START_ROW = 1;
    private static final int TABLE_START_COLUMN = 1;

    private final TableWritersFactory tableWritersFactory;
    private final TableWriterExecutor tableWriterExecutor;
    private final ProjectFilesService projectFilesService;
    private final ProjectFileRootFactory projectFileRootFactory;

    /**
     * Creates a table in an existing module.
     *
     * @return identifier of the table at its written position
     */
    public String createTable(CreateNewTableRequest createTableRequest, ProjectModel projectModel) {
        var table = (TableView) createTableRequest.table();
        requireUniqueTable(projectModel, table.name);

        var gridModel = getXlsSheetGridModel(createTableRequest, projectModel);
        var tableWriter = tableWritersFactory.getNewTableWriter(table, gridModel);
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
        var modulePath = createTableRequest.modulePath();
        var root = projectFileRootFactory.of(project);
        var workbook = createWorkbook(createTableRequest, rawTable);
        projectFilesService.createResource(root, modulePath, new ByteArrayInputStream(workbook), true);
        try {
            registerModule(project, root, resolvedDescriptor, createTableRequest.moduleName(), modulePath);
        } catch (RuntimeException | ProjectException e) {
            rollbackModule(root, modulePath);
            throw e;
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

    /** Rejects a table with no name, or one whose name another table in the project already holds. */
    public void requireUniqueTable(ProjectModel model, @Nullable String tableName) {
        requireTableName(tableName);
        var taken = model.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNode::getMember)
                .filter(Objects::nonNull)
                .anyMatch(member -> tableName.equalsIgnoreCase(member.getName()));
        if (taken) {
            throw new ConflictException("table.exists.message", tableName);
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
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = createSheet(workbook, resolveSheetName(request));
            for (var rowIndex = 0; rowIndex < table.source.size(); rowIndex++) {
                var sourceRow = table.source.get(rowIndex);
                // The same matrix reaches an existing module through RawTableWriter, which rejects a missing or
                // blank row there. A new module holds the table on its own, so the rule has to hold here too.
                RawTableWriter.requireWritableRow(sourceRow);
                var row = sheet.createRow(TABLE_START_ROW + rowIndex);
                for (var columnIndex = 0; columnIndex < sourceRow.size(); columnIndex++) {
                    var sourceCell = sourceRow.get(columnIndex);
                    if (sourceCell != null && !Boolean.TRUE.equals(sourceCell.covered())) {
                        writeCell(row.createCell(TABLE_START_COLUMN + columnIndex), sourceCell.value());
                        mergeCell(sheet, table, rowIndex, columnIndex, sourceCell);
                    }
                }
                requireRowOnSheet(sheet, row);
            }
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
     * Rejects a row that reached the sheet blank.
     *
     * <p>A row of nothing but covered cells is legitimate while a merge declared above really spans into it. When
     * none does, the covered flags describe a coverage that does not exist and the row is written empty — and OpenL
     * reads a blank row as the end of the table, so every row below it is lost from the table being created.
     *
     * <p>The merges declared by earlier rows are already on the sheet by the time a row is checked.
     */
    private static void requireRowOnSheet(Sheet sheet, Row row) {
        if (carriesValue(row) || isCoveredFromAbove(sheet, row.getRowNum())) {
            return;
        }
        throw new BadRequestException("table.action.line.all-empty.message");
    }

    /**
     * Tells whether the row holds anything OpenL would read.
     *
     * <p>A cell written blank is still one of the row's cells, so counting them would take a row of nothing but
     * blanks for a filled one.
     */
    private static boolean carriesValue(Row row) {
        return StreamSupport.stream(row.spliterator(), false)
                .anyMatch(cell -> cell.getCellType() != CellType.BLANK);
    }

    /** Tells whether a merge starting on an earlier row spans into this one. */
    private static boolean isCoveredFromAbove(Sheet sheet, int rowNumber) {
        return sheet.getMergedRegions()
                .stream()
                .anyMatch(region -> region.getFirstRow() < rowNumber && rowNumber <= region.getLastRow());
    }

    /**
     * Writes one cell of the matrix, choosing the cell kind the grid's own writers would choose.
     *
     * <p>A text opening with {@code =} is a formula, as {@link XlsSheetGridModel} reads it. Without that the same
     * table would hold a formula in an existing module and the literal text in a new one.
     */
    private static void writeCell(Cell cell, @Nullable Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case Boolean booleanValue -> cell.setCellValue(booleanValue);
            case Number numberValue -> cell.setCellValue(numberValue.doubleValue());
            case Date dateValue -> cell.setCellValue(dateValue);
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
        if (descriptorManager.isCoveredByWildcardModule(descriptor, module)
                && module.getName().equals(FileUtils.getBaseName(modulePath))) {
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

    private static XlsSheetGridModel getXlsSheetGridModel(CreateNewTableRequest createTableRequest,
                                                         ProjectModel projectModel) {
        var currentWorkbook = projectModel.getXlsModuleNode().getWorkbookSyntaxNodes()[0]
                .getWorkbookSourceCodeModule();

        var excelWorkbook = currentWorkbook.getWorkbook();
        var sheetName = resolveSheetName(createTableRequest);
        var sheet = excelWorkbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = createSheet(excelWorkbook, sheetName);
        }

        var sourceCodeModule = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), currentWorkbook);
        return new XlsSheetGridModel(sourceCodeModule);
    }
}
