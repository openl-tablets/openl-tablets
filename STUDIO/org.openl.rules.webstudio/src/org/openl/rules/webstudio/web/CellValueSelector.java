package org.openl.rules.webstudio.web;

import java.lang.reflect.Array;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringUtils;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class CellValueSelector implements Predicate<TableSyntaxNode> {

    private final String value;

    @Override
    public boolean test(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        var table = node.getGridTable();
        for (var row = 0; row < table.getHeight(); row++) {
            for (var col = 0; col < table.getWidth(); col++) {
                var cell = table.getCell(col, row);
                var cellValue = cell.getObjectValue();
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
            var len = Array.getLength(cellValue);
            for (var i = 0; i < len; i++) {
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
