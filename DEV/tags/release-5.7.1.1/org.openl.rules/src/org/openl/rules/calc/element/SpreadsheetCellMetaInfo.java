package org.openl.rules.calc.element;

import org.openl.meta.IMetaInfo;
import org.openl.source.IOpenSourceCodeModule;

public class SpreadsheetCellMetaInfo implements IMetaInfo {

    private String name;
    private IOpenSourceCodeModule source;

    public SpreadsheetCellMetaInfo(String name, IOpenSourceCodeModule source) {
        this.name = name;
        this.source = source;
    }

    public String getDisplayName(int mode) {
        return name;
    }

    public String getSourceUrl() {
        return source.getUri(0);
    }

}
