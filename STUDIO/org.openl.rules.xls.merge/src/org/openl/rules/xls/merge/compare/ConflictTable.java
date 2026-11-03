package org.openl.rules.xls.merge.compare;

import java.util.List;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * A table of a sheet, as the base version and the two versions of a conflict hold it.
 *
 * <p>Each version is compared with the base, as a comparison of two versions compares them. The rows and the columns
 * of the three are lined up by the lines of the base: a line of the base stands against what each version made of
 * it, and a line a version inserted stands where that version inserted it. A table a version added stands alone.
 *
 * <p>A cell both versions changed is a conflict when they changed it differently. A cell both changed alike is only
 * changed.
 *
 * @param name           the header of the table, as your version holds it, or as the base or their version does when
 *                       your version does not hold the table
 * @param base           the place of the table in the base, or {@code null} for a table a version added
 * @param yours          the place of the table in your version, or {@code null} when it does not hold it
 * @param theirs         the place of the table in their version, or {@code null} when it does not hold it
 * @param yoursStatus    what your version did to the table
 * @param theirsStatus   what their version did to the table
 * @param rows           the rows, lined up
 * @param columns        the columns, lined up
 * @param yoursChanged   the cells of your version that differ from the base
 * @param theirsChanged  the cells of their version that differ from the base
 * @param yoursConflicts the cells of your version both versions changed differently
 * @param theirsConflicts the cells of their version both versions changed differently
 */
public record ConflictTable(String name,
                            @Nullable CellRangeAddress base,
                            @Nullable CellRangeAddress yours,
                            @Nullable CellRangeAddress theirs,
                            ChangeStatus yoursStatus,
                            ChangeStatus theirsStatus,
                            List<ConflictLine> rows,
                            List<ConflictLine> columns,
                            List<CellAddress> yoursChanged,
                            List<CellAddress> theirsChanged,
                            List<CellAddress> yoursConflicts,
                            List<CellAddress> theirsConflicts) {

    public ConflictTable {
        rows = List.copyOf(rows);
        columns = List.copyOf(columns);
        yoursChanged = List.copyOf(yoursChanged);
        theirsChanged = List.copyOf(theirsChanged);
        yoursConflicts = List.copyOf(yoursConflicts);
        theirsConflicts = List.copyOf(theirsConflicts);
    }
}
