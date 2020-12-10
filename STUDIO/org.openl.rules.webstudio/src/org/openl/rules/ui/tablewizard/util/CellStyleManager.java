package org.openl.rules.ui.tablewizard.util;

import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.richfaces.json.JSONObject;

public class CellStyleManager {
    private final CellStyleCreator styleFactory;
    private final JSONHolder table;

    public CellStyleManager(XlsSheetGridModel gridModel, JSONHolder table) {
        this.styleFactory = new CellStyleCreator(gridModel);
        this.table = table;
    }

    public ICellStyle getHeaderStyle() {
        return styleFactory.getCellStyle(table.getHeaderStyle());
    }

    public ICellStyle getCellStyle(JSONObject style) {
        return styleFactory.getCellStyle(style);
    }

    public ICellStyle getPropertyStyles() {
        return styleFactory.getCellStyle(table.getPropertyStyle());
    }
}
