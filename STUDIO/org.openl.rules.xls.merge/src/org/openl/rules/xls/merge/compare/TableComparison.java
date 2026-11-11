package org.openl.rules.xls.merge.compare;

import java.util.List;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * A table of a sheet, as the two versions of the workbook hold it.
 *
 * <p>The rows and the columns of a table both versions hold are lined up the way a text comparison lines up lines,
 * in the order they are read: a removed line before the line added in its place. A table one version holds alone has
 * no lines lined up.
 *
 * <p>A cell of a line both versions hold differs when its value, its formula, its comment or its merging differs. A
 * change of its formatting alone leaves it equal, and is told for the table as a whole.
 *
 * @param name              the text of the first cell of the table, the header of an OpenL table, in the second
 *                          version, or in the first one when the second does not hold the table
 * @param first             the place of the table in the first version, or {@code null} when it does not hold it
 * @param second            the place of the table in the second version, or {@code null} when it does not hold it
 * @param status            what became of the table
 * @param moved             whether the second version moved the table on purpose, whatever else changed
 * @param formattingChanged whether the formatting of a cell both versions hold differs
 * @param rows              the rows of both versions, lined up, or none for a table only one version holds
 * @param columns           the columns, lined up the same way
 * @param firstChanged      the cells of the first version that differ in the second one
 * @param secondChanged     the cells of the second version that differ in the first one
 */
public record TableComparison(String name,
                              @Nullable CellRangeAddress first,
                              @Nullable CellRangeAddress second,
                              ChangeStatus status,
                              boolean moved,
                              boolean formattingChanged,
                              List<LineChange> rows,
                              List<LineChange> columns,
                              List<CellAddress> firstChanged,
                              List<CellAddress> secondChanged) {

    public TableComparison {
        rows = List.copyOf(rows);
        columns = List.copyOf(columns);
        firstChanged = List.copyOf(firstChanged);
        secondChanged = List.copyOf(secondChanged);
    }

    /**
     * Whether the formatting is all that differs: the table holds no cell that reads differently and no line only one
     * version holds. Such a table may still have been moved.
     */
    public boolean formattingOnly() {
        return formattingChanged && LineChange.allEqual(rows) && LineChange.allEqual(columns);
    }

    /**
     * A table only one version holds.
     *
     * @param status {@link ChangeStatus#ADDED} for a table of the second version, {@link ChangeStatus#REMOVED} for a
     *               table of the first one
     */
    public static TableComparison oneSided(String name, CellRangeAddress place, ChangeStatus status) {
        var added = status == ChangeStatus.ADDED;
        return new TableComparison(name,
                added ? null : place,
                added ? place : null,
                status,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
