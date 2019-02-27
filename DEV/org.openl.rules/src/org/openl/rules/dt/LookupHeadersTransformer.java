package org.openl.rules.dt;

import org.openl.rules.table.IGridTable;

/**
 * Transformer for lookup table headers values. The common case is that the RET section is the last one in the header,
 * as shown below:<br>
 * 
 * <table cellspacing="2">
 * <tr>
 * <td align="center" bgcolor="#8FCB52"><b>C1</b></td>
 * <td align="center" bgcolor="#8FCB52"><b>C2</b></td>
 * <td align="center" bgcolor="#8FCB52"><b>C3</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC1</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC2</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC3</b></td>
 * <td align="center" bgcolor="bc8f8f"><b>RET1</b></td>
 * </tr>
 * </table>
 * <br>
 * For users convenience it is possible to define RET section in any place of lookup header after vertical conditions.
 * Example:
 * <table cellspacing="2">
 * <tr>
 * <td align="center" bgcolor="#8FCB52"><b>C1</b></td>
 * <td align="center" bgcolor="#8FCB52"><b>C2</b></td>
 * <td align="center" bgcolor="#8FCB52"><b>C3</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC1</b></td>
 * <td align="center" bgcolor="bc8f8f"><b>RET1</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC2</b></td>
 * <td align="center" bgcolor="#ccffff"><b>HC3</b></td>
 * </tr>
 * </table>
 * And we need to transform it to common case for further work.
 * 
 * @author DLiauchuk
 * 
 */
public class LookupHeadersTransformer extends TwoDimensionDecisionTableTranformer {

    // physical index in grid table, indicating the beginning of return section
    private int retStartIndex = 0;

    // physical index in grid table, indicating the beginning of the first HC
    // column after return section
    private int hcColumnStartAfterRet = 0;

    // physical index in grid table, indicating the first empty cell after all
    // headers
    private int firstEmptyCell = 0;

    LookupHeadersTransformer(IGridTable entireTable,
            IGridTable lookupValuesTable,
            int retTableWidth,
            int retColumnStartIndex,
            int firstEmptyCell) {
        super(entireTable, lookupValuesTable, retTableWidth);
        this.retStartIndex = retColumnStartIndex;
        this.firstEmptyCell = firstEmptyCell;
        this.hcColumnStartAfterRet = retStartIndex + getRetTableWidth();
    }

    @Override
    public int getColumn(int col, int row) {
        if (col < retStartIndex) {
            return super.getColumn(col, row);
        } else if (col < retStartIndex + firstEmptyCell - hcColumnStartAfterRet) {
            return col + getRetTableWidth();
        } else if (retStartIndex + firstEmptyCell - hcColumnStartAfterRet <= col && col < firstEmptyCell) {
            return col - (firstEmptyCell - hcColumnStartAfterRet);
        }
        return super.getColumn(col, row);
    }

    @Override
    public int getRow(int col, int row) {
        if (col < retStartIndex) {
            return super.getRow(col, row);
        } else if (col < retStartIndex + firstEmptyCell - hcColumnStartAfterRet) {
            return row;
        } else if (retStartIndex + firstEmptyCell - hcColumnStartAfterRet <= col && col < firstEmptyCell) {
            return row;
        }
        return super.getRow(col, row);
    }
}
