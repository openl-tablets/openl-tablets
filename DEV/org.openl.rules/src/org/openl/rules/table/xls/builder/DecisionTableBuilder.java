package org.openl.rules.table.xls.builder;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * Helper class that allows creating new decision tables in specified excel sheet.
 *
 * @author Aliaksandr Antonik
 */
public class DecisionTableBuilder extends TableBuilder {

    /**
     * Number of rows that usually are used for table logic element. That is actions & conditions title, logic,
     * parameter declarations, paramater business names.
     */
    public static final int LOGIC_ELEMENT_HEIGHT = 4;

    private int elementColumn;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public DecisionTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

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

    /**
     * Writes an element, which is an action, a condition or return block. As an example look at a part of
     * <i>driverPremium</i> table in OpenL Tutorial 4:
     * <table cellspacing="2">
     * <tr bgcolor="#ccffff">
     * <td align="center" colspan="2"><b>C2</b></td>
     * </tr>
     * <tr bgcolor="#ccffff">
     * <td colspan="2">located == "in" &amp;&amp; statelist.indexOf( di.driver.state) &gt;= 0</td>
     * </tr>
     * <tr bgcolor="#ccffff">
     * <td align="center">String located</td>
     * <td align="center">String statelist</td>
     * </tr>
     * <tr bgcolor="#ffff99">
     * <td align="center"><b>Located</b></td>
     * <td align="center"><b>State</b></td>
     * </tr>
     * </table>
     *
     * Here element's <code>title</code> is <i>C2</i>, <code>logic</code> is <i>located == "in" &amp;&amp;
     * statelist.indexOf( di.driver.state) &gt;= 0</i>, <code>parameterNames</code> are <i>Located</i> and <i>State</i>
     * and finally <code>parameterSignatures</code> are <i>String located</i> and <i>String statelist</i>.
     *
     * <br/>
     * <br/>
     * The lengths of <code>parameterNames</code> and <code>parameterSignatures</code> must be equal and positive.
     *
     * @param title element title
     * @param logic element logic
     * @param parameterNames array of element parameter names
     * @param parameterSignatures array of element parameter signatures
     */
    public void writeElement(String title, String logic, String[] parameterNames, String[] parameterSignatures) {
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        if (parameterNames == null || parameterNames.length == 0) {
            throw new IllegalArgumentException("parameterNames must be not null array of positive length");
        }
        if (parameterSignatures == null || parameterSignatures.length == 0) {
            throw new IllegalArgumentException("parameterSignatures must be not null array of positive length");
        }
        if (parameterSignatures.length != parameterNames.length) {
            throw new IllegalArgumentException("numbers of parameter names and parameter signatures must be equal");
        }

        int elementWidth = parameterNames.length;
        if (elementColumn + elementWidth > getHeight()) {
            throw new IllegalStateException("total elements width is too big, expected height = " + getHeight());
        }

        writeCell(elementColumn, getCurrentRow(), elementWidth, 1, title);
        writeCell(elementColumn, getCurrentRow() + 1, elementWidth, 1, logic);
        for (int i = 0; i < elementWidth; ++i) {
            writeCell(elementColumn + i, getCurrentRow() + 2, 1, 1, parameterSignatures[i]);
            writeCell(elementColumn + i, getCurrentRow() + 3, 1, 1, parameterNames[i]);
        }

        elementColumn += elementWidth;
    }

    /**
     * Writes decision table header. <br />
     * Requires the header signature, e.g. <code><i>void hello1(int hour)</i></code><br/>
     * without Decision table header token <code>Rules</code>
     *
     * @param signature method signature for the table.
     */
    public void writeHeader(String signature) {
        String headerText = IXlsTableNames.DECISION_TABLE2 + " " + signature;
        super.writeHeader(headerText, null);
    }

}
