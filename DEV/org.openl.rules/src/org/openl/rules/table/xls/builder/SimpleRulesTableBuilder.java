package org.openl.rules.table.xls.builder;

import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * Helper class that allows creating new SimpleRules tables in specified excel
 * sheet.
 *
 * @author Pavel Tarasevich
 */

public class SimpleRulesTableBuilder extends TableBuilder {

    public SimpleRulesTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }
    
    /**
     * Number of rows that usually are used for table logic element. That is
     * conditions title
     */
    public static final int LOGIC_ELEMENT_HEIGHT = 1;

    private int elementColumn;

    @Override
    public void beginTable(int width, int height) throws CreateTableException {
        super.beginTable(width, height);
        elementColumn = 0;
    }

    @Override
    public void endTable() throws CreateTableException {
        if (elementColumn > 0) {
            incCurrentRow(LOGIC_ELEMENT_HEIGHT);
        }
        super.endTable();
    }

    public void writeTableBodyRow(List<Map<String, Object>> row) {
        int i = 0;
        for (Map<String, Object> cell : row) {
            writeCell(i, getCurrentRow(), 1, 1, cell.get("value"), (ICellStyle) cell.get("style") );
            i++;
        }

        incCurrentRow(1);
    }

    /**
     * Writes decision table header. <br />
     * Requires the header signature, e.g.
     * <code><i>void hello1(int hour)</i></code><br/> without Decision table
     * header token <code>Rules</code>
     *
     * @param signature method signature for the table.
     */
    @Override
    public void writeHeader(String signature, ICellStyle style) {
        String headerText = IXlsTableNames.SIMPLE_DECISION_TABLE + " " + signature;
        super.writeHeader(headerText, style);
    }
}
