package org.openl.rules.webstudio.web.tableeditor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PropertyRow {

    private final PropertyRowType type;
    private final Object data;

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
