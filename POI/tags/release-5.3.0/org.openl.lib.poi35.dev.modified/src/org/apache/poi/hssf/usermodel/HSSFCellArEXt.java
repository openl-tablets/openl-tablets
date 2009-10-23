/**
 * 
 */
package org.apache.poi.hssf.usermodel;

import java.util.Calendar;
import java.util.Date;

import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.aggregates.FormulaRecordAggregate;
import org.apache.poi.ss.formula.ArrayFormula;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellArExt;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;

/**
 * @author vabramovs
 *
 */
public class HSSFCellArEXt extends HSSFCell implements CellArExt {
	
	  /**
     * If cell belong to Array Formula Range - this is reference to cell, contains formula and Range
     *   otherwise - null
     **/
    
    private ArrayFormula arrayFormulaRef = null;
  

	public HSSFCellArEXt(HSSFWorkbook book, HSSFSheet sheet,
			CellValueRecordInterface cval) {
		super(book, sheet, cval);
	   	if(_record instanceof FormulaRecordAggregate){
    		FormulaRecordAggregate frec = (FormulaRecordAggregate)_record;
    		
    		CellRangeAddress range = frec.getFormulaRange();
    		if(range != null){
    			// We set array ref for first cell only
    			if(frec.getRow() == range.getFirstRow() && frec.getColumn() == range.getFirstColumn())
    				arrayFormulaRef = new ArrayFormula(this,range);
    			
    		}
    			
    	}
 	}

	public HSSFCellArEXt(HSSFWorkbook book, HSSFSheet sheet, int row,
			short col, int type) {
		super(book, sheet, row, col, type);
	}

	public HSSFCellArEXt(HSSFWorkbook book, HSSFSheet sheet, int row, short col) {
		super(book, sheet, row, col);
	}
	public ArrayFormula getArrayFormulaRef() {
		return arrayFormulaRef;
	}

	public void setArrayFormulaRef(ArrayFormula ref) {
		arrayFormulaRef = ref;
		
	}
	
    public void setCellFormula(String formula,String  range) {
    	if(formula != null){
        	super.setCellFormula(formula);
    		
    		if(range != null){
    			super.setCellType(Cell.CELL_TYPE_FORMULA);
    			CellRangeAddress rangeAdr =  (CellRangeAddress) CellRangeAddress.valueOf(range);
    			ArrayFormula arrayRef =  new ArrayFormula(this, rangeAdr);
    			//  We set Array Formula ref only for this cell
    			// Later(after sheet complete initialization) this ref will be expanded to all ceels in range
    			this.setArrayFormulaRef(arrayRef);
    			// expand ref to all range
    			arrayRef.expandArFormulaRef();
    			
           }
   		}
    	
    }
    /* (non-Javadoc)
     * @see org.apache.poi.ss.usermodel.CellArExt#isArrayFormulaContext()
     */
    public boolean isArrayFormulaContext(){
    	return !(arrayFormulaRef == null); 
    }

	  /*********** Internal  methods for set values (even in Array Formula range ) from POI  - *************/
	  /*********** Set public to allow to use this methods from tests *************/
	
	public void setCellCommentInt(Comment comment) {
    	super.setCellComment(comment);
	}
	
   public void setCellErrorValueInt(byte errorCode) {
    	super.setCellErrorValue(errorCode);
	}
     public void setCellValueInt(boolean value) {
    	super.setCellValue(value);
	}
    public void setCellValueInt(Calendar value) {
    	super.setCellValue(value);
	}
    public void setCellValueInt(Date value) {
    	super.setCellValue(value);
	}
    public void setCellValueInt(double value) {
    	super.setCellValue(value);
	}
    public void setCellValueInt(RichTextString value) {
    	super.setCellValue(value);
	}
    public void setCellValueInt(String value) {
    	super.setCellValue(value);
	}
  /*********** Overridden methods from XSSFCell *************/
	
	@Override
	public void setCellComment(Comment comment) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
    	super.setCellComment(comment);
	}
	
	@Override
	   public void setCellErrorValue(byte errorCode) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
    	super.setCellErrorValue(errorCode);
	}
	@Override
    public void setCellValue(boolean value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	@Override
    public void setCellValue(Calendar value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	@Override
    public void setCellValue(Date value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	@Override
    public void setCellValue(double value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	
	@Override
    public void setCellValue(RichTextString value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	@Override
    public void setCellValue(String value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}

}
