package org.openl.rules.xls.merge.compare;

import java.util.List;

/**
 * A sheet, as the two versions of the workbook hold it.
 *
 * <p>A sheet is found by its name. It is {@link ChangeStatus#EQUAL} when every table of it is, and
 * {@link ChangeStatus#CHANGED} otherwise.
 *
 * @param name   the name of the sheet
 * @param status what became of the sheet
 * @param tables the tables of the sheet, from top to bottom and from left to right as the second version places them;
 *               a table only the first version holds stands where the first version places it
 */
public record SheetComparison(String name, ChangeStatus status, List<TableComparison> tables) {

    public SheetComparison {
        tables = List.copyOf(tables);
    }
}
