package org.openl.rules.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Bean consists criteria for bussiness search
 * @author DLiauchuk
 *
 */
public class BussinessSearchCondition {
    
    private List<Property> propToSearch = new ArrayList<Property>();
    
    /*
     * Contain all tables that suit to table contains search cryteria 
     */
    private TableSyntaxNode[] tablesContains;
    
    public List<Property> getPropToSearch() {
        return propToSearch;
    }
    
    public void setPropToSearch(List<Property> propToSearch) {
        this.propToSearch = propToSearch;
    }
    
    public TableSyntaxNode[] getTablesContains() {
        return tablesContains;
    }

    public void setTablesContains(TableSyntaxNode[] tablesContains) {
        this.tablesContains = tablesContains;
    }
    
}
