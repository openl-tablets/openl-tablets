package org.openl.rules.excel.builder.export;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.SpreadsheetTableStyleImpl;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.table.xls.PoiExcelHelper;

public class SpreadsheetResultTableExporter extends AbstractOpenlTableExporter<SpreadsheetModel> {

    public static final String SPR_RESULT_SHEET = "SpreadsheetResults";

    public static final String SPREADSHEET_RESULT_NAME_TEMPLATE = "\\{spr.name}";
    public static final String SPREADSHEET_RESULT_RETURN_TYPE = "\\{spr.return.type}";
    public static final String SPREADSHEET_RESULT_SIGNATURE = "\\{spr.signature}";
    public static final String SPREADSHEET_RESULT_STEP_NAME = "\\{spr.field.name}";
    public static final String SPREADSHEET_RESULT_STEP_VALUE = "\\{spr.field.value}";

    public SpreadsheetResultTableExporter() {
        // empty constructor for tests
    }

    @Override
    protected void exportTables(Collection<SpreadsheetModel> models, Sheet sheet) {
        Cursor endPosition = null;
        TableStyle style = getTableStyle();
        for (SpreadsheetModel model : models) {
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(SpreadsheetModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        SpreadsheetTableStyleImpl style = (SpreadsheetTableStyleImpl) defaultStyle;
        CellStyle headerStyle = style.getHeaderStyle();
        RichTextString tableHeaderText = style.getHeaderTemplate();
        CellRangeSettings headerSettings = style.getHeaderSizeSettings();

        CellStyle stepHeaderStyle = style.getHeaderRowStyle().getNameStyle();
        String stepHeaderText = style.getStepHeaderText();

        CellStyle valueHeaderStyle = style.getHeaderRowStyle().getValueStyle();
        String valueHeaderText = style.getValueHeaderText();

        String sprHeaderText = tableHeaderText.getString().replaceAll(SPREADSHEET_RESULT_RETURN_TYPE, model.getType());
        sprHeaderText = sprHeaderText.replaceAll(SPREADSHEET_RESULT_NAME_TEMPLATE, model.getName());
        String parameters = model.getParameters()
            .stream()
            .map(x -> x.getType().getSimpleName() + " " + x.getFormattedName())
            .collect(Collectors.joining(", "));
        sprHeaderText = sprHeaderText.replaceAll(SPREADSHEET_RESULT_SIGNATURE, parameters);

        addMergedHeader(sheet, startPosition, headerStyle, headerSettings);

        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        topLeftCell.setCellValue(sprHeaderText);

        startPosition = startPosition.moveDown(headerSettings.getHeight() + 1);

        Cell stepHeaderCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        stepHeaderCell.setCellValue(stepHeaderText);
        stepHeaderCell.setCellStyle(stepHeaderStyle);

        startPosition = startPosition.moveRight(1);

        Cell valueHeaderCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        valueHeaderCell.setCellValue(valueHeaderText);
        valueHeaderCell.setCellStyle(valueHeaderStyle);

        startPosition = startPosition.moveLeft(1);

        Cursor endPosition = startPosition;

        Iterator<StepModel> iterator = model.getSteps().iterator();
        while (iterator.hasNext()) {
            StepModel step = iterator.next();
            boolean lastRow = false;
            if (!iterator.hasNext()) {
                lastRow = true;
            }
            Cursor next = endPosition.moveDown(1);

            Cell stepNameCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            stepNameCell.setCellValue(step.getName());
            stepNameCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getNameStyle() : style.getRowStyle().getNameStyle());

            next = next.moveRight(1);

            Cell stepValueCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            stepValueCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getValueStyle() : style.getRowStyle().getValueStyle());

            stepValueCell.setCellValue(step.getValue());

            endPosition = next.moveLeft(1);

        }

        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    @Override
    protected String getExcelSheetName() {
        return SPR_RESULT_SHEET;
    }
}
