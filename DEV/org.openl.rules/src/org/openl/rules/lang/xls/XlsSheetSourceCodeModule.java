package org.openl.rules.lang.xls;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.lang.xls.load.SheetLoader;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.StringTool;

public class XlsSheetSourceCodeModule implements IOpenSourceCodeModule {
    private XlsWorkbookSourceCodeModule workbookSource;

    private SheetLoader sheetLoader;

    private Map<String, Object> params;

    public XlsSheetSourceCodeModule(int sheetIndex, XlsWorkbookSourceCodeModule workbookSource) {
        this(workbookSource.getWorkbookLoader().getSheetLoader(sheetIndex), workbookSource);
    }

    public XlsSheetSourceCodeModule(SheetLoader sheetLoader, XlsWorkbookSourceCodeModule workbookSource) {
        this.sheetLoader = sheetLoader;
        this.workbookSource = workbookSource;
    }

    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    public Reader getCharacterStream() {
        throw new UnsupportedOperationException();
    }

    public String getCode() {
        return null;
    }

    public String getDisplayName() {
        return getSheetName();
    }

    public Sheet getSheet() {
        return sheetLoader.getSheet();
    }

    public String getSheetName() {
        return sheetLoader.getSheetName();
    }

    public SheetLoader getSheetLoader() {
        return sheetLoader;
    }

    public int getStartPosition() {
        return 0;
    }

    public String getUri() {
        return workbookSource.getUri() + "?" + XlsURLConstants.SHEET + "=" + StringTool.encodeURL(getSheetName());
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
