package org.openl.rules.webstudio.util;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.webstudio.util.converter.impl.DatatypeDto;
import org.openl.rules.webstudio.util.converter.impl.FieldDto;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build the xlsx datatype spreadsheet from the given data type list
 */
public class ExcelFileBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelFileBuilder.class);

    public static final int FIELD_CLASS_CELL_INDEX = 0;
    public static final int FIELD_NAME_CELL_INDEX = 1;
    public static final int FIELD_DEFAULT_VALUE_CELL_INDEX = 2;

    public static final String DATATYPE_NAME = "\\{datatype.name}";
    public static final String DATATYPE_FIELD_CLASS = "\\{datatype.field.class}";
    public static final String DATATYPE_FIELD_NAME = "\\{datatype.field.name}";
    public static final String DATATYPE_FIELD_DEFAULT_VALUE = "\\{datatype.field.defaultValue}";
    public static final String DATATYPES_SHEET = "Datatypes";
    private static final byte DATATYPE_MARGIN = 3;

    private ExcelFileBuilder() {
    }

    /**
     * 
     * @param projectName - name of the project
     * @param dataTypes - list of data types, extracted from the json/yaml
     */
    public static void generateExcelFile(String projectName, List<DatatypeDto> dataTypes) {
        ClassLoader classLoader = ExcelFileBuilder.class.getClassLoader();
        try (OPCPackage fs = OPCPackage
            .open(Objects.requireNonNull(classLoader.getResourceAsStream("datatype_template.xlsx")))) {

            XSSFWorkbook wb = new XSSFWorkbook(fs);

            Sheet sheet = wb.getSheet(DATATYPES_SHEET);
            // findTemplate
            Row datatypeDefinitionRow = sheet.getRow(0);
            Cell datatypeDefinitionCell = datatypeDefinitionRow.getCell(0);
            CellStyle datatypeDefinitionStyle = datatypeDefinitionCell.getCellStyle();
            Row fieldDefinitionRow = sheet.getRow(1);

            // region of datatype definition
            CellRangeAddress existingDatatypeRegion = sheet.getMergedRegion(0);
            int firstColumn = existingDatatypeRegion.getFirstColumn();
            int firstRow = existingDatatypeRegion.getFirstRow();
            int lastColumn = existingDatatypeRegion.getLastColumn();
            int lastRow = existingDatatypeRegion.getLastRow();
            int rowDistance = lastRow - firstRow;
            int columnDistance = lastColumn - firstColumn;

            Cell fieldClassTemplateCell = fieldDefinitionRow.getCell(FIELD_CLASS_CELL_INDEX);
            CellStyle fieldClassStyle = fieldClassTemplateCell.getCellStyle();

            Cell fieldNameTemplateCell = fieldDefinitionRow.getCell(FIELD_NAME_CELL_INDEX);
            CellStyle fieldNameStyle = fieldNameTemplateCell.getCellStyle();

            Cell defaultValueTemplateCell = fieldDefinitionRow.getCell(FIELD_DEFAULT_VALUE_CELL_INDEX);
            CellStyle defaultValueStyle = defaultValueTemplateCell.getCellStyle();

            String datatypeTemplate = datatypeDefinitionCell.getStringCellValue();
            String classNameTemplate = fieldClassTemplateCell.getStringCellValue();
            String nameTemplate = fieldNameTemplateCell.getStringCellValue();
            String defaultValueTemplate = defaultValueTemplateCell.getStringCellValue();

            int rowCounter = 0;
            for (DatatypeDto dataType : dataTypes) {
                String datatypeValue = datatypeTemplate.replaceAll(DATATYPE_NAME, dataType.getName());

                CellRangeAddress region = new CellRangeAddress(rowCounter, rowCounter + rowDistance, 0, columnDistance);
                if (region.getFirstRow() != existingDatatypeRegion.getFirstRow() && region
                    .getLastRow() != existingDatatypeRegion.getLastRow()) {
                    addDefinitionRegion(sheet, datatypeDefinitionStyle, region);
                }
                Cell datatypeCell = PoiExcelHelper.getOrCreateCell(0, rowCounter, sheet);
                datatypeCell.setCellValue(datatypeValue);
                rowCounter += rowDistance + 1;
                for (FieldDto field : dataType.getFields()) {

                    Cell typeCell = PoiExcelHelper.getOrCreateCell(FIELD_CLASS_CELL_INDEX, rowCounter, sheet);
                    String fieldClass = classNameTemplate.replaceAll(DATATYPE_FIELD_CLASS, field.getType());
                    typeCell.setCellStyle(fieldClassStyle);
                    typeCell.setCellValue(fieldClass);

                    Cell nameCell = PoiExcelHelper.getOrCreateCell(FIELD_NAME_CELL_INDEX, rowCounter, sheet);
                    String fieldName = nameTemplate.replaceAll(DATATYPE_FIELD_NAME, field.getName());
                    nameCell.setCellValue(fieldName);
                    nameCell.setCellStyle(fieldNameStyle);

                    Cell defaultValueCell = PoiExcelHelper
                        .getOrCreateCell(FIELD_DEFAULT_VALUE_CELL_INDEX, rowCounter, sheet);
                    String fieldDefaultValue = defaultValueTemplate.replaceAll(DATATYPE_FIELD_DEFAULT_VALUE,
                        field.getDefaultValue());
                    // TODO: check the types and formats
                    defaultValueCell.setCellValue(fieldDefaultValue);
                    defaultValueCell.setCellStyle(defaultValueStyle);
                    rowCounter++;
                }
                rowCounter += DATATYPE_MARGIN;
            }

            try (FileOutputStream fos = new FileOutputStream(projectName + ".xlsx")) {
                wb.write(fos);
                wb.close();
            }
        } catch (Exception e) {
            logger.error("Exception was occurred during building excel file", e);
        }
    }

    private static void addDefinitionRegion(Sheet sheet, CellStyle datatypeDefinitionStyle, CellRangeAddress region) {
        sheet.addMergedRegion(region);
        drawRegion(sheet, datatypeDefinitionStyle, region);
    }

    private static void drawRegion(Sheet sheet, CellStyle datatypeDefinitionStyle, CellRangeAddress region) {
        for (int i = region.getFirstRow(); i <= region.getLastRow(); i++) {
            for (int j = region.getFirstColumn(); j <= region.getLastColumn(); j++) {
                Cell cell = PoiExcelHelper.getOrCreateCell(j, i, sheet);
                cell.setCellStyle(datatypeDefinitionStyle);
            }
        }
    }
}
