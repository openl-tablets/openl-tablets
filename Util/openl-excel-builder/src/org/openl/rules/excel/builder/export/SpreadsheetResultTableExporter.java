package org.openl.rules.excel.builder.export;

import java.util.Collection;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.SpreadsheetResultTableStyle;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ui.ICellStyle;

public class SpreadsheetResultTableExporter extends AbstractOpenlTableExporter<SpreadsheetResultModel> {

    public static final String SPR_RESULT_SHEET = "SpreadsheetResults";

    public static final String SPREADSHEET_RESULT_NAME_TEMPLATE = "\\{spr.name}";
    public static final String SPREADSHEET_RESULT_RETURN_TYPE = "\\{spr.return.type}";
    public static final String SPREADSHEET_RESULT_SIGNATURE = "\\{spr.signature}";
    public static final String SPREADSHEET_RESULT_STEP_NAME = "\\{spr.field.name}";
    public static final String SPREADSHEET_RESULT_STEP_VALUE = "\\{spr.field.value}";

    @Override
    protected void exportTables(Collection<SpreadsheetResultModel> models, IWritableExtendedGrid gridToExport) {
        Cursor endPosition = null;
        for (SpreadsheetResultModel model : models) {
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, gridToExport, startPosition);
        }
    }

    @Override
    protected Cursor exportTable(SpreadsheetResultModel model, IWritableExtendedGrid gridToWrite, Cursor startPosition) {
        SpreadsheetResultTableStyle style = (SpreadsheetResultTableStyle) getTableStyle();

        ICellStyle headerStyle = style.getHeaderStyle();
        String tableHeaderText = style.getHeaderTemplate();
        CellRangeSettings headerSettings = style.getHeaderSettings();

        ICellStyle stepHeaderStyle = style.getStepHeaderStyle();
        String stepHeaderText = style.getStepHeaderText();

        ICellStyle valueHeaderStyle = style.getValueHeaderStyle();
        String valueHeaderText = style.getValueHeaderText();

        ICellStyle valueStyle = style.getStepValueStyle();
        ICellStyle nameStyle = style.getStepNameStyle();

        String sprHeaderText = tableHeaderText.replaceAll(SPREADSHEET_RESULT_RETURN_TYPE, model.getType());
        sprHeaderText = sprHeaderText.replaceAll(SPREADSHEET_RESULT_NAME_TEMPLATE, model.getName());
        sprHeaderText = sprHeaderText.replaceAll(SPREADSHEET_RESULT_SIGNATURE, model.getSignature());

        GridRegion mergedRegion = new GridRegion(startPosition.getRow(),
            startPosition.getColumn(),
            startPosition.getRow() + headerSettings.getHeight(),
            startPosition.getColumn() + headerSettings.getWidth());
        gridToWrite.addMergedRegionUnsafe(mergedRegion);

        for (int i = mergedRegion.getTop(); i <= mergedRegion.getBottom(); i++) {
            for (int j = mergedRegion.getLeft(); j <= mergedRegion.getRight(); j++) {
                gridToWrite.setCellStyle(j, i, headerStyle);
            }
        }

        gridToWrite.setCellValue(startPosition.getColumn(), startPosition.getRow(), sprHeaderText);
        startPosition = startPosition.moveDown(headerSettings.getHeight() + 1);

        gridToWrite.setCellValue(startPosition.getColumn(), startPosition.getRow(), stepHeaderText);
        gridToWrite.setCellStyle(startPosition.getColumn(), startPosition.getRow(), stepHeaderStyle);
        startPosition = startPosition.moveRight(1);

        gridToWrite.setCellValue(startPosition.getColumn(), startPosition.getRow(), valueHeaderText);
        gridToWrite.setCellStyle(startPosition.getColumn(), startPosition.getRow(), valueHeaderStyle);

        startPosition = startPosition.moveLeft(1);

        Cursor endPosition = startPosition;

        for (FieldModel step : model.getSteps()) {
            Cursor next = endPosition.moveDown(1);

            gridToWrite.setCellValue(next.getColumn(), next.getRow(), step.getName());
            gridToWrite.setCellStyle(next.getColumn(), next.getRow(), nameStyle);

            next = next.moveRight(1);

            if (step.getDefaultValue() == null) {
                gridToWrite.setCellValue(next.getColumn(), next.getRow(), convertValue(step));
            } else {
                gridToWrite.setCellFormula(next.getColumn(), next.getRow(), "testSpr");
            }
            gridToWrite.setCellStyle(next.getColumn(), next.getRow(), valueStyle);

            endPosition = next.moveLeft(1);

        }

        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    @Override
    protected String getExcelSheetName() {
        return SPR_RESULT_SHEET;
    }

    private static Object convertValue(FieldModel model) {
        Object result;
        switch (model.getType()) {
            case "Integer":
                result = 0;
                break;
            case "Double":
                result = 0.0d;
                break;
            case "Float":
                result = 0.0f;
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

}
