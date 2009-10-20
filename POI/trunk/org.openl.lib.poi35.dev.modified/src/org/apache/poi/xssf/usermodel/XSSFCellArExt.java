/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.usermodel;

import java.util.Calendar;
import java.util.Date;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.formula.ArrayFormula;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.POILogFactory;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;

/**
 *  XSSFCell Class Extension to work with array formula.
 */
public final class XSSFCellArExt extends XSSFCell  implements CellArExt {
    private static POILogger logger = POILogFactory.getLogger(XSSFCellArExt.class);
    
    /**
     * If cell belong to Array Formula Range - this is reference to cell, contains formula and Range
     *   otherwise - null
     **/
    
    private ArrayFormula arrayFormulaRef = null;
    
   
    /**
     * Construct a XSSFCell.
     *
     * @param row the parent row.
     * @param cell the xml bean containing information about the cell.
     */
    protected XSSFCellArExt(XSSFRow row, CTCell cell) {
    	super(row, cell);
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
    			CTCell c  = this.getCTCell();
    			CTCellFormula f = c.getF();
    			if(f == null){
    				 f = c.addNewF();
    				 c.setF(f);
    			}
                f.setRef(range);
                f.setT(STCellFormulaType.ARRAY);
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
	  /*********** Set public to allow use this methods from tests *************/
	
	public void setCellCommentInt(Comment comment) {
    	super.setCellComment(comment);
	}
	
   public void setCellErrorValueInt(byte errorCode) {
    	super.setCellErrorValue(errorCode);
	}
    public void setCellErrorValueInt(FormulaError error) {
    	super.setCellErrorValue(error);
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
    public void setCellErrorValue(FormulaError error) {
		if(arrayFormulaRef != null){
			RuntimeException ex = new RuntimeException("You cannot change part of an array");
			if(!ex.getStackTrace()[1].getClassName().startsWith("org.apache.poi"))
				throw ex;
		}
 	super.setCellErrorValue(error);
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
