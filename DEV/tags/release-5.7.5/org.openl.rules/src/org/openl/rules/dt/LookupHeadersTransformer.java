package org.openl.rules.dt;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.Point;

/**
 * Transformer for lookup table headers values. The common case is that the RET
 * section is the last one in the header, as shown below:<br>
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
    
    // physical index in grid table, indicating the beginning of the first HC column after return section
    private int hcColumnStartAfterRet = 0;
    
    // physical index in grid table, indicating the first empty cell after all headers
    private int firstEmptyCell = 0;
    
    public LookupHeadersTransformer(IGridTable entireTable, IGridTable lookupValuesTable, int retTableWidth, int retColumnStartIndex, int firstEmptyCell) {
        super(entireTable, lookupValuesTable, retTableWidth);         
        this.retStartIndex = retColumnStartIndex;
        this.firstEmptyCell = firstEmptyCell;
        this.hcColumnStartAfterRet = retStartIndex + getRetTableWidth();
    }
    
    @Override
    protected Point getCoordinatesFromConditionHeaders(int column, int row) {
        if (column < retStartIndex) {
            return super.getCoordinatesFromConditionHeaders(column, row);
        } else if (retStartIndex <= column && column < retStartIndex + firstEmptyCell - hcColumnStartAfterRet) {
            return new Point(column + getRetTableWidth(), row);
        } else if (retStartIndex + firstEmptyCell - hcColumnStartAfterRet <= column && column < firstEmptyCell) {
            return new Point(column - (firstEmptyCell - hcColumnStartAfterRet), row);
        }
        return super.getCoordinatesFromConditionHeaders(column, row);
    }
}
