package org.openl.rules.excel.builder.export;

import java.util.Collection;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.EnvironmentTableStyleImpl;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.table.xls.PoiExcelHelper;

public class EnvironmentTableExporter extends AbstractOpenlTableExporter<EnvironmentModel> {

    public static final String ENV_SHEET = "Environment";
    public static final String IMPORT = "import";
    public static final String DEPENDENCY = "dependency";

    @Override
    protected void exportTables(Collection<EnvironmentModel> models, Sheet sheet) {
        Cursor endPosition = null;
        TableStyle style = getTableStyle();
        for (EnvironmentModel model : models) {
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(EnvironmentModel model, Cursor startPosition, TableStyle tableStyle, Sheet sheet) {
        EnvironmentTableStyleImpl style = (EnvironmentTableStyleImpl) tableStyle;
        RichTextString headerText = style.getHeaderTemplate();
        CellRangeSettings headerSizeSettings = style.getHeaderSizeSettings();
        addMergedHeader(sheet, startPosition, style.getHeaderStyle(), headerSizeSettings);
        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        topLeftCell.setCellValue(headerText);
        startPosition = startPosition.moveDown(headerSizeSettings.getHeight());
        Cursor endPosition = startPosition;
        Iterator<String> dpdIterator = model.getDependencies().iterator();
        Iterator<String> importIterator = model.getImports().iterator();
        endPosition = writeData(sheet, style, endPosition, dpdIterator, DEPENDENCY);
        endPosition = endPosition.moveRight(1);
        endPosition = writeData(sheet, style, endPosition, importIterator, IMPORT);
        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    private Cursor writeData(Sheet sheet,
            EnvironmentTableStyleImpl style,
            Cursor endPosition,
            Iterator<String> importIterator,
            String anImport) {
        while (importIterator.hasNext()) {
            boolean lastRow = false;
            String dpd = importIterator.next();
            if (!importIterator.hasNext()) {
                lastRow = true;
            }
            Cursor next = endPosition.moveDown(1);
            Cell nameCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            nameCell.setCellValue(anImport);
            nameCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getNameStyle() : style.getRowStyle().getNameStyle());

            next = next.moveRight(1);

            Cell valueCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            valueCell.setCellValue(dpd);
            valueCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getValueStyle() : style.getRowStyle().getValueStyle());

            endPosition = next.moveLeft(2);
        }
        return endPosition;
    }

    @Override
    protected String getExcelSheetName() {
        return ENV_SHEET;
    }
}
