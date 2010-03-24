package org.openl.rules.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.binding.TableProperties.Property;

public class TableBusSearchResult {
    
    List<Property> propValues = new ArrayList<Property>();

    public List<Property> getPropValues() {
        return propValues;
    }

    public void setPropValues(List<Property> propValues) {
        this.propValues = propValues;
    }
    
    public String getColumnValue(String displayName) {
        return null;
    }
}
