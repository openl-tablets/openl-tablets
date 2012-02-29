package org.apache.poi.ss.formula;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;

public interface IExternalWorkbookResolver {
	
	 
	/**
	 * Resolve External Workbook reference as InputStream
	 * @param externalWorkbookReference
	 * @param callerPath
	 * @return
	 */
	InputStream resolveExternalExcel(String externalWorkbookReference)throws FileNotFoundException;  

	/**
	 * Resolve External Workbook reference as WorkBook
	 * @param externalWorkbookReference
	 * @param callerPath
	 * @return
	 */
	Workbook resolveExternalWorkbook(String externalWorkbookReference)throws FileNotFoundException;  
}
