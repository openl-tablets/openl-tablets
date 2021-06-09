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

import org.apache.poi.xssf.streaming.SXSSFRow;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build the xlsx datatype spreadsheet from the given data type list
 */
public class ExcelFileBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelFileBuilder.class);

    private ExcelFileBuilder() {
    }

    /**
     * Generating xlsx file with project which contains data types and spreadsheets.
     * 
     * @param projectModel - model of the project.
     */
    public static void generateProject(ProjectModel projectModel) {
        String projectName = projectModel.getName();
        String fileName = projectName + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            writeProject(projectModel, fos);
        } catch (IOException e) {
            LOGGER.error("Error on saving the file occurred.", e);
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
     * @param outputStream - output stream with models.
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
     * @param outputStream - output stream with models.
     */
    public static void generateSpreadsheets(List<SpreadsheetModel> spreadsheetModels, OutputStream outputStream) {
        writeSpreadsheets(spreadsheetModels, outputStream);
    }

    /**
     * Generate spreadsheets with environment.
     *
     * @param spreadsheetModels - spreadsheet models.
     * @param outputStream - output stream with models.
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
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            SXSSFSheet dtSheet = workbook.createSheet(DATATYPES_SHEET);
            TableStyle datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);
            datatypeTableExporter.export(datatypeModels, dtSheet);
            dtSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOGGER.error("Error on generating DataTypes workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    private static void writeDataTables(List<DataModel> dataModels, OutputStream outputStream) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            SXSSFSheet dtSheet = workbook.createSheet(DATA_SHEET);
            TableStyle datatypeStyles = stylesMap.get(DATA_SHEET);
            DataTableExporter dataTableExporter = new DataTableExporter();
            dataTableExporter.setTableStyle(datatypeStyles);
            dataTableExporter.export(dataModels, dtSheet);
            dtSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOGGER.error("Error on generating Data tables workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    /**
     * Writing spreadsheets to Excel file with styles from template.
     * 
     * @param spreadsheetModels
     * @param outputStream
     */
    private static void writeSpreadsheets(List<SpreadsheetModel> spreadsheetModels, OutputStream outputStream) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            TableStyle sprStyles = stylesMap.get(SPR_RESULT_SHEET);
            writeSpreadsheets(spreadsheetModels, workbook, sprStyles);
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOGGER.error("Error on generating Spreadsheet workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    private static void writeSpreadsheets(List<SpreadsheetModel> spreadsheetModels,
            SXSSFWorkbook workbook,
            TableStyle tableStyle) {
        SXSSFSheet sprSheet = workbook.createSheet(SPR_RESULT_SHEET);
        SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter();
        Set<String> reservedWords = spreadsheetModels.stream()
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
        String defaultValueHeader = tableStyle.getValueHeaderText();
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
        SXSSFSheet dataTableSheet = workbook.createSheet(DATA_SHEET);
        DataTableExporter dtExporter = new DataTableExporter();
        dtExporter.setTableStyle(tableStyle);
        dtExporter.export(dataModels, dataTableSheet);
        dataTableSheet.validateMergedRegions();
    }

    private static void writeAlgorithmsModule(List<SpreadsheetModel> spreadsheetModels,
            List<DataModel> dataModels,
            OutputStream outputStream,
            EnvironmentModel environmentModel) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            TableStyle sprStyle = stylesMap.get(SPR_RESULT_SHEET);
            TableStyle envStyle = stylesMap.get(ENV_SHEET);
            TableStyle dataTableStyle = stylesMap.get(DATA_SHEET);
            writeSpreadsheets(spreadsheetModels, workbook, sprStyle);
            writeEnvironment(environmentModel, workbook, envStyle);
            writeDataTables(dataModels, workbook, dataTableStyle);
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOGGER.error("Error on generating Spreadsheet workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    private static void writeEnvironment(EnvironmentModel environmentModel, SXSSFWorkbook workbook, TableStyle style) {
        if (environmentModel == null) {
            return;
        }
        SXSSFSheet envSheet = workbook.createSheet(ENV_SHEET);
        EnvironmentTableExporter environmentTableExporter = new EnvironmentTableExporter();
        environmentTableExporter.setTableStyle(style);
        environmentTableExporter.export(Collections.singletonList(environmentModel), envSheet);
        envSheet.validateMergedRegions();
    }

    /**
     * Writing project to Excel file with styles from template.
     *
     * @param projectModel - model of the project with data types, environment, spreadsheets
     * @param fos - output stream.
     */
    private static void writeProject(ProjectModel projectModel, OutputStream fos) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = ExcelTemplateUtils.getTemplate()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);

            SXSSFSheet dtSheet = workbook.createSheet(DATATYPES_SHEET);
            SXSSFSheet sprSheet = workbook.createSheet(SPR_RESULT_SHEET);
            SXSSFSheet dataSheet = workbook.createSheet(DATA_SHEET);
            TableStyle datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            TableStyle sprStyles = stylesMap.get(SPR_RESULT_SHEET);
            TableStyle dataStyles = stylesMap.get(DATA_SHEET);

            DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);

            SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter();
            sprTableExporter.setTableStyle(sprStyles);

            DataTableExporter dataTableExporter = new DataTableExporter();
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
            LOGGER.error("Error on generating workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    /**
     * Make sheets readable.
     * 
     * @param workbook - target document.
     */
    private static void autoSizeSheets(SXSSFWorkbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
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
        SXSSFRow row = sheet.getRow(sheet.getLastRowNum());
        if (row == null) {
            return;
        }
        short lastColumn = row.getLastCellNum();
        for (int i = 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i, true);
        }
    }

}
