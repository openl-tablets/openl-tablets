package org.openl.rules.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class BussinessSearchResultBean {  
    
    private TableBusSearchResult[] tableRes = null;
    private OpenLBussinessSearchResult busSearchRes = null;
    private List<Property> propValues = new ArrayList<Property>();
    private List<TableProperty> propsForSearch = new ArrayList<TableProperty>();    
    
    
    public List<Property> getPropValues() {
        return propValues;
    }

    public void setPropValues(List<Property> propValues) {
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
            List<Property> prop = initPropListForUI(tableSearch);
            tbsr.setPropValues(prop);
            tableRes[tableRes.length-1] = tbsr;
        }   
    }
    
    private List<Property> initPropListForUI(TableSyntaxNode tableSearch) {
        List<Property> prop = new ArrayList<Property>();
        for(TableProperty propForSearch : propsForSearch) {
            String propName = propForSearch.getName();
            String propDisplName = DefaultPropertyDefinitions.getPropertyDisplayName(propName);
            ObjectValue propValue = tableSearch.getPropertyValue(propName);
            if(propValue==null) {
                propValue = new ObjectValue("--EMPTY--");
            }
            Property pr = new Property(new StringValue(propDisplName), propValue);
            prop.add(pr);
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
