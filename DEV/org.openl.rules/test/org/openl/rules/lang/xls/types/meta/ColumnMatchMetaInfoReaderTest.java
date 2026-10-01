package org.openl.rules.lang.xls.types.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;

class ColumnMatchMetaInfoReaderTest extends BaseOpenlBuilderHelper {

    private static final int FIRST_MATCH_ROW = 4;
    private static final int FIRST_VALUE_COLUMN = 2;

    ColumnMatchMetaInfoReaderTest() {
        super("test/rules/cmatch1/match2-1.xls");
    }

    @Test
    void aCheckedValueIsTypedByTheValueTheRowChecks() {
        var meta = metaInfo(FIRST_MATCH_ROW, FIRST_VALUE_COLUMN);

        assertEquals(Integer.class.getName(), meta.getDataType().getName());
    }

    @Test
    void anEmptyValueOfTheRowHasNoType() {
        assertNull(metaInfo(FIRST_MATCH_ROW, FIRST_VALUE_COLUMN + 3));
    }

    private CellMetaInfo metaInfo(int row, int column) {
        TableSyntaxNode table = findTable("ColumnMatch <MATCH> int runColumnMatch(int i1, int i2)");
        var cell = table.getGridTable().getCell(column, row);
        return table.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
    }
}
