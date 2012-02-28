package org.openl.rules.ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class BussinessSearchResultBean {  
    
    private static final String EMPTY_VALUE = "--EMPTY--";    
    private TableBusSearchResult[] tableRes = null;
    private OpenLBussinessSearchResult busSearchRes = null;
    private Map<String, Object> propValues = new HashMap<String, Object>();
    private List<TableProperty> propsForSearch = new ArrayList<TableProperty>();    
    
    
    public Map<String, Object> getPropValues() {
        return propValues;
    }

    public void setPropValues(Map<String, Object> propValues) {
        this.propValues = propValues;
    }

    public List<TableProperty> getPropsForSearch() {
        return propsForSearch;
    }

    public void setPropsForSearch(List<TableProperty> propsForSearch) {
        this.propsForSearch = propsForSearch;
    }    

    public TableBusSearchResult[] getTableRes() {
        return tableRes;
    }

    public void setTableRes(TableBusSearchResult[] tableRes) {
        this.tableRes = tableRes;
    }
    
    public BussinessSearchResultBean(List<TableProperty> propForSearch) {
        this.propsForSearch = propForSearch;        
    }

    public OpenLBussinessSearchResult getBusSearchRes() {
        return busSearchRes;
    }

    /*public List<Property> getPropValues() {
        if(busSearchRes!=null && !propsForSearch.isEmpty()) {
            for(TableProperty propForSearch : propsForSearch)                
                for(TableSyntaxNode table : busSearchRes.getFoundTables()) {
                    table.getProperty();
            }
        }
        
        
        return propValues;
    }*/

    /*public void setPropValues(List<Property> propValues) {
        this.propValues = propValues;
    }*/

    public void setBusSearchRes(OpenLBussinessSearchResult busSearchRes) {
        this.busSearchRes = busSearchRes;
        initTableResList();
    }
    
    private void initTableResList() {
        tableRes = new TableBusSearchResult[busSearchRes.getFoundTables().size()];
        for(TableSyntaxNode tableSearch : busSearchRes.getFoundTables()) {
            TableBusSearchResult tbsr = new TableBusSearchResult();
            Map<String, Object> prop = initPropListForUI(tableSearch);
            tbsr.setPropValues(prop);
            tableRes[tableRes.length-1] = tbsr;
        }   
    }
    
    private Map<String, Object> initPropListForUI(TableSyntaxNode tableSearch) {
        Map<String, Object> prop = new HashMap<String, Object>();
        for(TableProperty propForSearch : propsForSearch) {
            String propName = propForSearch.getName();
            String propDisplName = TablePropertyDefinitionUtils.getPropertyDisplayName(propName);
            Object propValue = tableSearch.getTableProperties().getPropertyValue(propName);
            if(propValue==null) {
                propValue = new String(EMPTY_VALUE);
            }            
            prop.put(propDisplName, propValue);
        }
        return prop;
    }
    
    private List<String> labels = new ArrayList<String>();
    
    private List<String> values = new ArrayList<String>();

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
    
    public String getColumnValue(int index) {
        int k = index;
        return null;
    }
    
    
}
