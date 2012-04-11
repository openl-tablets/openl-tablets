package org.openl.rules.ui.search;

import java.util.HashMap;
import java.util.Map;

public class TableBusSearchResult {
    
    Map<String, Object> propValues = new HashMap<String, Object>();

    public Map<String, Object> getPropValues() {
        return propValues;
    }

    public void setPropValues(Map<String, Object> propValues) {
        this.propValues = propValues;
    }
    
    public String getColumnValue(String displayName) {
        return null;
    }
}
