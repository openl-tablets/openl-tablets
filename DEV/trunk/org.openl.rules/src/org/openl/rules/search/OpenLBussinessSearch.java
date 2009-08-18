package org.openl.rules.search;

import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.TableProperties.Property;

/**
 * Class to search tables, by {@link BussinessSearchCondition}  
 * @author DLiauchuk
 *
 */
public class OpenLBussinessSearch implements IOpenLSearch{
    
    private BussinessSearchCondition busSearchCondit = new BussinessSearchCondition();    
    
    public BussinessSearchCondition getBusSearchCondit() {
        return busSearchCondit;
    }
    
    public void setBusSearchCondit(BussinessSearchCondition busSearchCondit) {
        this.busSearchCondit = busSearchCondit;
    }
    
    
    /**
     * Searches the tables by {@link BussinessSearchCondition}
     * @param xsn All the existing tables
     * @return 
     */
    public Object search(XlsModuleSyntaxNode xsn) { 
        OpenLBussinessSearchResult res = new OpenLBussinessSearchResult();

        TableSyntaxNode[] tables = xsn.getXlsTableSyntaxNodesWithoutErrors();
        for (TableSyntaxNode table : tables) {
            if(isEqual(table.getTableProperties())){
                res.add(table);
            }
        }
        return res;
    }
    
    /**
     * Check if table properties from excel table consists all the values for properties,
     * from properties defined in {@link BussinessSearchCondition}
     * @param tableProperties
     * @return
     */
    private boolean isEqual(TableProperties tableProperties) {
        boolean result = false;
        int numMatch = 0;
        List<Property> propsFromSearch = busSearchCondit.getPropToSearch();
        for(Property propFromSearch : propsFromSearch) {
            if(tableProperties!=null){
                Property property = tableProperties.getProperty(propFromSearch.getKey().getValue());
                if(property!=null && property.getValue().getValue().equalsIgnoreCase(propFromSearch.getValue().getValue())) {
                    numMatch++;
                }
            }
        }
        if(numMatch == propsFromSearch.size()) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    //TO DO
    //private void searchByContainTable(){}
    
}

