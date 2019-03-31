package org.openl.rules.webstudio.web.tableeditor;

public class PropertyRow {

    private PropertyRowType type;
    private Object data;

    public PropertyRow() {
    }

    public PropertyRow(PropertyRowType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public PropertyRowType getType() {
        return type;
    }

    public void setType(PropertyRowType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(type).append("-").append(data).toString();
    }

}
