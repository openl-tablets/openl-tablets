package org.openl.rules.search;


import java.util.HashMap;
import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * 
 * Bean consists criteria for business search
 * @author DLiauchuk
 *
 */
public class BussinessSearchCondition {
    
    private Map<String, Object> propToSearch = new HashMap<String, Object>();
    
    /*
     * Contain all tables that suit to table contains search cryteria 
     */
    private TableSyntaxNode[] tablesContains;
    
    public Map<String, Object> getPropToSearch() {
        return propToSearch;
    }
    
    public void setPropToSearch(Map<String, Object> propToSearch) {
        this.propToSearch = propToSearch;
    }
    
    public TableSyntaxNode[] getTablesContains() {
        if (tablesContains != null) {
            return tablesContains.clone();
        } else {
            return null;
        }        
    }

    public void setTablesContains(TableSyntaxNode[] tablesContains) {
        if (tablesContains != null) {
            this.tablesContains = tablesContains.clone();
        }         
    }
    
}
