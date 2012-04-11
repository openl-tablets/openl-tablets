/* 
 * @author vabramovs
 */

package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.formula.ArrayFormula;

/**
 *   Cell Interface Extension to work with array formula
 *	 @author vabramovs(VIA)

 */
public interface CellArExt {

     

    /**
     * get reference for Array Formula
     * @return
     */
    ArrayFormula getArrayFormulaRef();
    
    /**
     * set reference for Array Formula
     * @param ref
     */
    void setArrayFormulaRef(ArrayFormula ref);
    
    /**
     * Check is cell belong to Array Formula Range
     * @return
     */
    public boolean isArrayFormulaContext();
    	 
}

