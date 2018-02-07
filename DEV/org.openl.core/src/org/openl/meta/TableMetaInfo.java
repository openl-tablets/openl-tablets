package org.openl.meta;

public class TableMetaInfo implements IMetaInfo {
    private final String name;
    private final String url;

    public TableMetaInfo(String tableType, String tableName, String url) {
        this.name = tableType + " " + tableName;
        this.url = url;
    }

    @Override
    public String getDisplayName(int mode) {
        return name;
    }

    @Override
    public String getSourceUrl() {
        return url;
    }
}
