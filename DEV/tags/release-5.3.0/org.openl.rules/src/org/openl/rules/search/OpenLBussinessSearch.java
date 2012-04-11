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
        res = matchWithTableContains(res);
        return res;
    }
    
    /**
     * We need to filter our result, found by property values, with the result of tableContains field
     * 
     * @param busSearchRes Results found by property values
     * @return filtered results with table contains search result
     */
    private OpenLBussinessSearchResult matchWithTableContains(OpenLBussinessSearchResult busSearchRes) {
        OpenLBussinessSearchResult result = new OpenLBussinessSearchResult();
        TableSyntaxNode[] tablesContains = busSearchCondit.getTablesContains(); 
        if(tablesContains != null) {
            for(TableSyntaxNode table : busSearchRes.foundTables) {                
                for(TableSyntaxNode tablesConsist : tablesContains) {
                    if(table.equals(tablesConsist)) {
                        result.add(table);
                        break; //we need to break, because tableConsists result contains one table many times                                
                    }
                }
            }
        } else {
            result = busSearchRes;
        }
        return result;        
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
                if(property != null) {
                    if(property.getValue().compareTo(propFromSearch.getValue()) == 0) {
                        numMatch++;
                    } else {
                        if(property.getValue().getValue() instanceof String && propFromSearch.getValue().getValue() instanceof String
                                && checkIfContainString(((String)property.getValue().getValue()).toLowerCase(),
                                    ((String)propFromSearch.getValue().getValue()).toLowerCase())) {
                            numMatch++;
                            
                        }
                    }
                }                
            }
        }
        if(numMatch == propsFromSearch.size() && numMatch > 0) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Checks if the property value of the table consist the string from search condition.
     * It is made to find results that not fully match the search request, but also include.
     * To search by the parts of text properties.
     * @param propValue Value of the table property.
     * @param propValueFromSearch Value of the property from search condition.
     * @return
     */
    private boolean checkIfContainString(String propValue, String propValueFromSearch) {
        boolean result = false;
        if(propValue.contains(propValueFromSearch)) {
            result = true;
        }
        return result;
    }
    
}

