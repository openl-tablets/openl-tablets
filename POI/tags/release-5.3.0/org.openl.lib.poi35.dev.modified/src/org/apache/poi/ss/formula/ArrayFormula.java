/**
 * 
 */
package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellArExt;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

/**
 * @author vabramovs
 *
 */
public class ArrayFormula {
	CellArExt formulaCell;         // Cell with formula
	CellRangeAddress rangeAddress ;    // Range for formula span
	
	
	public ArrayFormula(CellArExt formulaCell,CellRangeAddress cellRangeAddress){
		this.formulaCell = formulaCell;
		this.rangeAddress = cellRangeAddress;
		
	}
	
	public int getFirstRow(){
		return rangeAddress.getFirstRow();
	}
	public int getFirstColumn(){
		return rangeAddress.getFirstColumn();
	}
	public int getLastColumn(){
		return rangeAddress.getLastColumn();
	}
	public int getLastRow(){
		return rangeAddress.getLastRow();
	}
	
	public int getNumberOfCells(){
		return rangeAddress.getNumberOfCells();
	}
	/**
	 * @return the formulaCell
	 */
	public CellArExt getFormulaCell() {
		return formulaCell;
	}
	
	/**
	 * Belong cell to Array Range?
	 * @param cell
	 * @return
	 */
	public boolean isInRange(Cell cell){
		if( cell.getSheet()!= ((Cell)formulaCell).getSheet())
			return false;
        int rowInd = cell.getRowIndex();
        int colInd = cell.getColumnIndex();
        return ( getFirstRow() <= rowInd  &&  rowInd <= getLastRow() &&
        		getFirstColumn() <= colInd && colInd <= getLastColumn()); 
	}
	/**
	 *  Set the same formula for all cells in range
	 */
	public void expandArFormulaRef(){
		
		for(int rowInd = getFirstRow();rowInd <=getLastRow();rowInd++)
 			for(int colInd = getFirstColumn();colInd <=getLastColumn();colInd++){
 				Row rowInRange = ((Cell)formulaCell).getSheet().getRow(rowInd);
 				CellArExt cellInRange = (CellArExt)rowInRange.getCell(colInd);
 				cellInRange.setArrayFormulaRef(this);
// 				System.out.println("Going to Set formula"+((Cell)formulaCell).getCellFormula()+" into row:"+rowInd+",col:"+colInd);
 				((Cell)cellInRange).setCellFormula(((Cell)formulaCell).getCellFormula());
 				((Cell)cellInRange).setCellType(Cell.CELL_TYPE_FORMULA);
 				
 			}
	}
		        				
}


