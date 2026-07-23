package org.openl.rules.webstudio.web.tableeditor;

public class PropertyRow {

    private final PropertyRowType type;
    private final Object data;

    public PropertyRow(PropertyRowType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public PropertyRowType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return type + "-" + data;
    }

}
