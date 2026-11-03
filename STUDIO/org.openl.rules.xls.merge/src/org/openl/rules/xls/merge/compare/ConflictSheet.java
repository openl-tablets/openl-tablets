package org.openl.rules.xls.merge.compare;

import java.util.List;

/**
 * A sheet, as the base version and the two versions of a conflict hold it.
 *
 * @param name         the name of the sheet
 * @param yoursStatus  what your version did to the sheet
 * @param theirsStatus what their version did to the sheet
 * @param tables       the tables of the sheet: those of the base in the order your version places them, then the ones
 *                     only their version holds
 */
public record ConflictSheet(String name, ChangeStatus yoursStatus, ChangeStatus theirsStatus, List<ConflictTable> tables) {

    public ConflictSheet {
        tables = List.copyOf(tables);
    }
}
