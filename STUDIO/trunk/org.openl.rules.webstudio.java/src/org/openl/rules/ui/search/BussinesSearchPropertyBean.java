package org.openl.rules.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;

public class BussinesSearchPropertyBean {

    List<TablePropertyDefinition> propertiesForSearch = new ArrayList<TablePropertyDefinition>();
    
    public BussinesSearchPropertyBean() {
        for (TablePropertyDefinition propDef : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if(propDef.isBusinessSearch()) {
                propertiesForSearch.add(propDef);                
            }
        }
    }
    
    public List<TablePropertyDefinition> getPropertiesForSearch() {
        return propertiesForSearch;
    }

    public void setPropertiesForSearch(List<TablePropertyDefinition> propertiesForSearch) {
        this.propertiesForSearch = propertiesForSearch;
    }
}
