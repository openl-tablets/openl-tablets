package org.openl.rules.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.binding.TableProperties.Property;

/**
 * Bean consists criteria for bussiness search
 * @author DLiauchuk
 *
 */
public class BussinessSearchCondition {
    
    private List<Property> propToSearch = new ArrayList<Property>();
    private String tableContains;
    
    public List<Property> getPropToSearch() {
        return propToSearch;
    }
    
    public void setPropToSearch(List<Property> propToSearch) {
        this.propToSearch = propToSearch;
    }
    
    public String getContains() {
        return tableContains;
    }
    
    public void setContains(String contains) {
        this.tableContains = contains;
    }
    
    
    
    
}
