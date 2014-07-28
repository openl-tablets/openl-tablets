package org.openl.rules.table.search.selectors;

import java.lang.reflect.Array;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.util.AStringBoolOperator;
import org.openl.util.AStringBoolOperator.ContainsIgnoreCaseOperator;

public class CellValueSelector extends TableDataSelector {

    private AStringBoolOperator matchOperator;
    private String value;

    public CellValueSelector() {
    }

    public CellValueSelector(String value) {
        this(value, new ContainsIgnoreCaseOperator(null));
    }

    public CellValueSelector(String value, AStringBoolOperator matchOperator) {
        this.value = value;
        this.matchOperator = matchOperator;
    }

    public AStringBoolOperator getMatchOperator() {
        return matchOperator;
    }

    public void setMatchOperator(AStringBoolOperator matchOperator) {
        this.matchOperator = matchOperator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean select(TableSyntaxNode node) {
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

    public boolean selectValue(Object cellValue) {
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

        return matchOperator.isMatching(value, strCellValue);
    }

}
