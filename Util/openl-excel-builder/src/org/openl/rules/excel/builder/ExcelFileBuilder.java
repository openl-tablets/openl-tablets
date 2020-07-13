package org.openl.rules.excel.builder;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.streaming.CustomizedSXSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openl.rules.excel.builder.export.DatatypeTableExporter;
import org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter;
import org.openl.rules.excel.builder.export.XlsExtendedSheetGridModel;
import org.openl.rules.excel.builder.template.ExcelTemplateUtils;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build the xlsx datatype spreadsheet from the given data type list
 */
public class ExcelFileBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelFileBuilder.class);

    public static final String VOCABULARY_SHEET = "Vocabulary";

    private ExcelFileBuilder() {
    }

    /**
     * 
     * @param projectModel - model of the project
     */
    public static void generateExcelFile(ProjectModel projectModel) {
        // create virtual grids
        List<SpreadsheetResultModel> sprs = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> dts = projectModel.getDatatypeModels();
        String projectName = projectModel.getName();
        String fileName = projectName + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            // extract this to separate method
            SXSSFWorkbook tempWorkbook = null;
            try (SXSSFWorkbook workbook = tempWorkbook = new CustomizedSXSSFWorkbook()) {

                // map with styles
                Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);

                SXSSFSheet datatypes = workbook.createSheet(DATATYPES_SHEET);
                SXSSFSheet sprResults = workbook.createSheet(SPR_RESULT_SHEET);
                // SXSSFSheet vocabulary = workbook.createSheet(VOCABULARY_SHEET);
                // create sheet spr, datatype, vocabulary

                TableStyle datatypeStyle = stylesMap.get(DATATYPES_SHEET);
                TableStyle sprStyle = stylesMap.get(SPR_RESULT_SHEET);
                DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
                datatypeTableExporter.setTableStyle(datatypeStyle);

                SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter();
                sprTableExporter.setTableStyle(sprStyle);

                XlsExtendedSheetGridModel dtGrid = ExcelBuilder.createGrid(datatypes, fileName);
                datatypeTableExporter.export(dtGrid, dts);

                XlsExtendedSheetGridModel sprGrid = ExcelBuilder.createGrid(sprResults, fileName);
                sprTableExporter.export(sprGrid, sprs);

                // XlsSheetGridModel voc = ExcelBuilder.createGrid(vocabulary, fileName);
                // datatypeTableExporter.export()

                autoSizeSheets(workbook);

                workbook.write(fos);
            } catch (IOException e) {
                logger.error("", e);
            } finally {
                if (tempWorkbook != null) {
                    tempWorkbook.dispose();
                }
            }

        } catch (IOException e) {
            logger.error("", e);
        }

    }

    private static void autoSizeSheets(SXSSFWorkbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.trackAllColumnsForAutoSizing();
            autoSizeColumns(sheet);
        }
    }

    protected static void autoSizeColumns(SXSSFSheet sheet) {
        SXSSFRow row = sheet.getRow(sheet.getLastRowNum());
        if (row == null) {
            return;
        }
        short lastColumn = row.getLastCellNum();

        // Skip column with Test name and ID column
        for (int i = 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i, true);
        }
    }

}
