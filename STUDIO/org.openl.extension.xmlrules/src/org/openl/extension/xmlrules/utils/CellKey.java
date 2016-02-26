package org.openl.extension.xmlrules.utils;

import java.util.Map;

public final class CellKey {
    private final String cell;
    private final Map<String, Object> params;

    /**
     * Cached hash code
     */
    private int hash = 0;

    public CellKey(String cell, Map<String, Object> params) {
        this.cell = cell;
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CellKey cellKey = (CellKey) o;

        if (!cell.equals(cellKey.cell))
            return false;

        if (params.size() != cellKey.params.size()) {
            return false;
        }

        for (String param : params.keySet()) {
            if (!cellKey.params.containsKey(param)) {
                return false;
            }

            Object value = params.get(param);
            Object otherValue = cellKey.params.get(param);

            if (value != null ? !value.equals(otherValue) : otherValue != null) {
                return false;
            }
        }
        return params.equals(cellKey.params);

    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            int result = cell.hashCode();
            result = 31 * result + params.hashCode();
            for (String param : params.keySet()) {
                result = 31 * result + param.hashCode();

                Object value = params.get(param);
                result = 31 * result + (value != null ? value.hashCode() : 0);
            }

            hash = result;
        }

        return hash;
    }
}
