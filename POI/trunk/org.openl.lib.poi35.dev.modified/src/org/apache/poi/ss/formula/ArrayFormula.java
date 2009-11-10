
package org.apache.poi.ss.formula;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellArExt;
import org.apache.poi.ss.usermodel.Row;

/**
 * Class to support Array Formula 
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
	
	/**
	 * get first row index in range
	 * @return
	 */
	public int getFirstRow(){
		return rangeAddress.getFirstRow();
	}
	/**
	 * get first column index in range
	 * @return
	 */
	public int getFirstColumn(){
		return rangeAddress.getFirstColumn();
	}
	/**
	 * get last column index in range
	 * @return
	 */
	public int getLastColumn(){
		return rangeAddress.getLastColumn();
	}
	/**
	 * get last row index in range
	 * @return
	 */
	public int getLastRow(){
		return rangeAddress.getLastRow();
	}
	
	/**
	 * get number of cells in range
	 * @return
	 */
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
		double valueNum = 0.0;
		boolean valueBol = true;
		String valueStr = "";
		byte valueErr =  0;
		 		 		
		 		
		for(int rowInd = getFirstRow();rowInd <=getLastRow();rowInd++)
 			for(int colInd = getFirstColumn();colInd <=getLastColumn();colInd++){
 				Row rowInRange = ((Cell)formulaCell).getSheet().getRow(rowInd);
 				CellArExt cellInRange = (CellArExt)rowInRange.getCell(colInd);
 				cellInRange.setArrayFormulaRef(this);
 				// Get precalculated value
 				int type = ((Cell)cellInRange).getCellType();
 				if(type ==  Cell.CELL_TYPE_FORMULA) // Get type of formula result
 					type = ((Cell)cellInRange).getCachedFormulaResultType();
 				switch (type){
 					case Cell.CELL_TYPE_BLANK:
 					case Cell.CELL_TYPE_FORMULA:
 					case Cell.CELL_TYPE_NUMERIC:
 						 valueNum = ((Cell)cellInRange).getNumericCellValue();
 					break;
 					case Cell.CELL_TYPE_BOOLEAN: 				
 						 valueBol = ((Cell)cellInRange).getBooleanCellValue();
 					break;	
 					case Cell.CELL_TYPE_STRING: 				
 						valueStr = ((Cell)cellInRange).getStringCellValue();
 					break;	
 					case Cell.CELL_TYPE_ERROR:
 						 valueErr = ((Cell)cellInRange).getErrorCellValue();
 					break;	
 				}
// 				System.out.println("Going to Set formula"+((Cell)formulaCell).getCellFormula()+" into row:"+rowInd+",col:"+colInd);
 				((Cell)cellInRange).setCellFormula(((Cell)formulaCell).getCellFormula());
 				((Cell)cellInRange).setCellType(Cell.CELL_TYPE_FORMULA);
 				
 				// Restore precalculated value
 				
				switch (type){
					case Cell.CELL_TYPE_BLANK:
					case Cell.CELL_TYPE_FORMULA:
					case Cell.CELL_TYPE_NUMERIC:
						((Cell)cellInRange).setCellValue(valueNum);
					break;
					case Cell.CELL_TYPE_BOOLEAN: 				
						((Cell)cellInRange).setCellValue(valueBol);
					break;	
					case Cell.CELL_TYPE_STRING: 				
						((Cell)cellInRange).setCellValue(valueStr);
					break;	
					case Cell.CELL_TYPE_ERROR:
						((Cell)cellInRange).setCellErrorValue(valueErr);
					break;	
					default:
		 				((Cell)cellInRange).setCellValue(0.0);

					break;	
				}
			}
	}
		        				
}


