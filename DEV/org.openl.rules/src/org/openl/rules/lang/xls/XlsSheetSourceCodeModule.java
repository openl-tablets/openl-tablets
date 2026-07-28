package org.openl.rules.lang.xls;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Sheet;

import org.openl.rules.lang.xls.load.SheetLoader;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.StringTool;

@Deprecated
public class XlsSheetSourceCodeModule implements IOpenSourceCodeModule {
    @Getter
    private final XlsWorkbookSourceCodeModule workbookSource;

    @Getter
    private final SheetLoader sheetLoader;

    @Getter
    @Setter
    private Map<String, Object> params;

    public XlsSheetSourceCodeModule(int sheetIndex, XlsWorkbookSourceCodeModule workbookSource) {
        this(workbookSource.getWorkbookLoader().getSheetLoader(sheetIndex), workbookSource);
    }

    public XlsSheetSourceCodeModule(SheetLoader sheetLoader, XlsWorkbookSourceCodeModule workbookSource) {
        this.sheetLoader = sheetLoader;
        this.workbookSource = workbookSource;
    }

    @Override
    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream() {
        throw new UnsupportedOperationException();
    }

    @Override
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

    @Override
    public int getStartPosition() {
        return 0;
    }

    @Override
    public String getFileUri() {
        return workbookSource.getFileUri();
    }

    @Override
    public String getUri() {
        var workbookUri = workbookSource.getUri();
        if (workbookUri == null) {
            // assume that URI is null for virtual grid module, let's try to make it unique
            workbookUri = "VIRTUAL_WORKBOOK@" + System.identityHashCode(this);
        }
        return workbookUri + "?sheet=" + StringTool.encodeURL(getSheetName());
    }
}
