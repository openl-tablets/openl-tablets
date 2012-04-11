package org.openl.rules.lang.xls;

import java.io.InputStream;
import java.io.Reader;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.IOpenSourceCodeModule;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.table.syntax.XlsURLConstants;


public class XlsSheetSourceCodeModule implements IOpenSourceCodeModule, IIndexElement
{
  String sheetName;

  XlsWorkbookSourceCodeModule workbookSource;
  
  Sheet sheet;

  public XlsSheetSourceCodeModule(Sheet sheet, String sheetName,
  		XlsWorkbookSourceCodeModule workbookSource)
  {
  	this.sheet = sheet;
    this.sheetName = sheetName;
    this.workbookSource = workbookSource;
  }

  public InputStream getByteStream()
  {
    throw new UnsupportedOperationException();
  }

  public Reader getCharacterStream()
  {
    throw new UnsupportedOperationException();
  }

  public String getCode()
  {
  	return null;
//    throw new UnsupportedOperationException();
  }

  public int getTabSize()
  {
    throw new UnsupportedOperationException();
  }

  public String getUri(int textpos)
  {
    return workbookSource.getUri(0) + "?" + XlsURLConstants.SHEET + "=" + sheetName;
  }

  public int getStartPosition()
  {
    return 0;
  }

	public String getCategory()
	{
		return IDocumentType.WORKSHEET.getCategory();
	}

	public String getDisplayName()
	{
		return sheetName;
	}

	public String getIndexedText()
	{
		return sheetName;
	}

	
	
//	public IIndexElement getParent()
//	{
//		return workbookSource;
//	}

	public String getType()
	{
		return IDocumentType.WORKSHEET.getType();
	}

	public String getUri()
	{
		return getUri(0);
	}

	public Sheet getSheet()
	{
		return sheet;
	}

	public XlsWorkbookSourceCodeModule getWorkbookSource()
	{
		return workbookSource;
	}

	public String getSheetName()
	{
		return sheetName;
	}

}
