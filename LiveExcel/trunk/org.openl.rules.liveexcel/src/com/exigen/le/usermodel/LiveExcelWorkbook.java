/**
 * 
 */
package com.exigen.le.usermodel;

import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.evaluator.function.UDFFinderLE;

/**
 * @author vabramovs
 *
 */
public interface LiveExcelWorkbook extends Workbook {
	public UDFFinderLE getUDFFinder();

}
