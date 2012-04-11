package org.openl.rules.indexer;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

public class WorkbookIndexParser implements IIndexParser
{

	public String getCategory()
	{
		return IDocumentType.WORKBOOK.getCategory();
	}

	public String getType()
	{
		return IDocumentType.WORKBOOK.getType();
	}

	public IIndexElement[] parse(IIndexElement root)
	{
    XlsWorkbookSourceCodeModule wbSrc = (XlsWorkbookSourceCodeModule)root;
    return parseWorkbook(wbSrc);	
	}	
	
	public XlsSheetSourceCodeModule[] parseWorkbook(XlsWorkbookSourceCodeModule wbSrc)
	{

    HSSFWorkbook wb = wbSrc.getWorkbook();
    int nsheets = wb.getNumberOfSheets();

    XlsSheetSourceCodeModule[] sheets = new XlsSheetSourceCodeModule[nsheets];
    
    for (int i = 0; i < nsheets; i++)
    {
      HSSFSheet sheet = wb.getSheetAt(i);
      String sheetName = wb.getSheetName(i);

      sheets[i] = new XlsSheetSourceCodeModule(sheet,
          sheetName, wbSrc);
    }
    
    return sheets;
	}

	
	
}
