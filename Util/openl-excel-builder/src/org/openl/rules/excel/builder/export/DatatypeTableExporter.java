package org.openl.rules.excel.builder.export;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.DataTypeTableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.StringUtils;

public class DatatypeTableExporter extends AbstractOpenlTableExporter<DatatypeModel> {

    public static final String DATATYPES_SHEET = "Datatypes";

    public static final String DATATYPE_NAME = "\\{datatype.name}";

    @Override
    protected void exportTables(Collection<DatatypeModel> models, IWritableExtendedGrid gridToExport) {
        Cursor endPosition = null;
        for (DatatypeModel model : models) {
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, gridToExport, startPosition);
        }
    }

    @Override
    protected Cursor exportTable(DatatypeModel model, IWritableExtendedGrid gridToWrite, Cursor startPosition) {
        DataTypeTableStyle style = (DataTypeTableStyle) getTableStyle();

        String headerTemplate = style.getHeaderTemplate();
        CellRangeSettings rangeSettings = style.getHeaderSettings();
        ICellStyle headerStyle = style.getHeaderStyle();

        ICellStyle fieldTypeStyle = style.getFieldTypeStyle();
        ICellStyle fieldNameStyle = style.getFieldNameStyle();
        ICellStyle fieldDefaultValueStyle = style.getFieldDefaultValueStyle();

        String dtHeaderText = headerTemplate.replaceAll(DATATYPE_NAME, model.getName());
        if (StringUtils.isNotBlank(model.getParent()))
            dtHeaderText += " extends " + model.getParent();

        GridRegion mergedRegion = new GridRegion(startPosition.getRow(),
            startPosition.getColumn(),
            startPosition.getRow() + rangeSettings.getHeight(),
            startPosition.getColumn() + rangeSettings.getWidth());
        gridToWrite.addMergedRegionUnsafe(mergedRegion);
        for (int i = mergedRegion.getTop(); i <= mergedRegion.getBottom(); i++) {
            for (int j = mergedRegion.getLeft(); j <= mergedRegion.getRight(); j++) {
                gridToWrite.setCellStyle(j, i, headerStyle);
            }
        }

        gridToWrite.setCellValue(startPosition.getColumn(), startPosition.getRow(), dtHeaderText);
        startPosition = startPosition.moveDown(rangeSettings.getHeight());

        Cursor endPosition = startPosition;

        for (FieldModel field : model.getFields()) {
            Cursor next = endPosition.moveDown(1);
            gridToWrite.setCellValue(next.getColumn(), next.getRow(), field.getType());
            gridToWrite.setCellStyle(next.getColumn(), next.getRow(), fieldTypeStyle);
            next = next.moveRight(1);

            gridToWrite.setCellValue(next.getColumn(), next.getRow(), field.getName());

            gridToWrite.setCellStyle(next.getColumn(), next.getRow(), fieldNameStyle);

            next = next.moveRight(1);

            if (field.getDefaultValue() == null) {
                gridToWrite.setCellValue(next.getColumn(), next.getRow(), "");
            } else {
                try {
                    gridToWrite.setCellValue(next.getColumn(), next.getRow(), convertDefaultValue(field));
                } catch (ParseException e) {

                }
            }
            gridToWrite.setCellStyle(next.getColumn(), next.getRow(), fieldDefaultValueStyle);

            endPosition = next.moveLeft(2);
        }

        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    @Override
    protected String getExcelSheetName() {
        return DATATYPES_SHEET;
    }

    private static Object convertDefaultValue(FieldModel model) throws ParseException {
        String valueAsString = model.getDefaultValue().toString();
        Object result;
        switch (model.getType()) {
            case "Integer":
                Number casted = NumberFormat.getInstance().parse(valueAsString);
                if (casted.longValue() <= Integer.MAX_VALUE) {
                    result = Integer.parseInt(valueAsString);
                } else {
                    result = Long.parseLong(valueAsString);
                }
                break;
            case "Double":
                result = Double.parseDouble(valueAsString);
                break;
            case "Float":
                result = Float.parseFloat(valueAsString);
                break;
            case "String":
                result = valueAsString;
                break;
            case "Boolean":
                result = Boolean.parseBoolean(valueAsString);
                break;
            case "Date":
                result = new Date();
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

}
