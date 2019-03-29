package org.openl.rules.cmatch;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableRow {
    private final Map<String, SubValue[]> id2values;

    public TableRow() {
        id2values = new LinkedHashMap<>();
    }

    public void add(String columnId, SubValue[] values) {
        id2values.put(columnId, values);
    }

    public SubValue[] get(String columnId) {
        return id2values.get(columnId);
    }

}
