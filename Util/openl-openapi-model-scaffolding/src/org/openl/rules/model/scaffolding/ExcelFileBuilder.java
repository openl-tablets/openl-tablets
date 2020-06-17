package org.openl.rules.model.scaffolding;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    public static final String SPREADSHEET_RESULT_NAME_TEMPLATE = "\\{spr.name.template}";
    public static final String SPREADSHEET_RESULT_STEP_NAME = "\\{spr.step.name}";
    public static final String SPREADSHEET_RESULT_STEP_VALUE = "\\{spr.step.value}";

    public static final String DATATYPES_SHEET = "Datatypes";
    public static final String SPR_RESULT_SHEET = "SpreadsheetResults";
    private static final byte DATATYPE_MARGIN = 3;

    private ExcelFileBuilder() {
    }

    /**
     * 
     * @param projectModel - model of the project
     */
    public static void generateExcelFile(ProjectModel projectModel) {
        ClassLoader classLoader = ExcelFileBuilder.class.getClassLoader();
        try (OPCPackage fs = OPCPackage
            .open(Objects.requireNonNull(classLoader.getResourceAsStream("datatype_template.xlsx")))) {

            XSSFWorkbook wb = new XSSFWorkbook(fs);

            Sheet dataTypeSheet = wb.getSheet(DATATYPES_SHEET);
            if (dataTypeSheet == null) {
                logger.info("Datatype sheet wasn't found");
                System.exit(1);
            }
            Sheet sprResultSheet = wb.getSheet(SPR_RESULT_SHEET);
            if (sprResultSheet == null) {
                logger.error("SpreadSheetResults sheet wasn't found");
                System.exit(1);
            }

            writeDatatypes(projectModel, dataTypeSheet);
            writeSpreadSheetResults(projectModel, sprResultSheet);

            try (FileOutputStream fos = new FileOutputStream(projectModel.getName() + ".xlsx")) {
                wb.write(fos);
                wb.close();
            }
        } catch (Exception e) {
            logger.error("Exception was occurred during building excel file", e);
        }
    }

    private static void writeSpreadSheetResults(ProjectModel projectModel, Sheet sheet) {
        Row sprDefinitionRow = sheet.getRow(0);
        Cell sprCell = sprDefinitionRow.getCell(0);
        CellStyle sprDefinitionStyle = sprCell.getCellStyle();

        // region of spreadsheet result definition
        CellRangeAddress sprRegion = sheet.getMergedRegion(0);
        CellRangeSettings sprRegionSettings = new CellRangeSettings(sprRegion);

        Row propertiesDescriptionRow = sheet.getRow(1);
        Cell propertiesCell = propertiesDescriptionRow.getCell(0);
        Cell descriptionCell = propertiesDescriptionRow.getCell(1);
        CellStyle propertiesStyle = propertiesCell.getCellStyle();
        CellStyle descriptionStyle = descriptionCell.getCellStyle();

        CellRangeAddress stepHeader = sheet.getMergedRegion(1);
        CellRangeSettings stepHeaderSettings = new CellRangeSettings(stepHeader);
        Row stepValueRow = sheet.getRow(2);
        Cell stepHeaderTemplateCell = stepValueRow.getCell(0);
        Cell stepValueTemplateHeaderCell = stepValueRow.getCell(stepHeaderSettings.getWidth() + 1);
        CellStyle stepHeaderStyle = stepHeaderTemplateCell.getCellStyle();
        CellStyle valueHeaderStyle = stepValueTemplateHeaderCell.getCellStyle();

        Row stepNameValueRow = sheet.getRow(3);
        CellRangeAddress stepNameTemplateRegion = sheet.getMergedRegion(2);
        CellRangeSettings stepNameSettings = new CellRangeSettings(stepNameTemplateRegion);
        Cell stepNameTemplateCell = stepNameValueRow.getCell(0);
        Cell stepValueTemplateCell = stepNameValueRow.getCell(stepNameSettings.getWidth() + 1);
        CellStyle stepNameCellStyle = stepNameTemplateCell.getCellStyle();
        CellStyle stepValueCellStyle = stepValueTemplateCell.getCellStyle();

        String spreadsheetNameTemplate = sprCell.getStringCellValue();
        String stepNameTemplate = stepNameTemplateCell.getStringCellValue();
        String stepValueTemplate = stepValueTemplateCell.getStringCellValue();

        int rowCounter = 0;
        for (SpreadsheetResultModel spreadsheetResultModel : projectModel.getSpreadsheetResultModels()) {

            String spreadsheetResultNameValue = spreadsheetNameTemplate.replaceAll(SPREADSHEET_RESULT_NAME_TEMPLATE,
                spreadsheetResultModel.getSignature());

            CellRangeAddress region = new CellRangeAddress(rowCounter,
                rowCounter + sprRegionSettings.getHeight(),
                0,
                sprRegionSettings.getWidth());
            if (region.getFirstRow() != sprRegion.getFirstRow() && region.getLastRow() != sprRegion.getLastRow()) {
                addRegion(sheet, sprDefinitionStyle, region);
            }
            Cell sprNameCell = getOrCreateCell(0, rowCounter, sheet);
            sprNameCell.setCellValue(spreadsheetResultNameValue);
            // properties and description
            rowCounter += 2;
            CellRangeAddress stepHeaderRegion = new CellRangeAddress(rowCounter,
                rowCounter + stepHeaderSettings.getHeight(),
                0,
                stepHeaderSettings.getWidth());
            if (stepHeaderRegion.getFirstRow() != stepHeader.getFirstRow())
                addRegion(sheet, stepHeaderStyle, stepHeaderRegion);
            Cell stepHeaderCell = getOrCreateCell(0, rowCounter, sheet);
            stepHeaderCell.setCellValue(stepHeaderTemplateCell.getStringCellValue());

            int dist = stepHeaderSettings.getWidth() + 1;
            Cell valueHeaderCell = getOrCreateCell(dist, rowCounter, sheet);
            valueHeaderCell.setCellValue(stepValueTemplateHeaderCell.getStringCellValue());
            valueHeaderCell.setCellStyle(valueHeaderStyle);
            rowCounter++;

            for (FieldModel field : spreadsheetResultModel.getModel().getFields()) {
                CellRangeAddress stepNameRegion = new CellRangeAddress(rowCounter,
                    rowCounter + stepNameSettings.getHeight(),
                    0,
                    stepNameSettings.getWidth());
                if (stepNameRegion.getFirstRow() != stepNameTemplateRegion.getFirstRow())
                    addRegion(sheet, stepNameCellStyle, stepNameRegion);

                String stepNameValue = stepNameTemplate.replaceAll(SPREADSHEET_RESULT_STEP_NAME, field.getName());
                Cell stepNameCell = getOrCreateCell(0, rowCounter, sheet);
                stepNameCell.setCellValue(stepNameValue);

                Object defaultValue = field.getDefaultValue();
                String valueStep = stepValueTemplate.replaceAll(SPREADSHEET_RESULT_STEP_VALUE,
                    defaultValue != null ? String.valueOf(defaultValue) : "");
                Cell valueStepCell = getOrCreateCell(stepNameSettings.getWidth() + 1, rowCounter, sheet);
                valueStepCell.setCellValue(valueStep);
                valueStepCell.setCellStyle(stepValueCellStyle);
                rowCounter++;
            }
            rowCounter++;
        }

    }

    private static void writeDatatypes(ProjectModel projectModel, Sheet sheet) throws ParseException {
        // findTemplate
        Row datatypeDefinitionRow = sheet.getRow(0);
        Cell datatypeDefinitionCell = datatypeDefinitionRow.getCell(0);
        CellStyle datatypeDefinitionStyle = datatypeDefinitionCell.getCellStyle();
        Row fieldDefinitionRow = sheet.getRow(1);

        // region of datatype definition
        CellRangeAddress templateDatatypeRegion = sheet.getMergedRegion(0);
        CellRangeSettings templateDatatypeSettings = new CellRangeSettings(templateDatatypeRegion);

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
        for (DatatypeModel dataType : projectModel.getDatatypeModels()) {
            String datatypeValue = datatypeTemplate.replaceAll(DATATYPE_NAME, dataType.getName());

            CellRangeAddress region = new CellRangeAddress(rowCounter,
                rowCounter + templateDatatypeSettings.getHeight(),
                0,
                templateDatatypeSettings.getWidth());
            if (region.getFirstRow() != templateDatatypeRegion.getFirstRow() && region
                .getLastRow() != templateDatatypeRegion.getLastRow()) {
                addRegion(sheet, datatypeDefinitionStyle, region);
            }
            Cell datatypeCell = getOrCreateCell(0, rowCounter, sheet);
            datatypeCell.setCellValue(datatypeValue);
            rowCounter += templateDatatypeSettings.getHeight() + 1;
            for (FieldModel field : dataType.getFields()) {

                Cell typeCell = getOrCreateCell(FIELD_CLASS_CELL_INDEX, rowCounter, sheet);
                String fieldClass = classNameTemplate.replaceAll(DATATYPE_FIELD_CLASS, field.getType());
                typeCell.setCellStyle(fieldClassStyle);
                typeCell.setCellValue(fieldClass);

                Cell nameCell = getOrCreateCell(FIELD_NAME_CELL_INDEX, rowCounter, sheet);
                String fieldName = nameTemplate.replaceAll(DATATYPE_FIELD_NAME, field.getName());
                nameCell.setCellValue(fieldName);
                nameCell.setCellStyle(fieldNameStyle);

                Cell defaultValueCell = getOrCreateCell(FIELD_DEFAULT_VALUE_CELL_INDEX, rowCounter, sheet);

                if (field.getDefaultValue() != null)
                    setDefaultValue(defaultValueCell, field);
                else
                    defaultValueCell.setCellValue("");

                defaultValueCell.setCellStyle(defaultValueStyle);
                rowCounter++;
            }
            rowCounter += DATATYPE_MARGIN;
        }
    }

    private static void setDefaultValue(Cell defaultValueCell, FieldModel field) throws ParseException {
        String format = "";
        if (field.getFormat() != null) {
            format = field.getFormat();
        }
        String valueAsString = field.getDefaultValue().toString();
        switch (field.getType()) {
            case "Integer":
                Number casted = NumberFormat.getInstance().parse(valueAsString);
                if (casted.longValue() <= Integer.MAX_VALUE) {
                    defaultValueCell.setCellValue(Integer.parseInt(valueAsString));
                } else {
                    defaultValueCell.setCellValue(Long.parseLong(valueAsString));
                }
                break;
            case "Number":
                BigDecimal bigDecimalValue = new BigDecimal(valueAsString);
                if (format.equals("double")) {
                    defaultValueCell.setCellValue(bigDecimalValue.doubleValue());
                } else if (format.equals("float")) {
                    defaultValueCell.setCellValue(bigDecimalValue.floatValue());
                } else {
                    defaultValueCell.setCellValue(0.0);
                }
                break;
            case "String":
                defaultValueCell.setCellValue(valueAsString);
                break;
            case "Boolean":
                defaultValueCell.setCellValue(Boolean.parseBoolean(valueAsString));
                break;
            default:
                break;
        }
    }

    private static void addRegion(Sheet sheet, CellStyle style, CellRangeAddress region) {
        sheet.addMergedRegion(region);
        drawRegion(sheet, style, region);
    }

    private static void drawRegion(Sheet sheet, CellStyle style, CellRangeAddress region) {
        for (int i = region.getFirstRow(); i <= region.getLastRow(); i++) {
            for (int j = region.getFirstColumn(); j <= region.getLastColumn(); j++) {
                Cell cell = getOrCreateCell(j, i, sheet);
                cell.setCellStyle(style);
            }
        }
    }

    public static Cell getOrCreateCell(int colIndex, int rowIndex, Sheet sheet) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }
}
