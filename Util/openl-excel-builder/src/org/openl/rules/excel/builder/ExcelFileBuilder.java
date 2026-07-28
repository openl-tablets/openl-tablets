package org.openl.rules.excel.builder;

import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;
import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import org.openl.rules.excel.builder.export.DataTableExporter;
import org.openl.rules.excel.builder.export.DatatypeTableExporter;
import org.openl.rules.excel.builder.export.EnvironmentTableExporter;
import org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter;
import org.openl.rules.excel.builder.template.ExcelTemplateUtils;
import org.openl.rules.excel.builder.template.SpreadsheetTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.util.CollectionUtils;

/**
 * Build the xlsx datatype spreadsheet from the given data type list
 */
@Slf4j
public class ExcelFileBuilder {


    private ExcelFileBuilder() {
    }

    /**
     * Generating xlsx file with project which contains data types and spreadsheets.
     *
     * @param projectModel - model of the project.
     */
    public static void generateProject(ProjectModel projectModel) {
        var projectName = projectModel.getName();
        var fileName = projectName + ".xlsx";
        try (var fos = new FileOutputStream(fileName)) {
            writeProject(projectModel, fos);
        } catch (IOException e) {
            log.error("Error on saving the file occurred.", e);
        }
    }

    /**
     * Generate project model to the the output stream.
     *
     * @param projectModel - model of the project with data types and spreadsheets.
     * @param outputStream - output stream, which contains result file.
     */
    public static void generateProject(ProjectModel projectModel, OutputStream outputStream) {
        writeProject(projectModel, outputStream);
    }

    /**
     * Generate data type to the output stream.
     *
     * @param datatypeModels - data type models.
     * @param outputStream   - output stream with models.
     */
    public static void generateDataTypes(Set<DatatypeModel> datatypeModels, OutputStream outputStream) {
        writeDataTypes(datatypeModels, outputStream);
    }

    public static void generateDataTables(List<DataModel> dataModels, OutputStream outputStream) {
        writeDataTables(dataModels, outputStream);
    }

    /**
     * Generate spreadsheets to the output stream.
     *
     * @param spreadsheetModels - spreadsheet models.
     * @param outputStream      - output stream with models.
     */
    public static void generateSpreadsheets(List<SpreadsheetModel> spreadsheetModels, OutputStream outputStream) {
        writeSpreadsheets(spreadsheetModels, outputStream);
    }

    /**
     * Generate spreadsheets with environment.
     *
     * @param spreadsheetModels - spreadsheet models.
     * @param outputStream      - output stream with models.
     */
    public static void generateAlgorithmsModule(List<SpreadsheetModel> spreadsheetModels,
                                                List<DataModel> dataModels,
                                                OutputStream outputStream,
                                                EnvironmentModel model) {
        writeAlgorithmsModule(spreadsheetModels, dataModels, outputStream, model);
    }

    /**
     * Writing models to Excel file with styles from template.
     *
     * @param datatypeModels
     * @param outputStream
     */
    private static void writeDataTypes(Set<DatatypeModel> datatypeModels, OutputStream outputStream) {
        try (SXSSFWorkbook workbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            var dtSheet = workbook.createSheet(DATATYPES_SHEET);
            var datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            var datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);
            datatypeTableExporter.export(datatypeModels, dtSheet);
            dtSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error on generating DataTypes workbook occurred.", e);
        }
    }

    private static void writeDataTables(List<DataModel> dataModels, OutputStream outputStream) {
        try (SXSSFWorkbook workbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            var dtSheet = workbook.createSheet(DATA_SHEET);
            var datatypeStyles = stylesMap.get(DATA_SHEET);
            var dataTableExporter = new DataTableExporter();
            dataTableExporter.setTableStyle(datatypeStyles);
            dataTableExporter.export(dataModels, dtSheet);
            dtSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error on generating Data tables workbook occurred.", e);
        }
    }

    /**
     * Writing spreadsheets to Excel file with styles from template.
     *
     * @param spreadsheetModels
     * @param outputStream
     */
    private static void writeSpreadsheets(List<SpreadsheetModel> spreadsheetModels, OutputStream outputStream) {
        try (SXSSFWorkbook workbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            var sprStyles = stylesMap.get(SPR_RESULT_SHEET);
            writeSpreadsheets(spreadsheetModels, workbook, sprStyles);
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error on generating Spreadsheet workbook occurred.", e);
        }
    }

    private static void writeSpreadsheets(List<SpreadsheetModel> spreadsheetModels,
                                          SXSSFWorkbook workbook,
                                          TableStyle tableStyle) {
        var sprSheet = workbook.createSheet(SPR_RESULT_SHEET);
        var sprTableExporter = new SpreadsheetResultTableExporter();
        var reservedWords = spreadsheetModels.stream()
                .map(SpreadsheetModel::getSteps)
                .flatMap(Collection::stream)
                .map(StepModel::getName)
                .collect(Collectors.toSet());
        editTextIfNeeded((SpreadsheetTableStyle) tableStyle, reservedWords);
        sprTableExporter.setTableStyle(tableStyle);
        sprTableExporter.export(spreadsheetModels, sprSheet);
        sprSheet.validateMergedRegions();
    }

    private static void editTextIfNeeded(SpreadsheetTableStyle tableStyle, Set<String> reservedWords) {
        var defaultValueHeader = tableStyle.getValueHeaderText();
        if (defaultValueHeader == null) {
            return;
        }
        String valueHeaderText = makeName(defaultValueHeader, reservedWords);
        if (!defaultValueHeader.equals(valueHeaderText)) {
            tableStyle.setValueHeaderText(valueHeaderText);
        }
    }

    private static String makeName(String text, Set<String> reservedWords) {
        if (CollectionUtils.isNotEmpty(reservedWords) && reservedWords.contains(text)) {
            text = text + "1";
            return makeName(text, reservedWords);
        }
        return text;
    }

    private static void writeDataTables(List<DataModel> dataModels, SXSSFWorkbook workbook, TableStyle tableStyle) {
        var dataTableSheet = workbook.createSheet(DATA_SHEET);
        var dtExporter = new DataTableExporter();
        dtExporter.setTableStyle(tableStyle);
        dtExporter.export(dataModels, dataTableSheet);
        dataTableSheet.validateMergedRegions();
    }

    private static void writeAlgorithmsModule(List<SpreadsheetModel> spreadsheetModels,
                                              List<DataModel> dataModels,
                                              OutputStream outputStream,
                                              EnvironmentModel environmentModel) {
        try (SXSSFWorkbook workbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            var sprStyle = stylesMap.get(SPR_RESULT_SHEET);
            var envStyle = stylesMap.get(ENV_SHEET);
            var dataTableStyle = stylesMap.get(DATA_SHEET);
            writeSpreadsheets(spreadsheetModels, workbook, sprStyle);
            writeEnvironment(environmentModel, workbook, envStyle);
            writeDataTables(dataModels, workbook, dataTableStyle);
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error on generating Spreadsheet workbook occurred.", e);
        }
    }

    private static void writeEnvironment(EnvironmentModel environmentModel, SXSSFWorkbook workbook, TableStyle style) {
        if (environmentModel == null) {
            return;
        }
        var envSheet = workbook.createSheet(ENV_SHEET);
        var environmentTableExporter = new EnvironmentTableExporter();
        environmentTableExporter.setTableStyle(style);
        environmentTableExporter.export(Collections.singletonList(environmentModel), envSheet);
        envSheet.validateMergedRegions();
    }

    /**
     * Writing project to Excel file with styles from template.
     *
     * @param projectModel - model of the project with data types, environment, spreadsheets
     * @param fos          - output stream.
     */
    private static void writeProject(ProjectModel projectModel, OutputStream fos) {
        try (SXSSFWorkbook workbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);

            var dtSheet = workbook.createSheet(DATATYPES_SHEET);
            var sprSheet = workbook.createSheet(SPR_RESULT_SHEET);
            var dataSheet = workbook.createSheet(DATA_SHEET);
            var datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            var sprStyles = stylesMap.get(SPR_RESULT_SHEET);
            var dataStyles = stylesMap.get(DATA_SHEET);

            var datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);

            var sprTableExporter = new SpreadsheetResultTableExporter();
            sprTableExporter.setTableStyle(sprStyles);

            var dataTableExporter = new DataTableExporter();
            dataTableExporter.setTableStyle(dataStyles);

            datatypeTableExporter.export(projectModel.getDatatypeModels(), dtSheet);
            sprTableExporter.export(projectModel.getSpreadsheetResultModels(), sprSheet);
            dataTableExporter.export(projectModel.getDataModels(), dataSheet);
            dtSheet.validateMergedRegions();
            sprSheet.validateMergedRegions();
            dataSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(fos);
        } catch (IOException e) {
            log.error("Error on generating workbook occurred.", e);
        }
    }

    /**
     * Make sheets readable.
     *
     * @param workbook - target document.
     */
    private static void autoSizeSheets(SXSSFWorkbook workbook) {
        var numberOfSheets = workbook.getNumberOfSheets();
        for (var i = 0; i < numberOfSheets; i++) {
            var sheet = workbook.getSheetAt(i);
            sheet.trackAllColumnsForAutoSizing();
            autoSizeColumns(sheet);
        }
    }

    /**
     * Normalize the width of the column.
     *
     * @param sheet
     */
    private static void autoSizeColumns(SXSSFSheet sheet) {
        var row = sheet.getRow(sheet.getLastRowNum());
        if (row == null) {
            return;
        }
        short lastColumn = row.getLastCellNum();
        for (var i = 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i, true);
        }
    }

}
