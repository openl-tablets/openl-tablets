package org.openl.rules.webstudio.web.tableeditor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PropertyRow {

    @Getter
    private final PropertyRowType type;
    @Getter
    private final Object data;

    @Override
    public String toString() {
        return type + "-" + data;
    }

}
