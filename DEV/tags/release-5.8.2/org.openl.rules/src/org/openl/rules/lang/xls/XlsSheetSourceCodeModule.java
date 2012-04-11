package org.openl.rules.lang.xls;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.source.IOpenSourceCodeModule;

public class XlsSheetSourceCodeModule implements IOpenSourceCodeModule, IIndexElement {
    private String sheetName;

    private XlsWorkbookSourceCodeModule workbookSource;

	private Sheet sheet;
	
	private Map<String, Object> params;

    public XlsSheetSourceCodeModule(Sheet sheet, String sheetName, XlsWorkbookSourceCodeModule workbookSource) {
        this.sheet = sheet;
        this.sheetName = sheetName;
        this.workbookSource = workbookSource;
    }

    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    public String getCategory() {
        return IDocumentType.WORKSHEET.getCategory();
    }

    public Reader getCharacterStream() {
        throw new UnsupportedOperationException();
    }

    public String getCode() {
        return null;
        // throw new UnsupportedOperationException();
    }

    public String getDisplayName() {
        return sheetName;
    }

    public String getIndexedText() {
        return sheetName;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getStartPosition() {
        return 0;
    }

    public int getTabSize() {
        throw new UnsupportedOperationException();
    }

    public String getType() {
        return IDocumentType.WORKSHEET.getType();
    }

    public String getUri() {
        return getUri(0);
    }

    public String getUri(int textpos) {
        return workbookSource.getUri(0) + "?" + XlsURLConstants.SHEET + "=" + sheetName;
    }

    public XlsWorkbookSourceCodeModule getWorkbookSource() {
        return workbookSource;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isModified() {
        return workbookSource.isModified();
    }
}
