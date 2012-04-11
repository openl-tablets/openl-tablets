/**
 * 
 */
package org.apache.poi.ss.formula;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;


/**
 * @author vabramovs
 *
 */
public class DefaultExternalWorkbookResolver implements
		IExternalWorkbookResolver {
	static DefaultExternalWorkbookResolver instance = new DefaultExternalWorkbookResolver();
	private DefaultExternalWorkbookResolver(){};

	/* (non-Javadoc)
	 * @see org.apache.poi.ss.formula.IExternalWorkbookResolver#resolveExternalWorkbook(java.lang.String, java.lang.String)
	 */
	public InputStream resolveExternalExcel(
			String externalWorkbookReference) throws FileNotFoundException {
		InputStream result = null;
			try {
				result = new FileInputStream(externalWorkbookReference);
				if(result != null)
					return result;
			} catch (Exception e) {
			}
			// Extract file from possible absolute path and resolve it from resource
			// TODO - need solution when path to resource is more complex
			String file = externalWorkbookReference;
			for(int i=0;i<2;i++){
				int lastpos  = file.lastIndexOf(new String("/\\").charAt(i));
				if(lastpos != (-1))
					 file = file.substring(lastpos+1);
			}
		return DefaultExternalWorkbookResolver.class.getClassLoader().getResourceAsStream(file);
	}
	public IExternalWorkbookResolver getExternalWorkbookResolver(){
		return instance;
	}

	public Workbook resolveExternalWorkbook(String externalWorkbookReference)
			throws FileNotFoundException {
		// This method always return null to force create Workbook from InputStream by envoker
		return null;
	}
}
