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
 * Class to provide array formula support for hssf cell.
 *   For cell belong to Array Formula range arrayFormulaRef is set, null otherwise.
 *    May be merged to HSSFCell 
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
	/* (non-Javadoc)
	 * @see org.apache.poi.ss.usermodel.CellArExt#getArrayFormulaRef()
	 */
	public ArrayFormula getArrayFormulaRef() {
		return arrayFormulaRef;
	}

	/* (non-Javadoc)
	 * @see org.apache.poi.ss.usermodel.CellArExt#setArrayFormulaRef(org.apache.poi.ss.formula.ArrayFormula)
	 */
	public void setArrayFormulaRef(ArrayFormula ref) {
		arrayFormulaRef = ref;
		
	}
	
    /**
     * Set Array Formula for cell's range
     * @param formula
     * @param range like "A1:B2"
     */
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
	
	/**
	 * Set comment for cell
	 * @param comment
	 */
	public void setCellCommentInt(Comment comment) {
    	super.setCellComment(comment);
	}
	
   /**
    * Set Error Value for cell
    * @param errorCode
    */
	public void setCellErrorValueInt(byte errorCode) {
    	super.setCellErrorValue(errorCode);
	}
     /**
      * Set boolean value for cell
     * @param value
     */
    public void setCellValueInt(boolean value) {
    	super.setCellValue(value);
	}
    /**
      * Set Calendar value for cell
     * @param value
     */
    public void setCellValueInt(Calendar value) {
    	super.setCellValue(value);
	}
    /**
      * Set Calendar value for cell
     * @param value
     */
    public void setCellValueInt(Date value) {
    	super.setCellValue(value);
	}
    /**
      * Set numeric value for cell
     * @param value
     */
    public void setCellValueInt(double value) {
    	super.setCellValue(value);
	}
    /**
     * Set RichText value for cell
     * @param value
     */
    public void setCellValueInt(RichTextString value) {
    	super.setCellValue(value);
	}
    /**
     * Set String value for cell
     * @param value
     */
    public void setCellValueInt(String value) {
    	super.setCellValue(value);
	}
  /*********** Overridden methods from HSSFCell *************/
	
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellComment(org.apache.poi.ss.usermodel.Comment)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellCommentInt()in case of need
	 */
	@Override
	public void setCellComment(Comment comment) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
    	super.setCellComment(comment);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellErrorValue(byte)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellErrorValueInt()in case of need
	 */
	@Override
	 public void setCellErrorValue(byte errorCode) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
    	super.setCellErrorValue(errorCode);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(boolean)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	@Override
    public void setCellValue(boolean value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(java.util.Calendar)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	@Override
    public void setCellValue(Calendar value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(java.util.Date)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	@Override
    public void setCellValue(Date value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(double)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(double)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	@Override
    public void setCellValue(double value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(org.apache.poi.ss.usermodel.RichTextString)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
	@Override
    public void setCellValue(RichTextString value) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellValue(value);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.usermodel.HSSFCell#setCellValue(java.lang.String)
	 * Throws RuntimeException("You cannot change part of an array") if cell belong Array Formula range.
	 * Use setCellValueInt()in case of need
	 */
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
