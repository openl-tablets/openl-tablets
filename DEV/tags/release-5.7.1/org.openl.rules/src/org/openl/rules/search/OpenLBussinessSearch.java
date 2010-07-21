package org.openl.rules.search;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

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
    private boolean isEqual(ITableProperties tableProperties) {
        boolean result = false;
        int numMatch = 0;
        Map<String, Object> propsFromSearch = busSearchCondit.getPropToSearch();
        for (Map.Entry<String, Object> propertyFromSearch : propsFromSearch.entrySet()) {            
            String propNameFromSearch = propertyFromSearch.getKey();
            Object propValueFromSearch = propertyFromSearch.getValue();
            if (tableProperties != null){
                Object propertyValue = tableProperties.getPropertyValue(propNameFromSearch);
                if(propertyValue != null) {
                    if(comparePropValues(propValueFromSearch, propertyValue) == 0) {
                        numMatch++;
                    } else {
                        if(propertyValue instanceof String && propValueFromSearch instanceof String
                                && checkIfContainString(((String)propertyValue).toLowerCase(),
                                    ((String)propValueFromSearch).toLowerCase())) {
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
    
    @SuppressWarnings("unchecked")
    private int comparePropValues(Object propValueFromSearch, Object propertyValue) {
        int result = -1;
        if(propertyValue instanceof String && propValueFromSearch instanceof String) 
            result = ((String)propertyValue).compareTo(((String)propValueFromSearch));
        else if(propertyValue instanceof Date && propValueFromSearch instanceof Date)
                result = ((Date)propertyValue).compareTo(((Date)propValueFromSearch));                
        else if(propertyValue instanceof Boolean && propertyValue instanceof Boolean)
                result = ((Boolean)propertyValue).compareTo(((Boolean)propValueFromSearch));
        else if(propertyValue instanceof Integer && propValueFromSearch instanceof Integer)
                result = ((Integer)propertyValue).compareTo(((Integer)propValueFromSearch));
        else if(propertyValue instanceof Enum && propValueFromSearch instanceof Enum)
            result = ((Enum)propertyValue).compareTo(((Enum)propValueFromSearch));
        else if(propertyValue.getClass().isArray() && propValueFromSearch.getClass().isArray()) {                        
            List propertyValueArray = Arrays.asList((Object[])propertyValue);
            List propertyValueFromSearchArray = Arrays.asList((Object[])propValueFromSearch);
            if (propertyValueArray.containsAll(propertyValueFromSearchArray)) {
                result = 0;
            }
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

