package org.openl.rules.webstudio.web;

import java.lang.reflect.Array;
import java.util.function.Predicate;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.util.StringUtils;

class CellValueSelector implements Predicate<TableSyntaxNode> {

    private final String value;

    CellValueSelector(String value) {
        this.value = value;
    }

    @Override
    public boolean test(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        IGridTable table = node.getGridTable();
        for (int row = 0; row < table.getHeight(); row++) {
            for (int col = 0; col < table.getWidth(); col++) {
                ICell cell = table.getCell(col, row);
                Object cellValue = cell.getObjectValue();
                if (selectValue(cellValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean selectValue(Object cellValue) {
        if (cellValue == null) {
            return false;
        }

        if (cellValue.getClass().isArray()) {
            int len = Array.getLength(cellValue);
            for (int i = 0; i < len; i++) {
                Object cv = Array.get(cellValue, i);
                if (selectValue(cv)) {
                    return true;
                }
            }
            return false;
        }

        String strCellValue = String.valueOf(cellValue);

        return StringUtils.containsIgnoreCase(strCellValue, value);
    }

}
