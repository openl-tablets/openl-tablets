package org.openl.rules.ui.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openl.meta.StringValue;
import org.openl.rules.search.OpenLBussinessSearch;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.lang.xls.binding.TableProperties.Property;

/**
 * Bean to handle business search, has session scope.
 * @author DLiauchuk
 *
 */
public class BussinesSearchPropertyBean {

    private List<TableProperty> propForSearch = new ArrayList<TableProperty>();
    private String tableContain;
    private final OpenLBussinessSearch search = new OpenLBussinessSearch();    
    private BussinessSearchResultBean busSearchResBean = null;
     
    
    public BussinessSearchResultBean getBusSearchResBean() {
        return busSearchResBean;
    }

    public void setBusSearchResBean(BussinessSearchResultBean busSearchResBean) {
        this.busSearchResBean = busSearchResBean;
    }
    
    public String getTableContain() {
        return tableContain;
    }

    public void setTableContain(String tableContain) {
        this.tableContain = tableContain;        
    }
    
    public List<TableProperty> getPropForSearch() {
        return propForSearch;
    }

    public void setPropForSearch(List<TableProperty> propForSearch) {
        this.propForSearch = propForSearch;
    }
    
    /**
     * During the construction, we take all the properties from DefaultPropertyDefinitions 
     * that must be included in business search 
     */
    public BussinesSearchPropertyBean() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if(propDefinition.isBusinessSearch()) {
                propForSearch.add(new TableProperty(propDefinition.getDisplayName(),
                    null,
                    propDefinition.getType() == null ? null : propDefinition.getType().getInstanceClass(),
                    propDefinition.getGroup(),
                    propDefinition.getName()));
            }
        }
        //busSearchResBean = new BussinessSearchResultBean(propForSearch);
    }
    
    public boolean isStudioReadOnly() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.getModel().isReadOnly();
    }
    
    /**
     * Initialize the conditions for business search
     */
    public void initBusSearchCond() {
        if(arePropertieValuesSet()) {
            List<Property> listforSearch = search.getBusSearchCondit().getPropToSearch();  
            listforSearch.clear();
            for(TableProperty prop : propForSearch) {
                if(prop.isStringValue() && (prop.getValue()!=null && !("").equals(prop.getValue()))) {
                    listforSearch.add(new Property(new StringValue(prop.getName()), new StringValue((String)prop.getValue())));
                } else {
                    if(prop.isDateValue() && prop.getValue()!=null) {
                        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                        listforSearch.add(new Property(new StringValue(prop.getName()), new StringValue(format.format((Date)prop.getValue()))));
                    }
                }
            }
            search.getBusSearchCondit().setContains(tableContain);
        }
    }
    
    public boolean isReady() {
        return WebStudioUtils.isStudioReady();
    }
    
    public boolean arePropertieValuesSet() {
        boolean result = false;
        for(TableProperty prop : propForSearch) {
            if((prop.getValue()!=null) && !("").equals(prop.getValue())){
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Request scope bean, holding flag if search run is required.
     */    
    public static class BussinessSearchRequest {
        private boolean needSearch;
        private BussinesSearchPropertyBean bussinessSearchBean;
        private List<TableSearch> tableSearchList;
                
        public BussinesSearchPropertyBean getBussinessSearchBean() {
            return bussinessSearchBean;
        }

        public void setBussinessSearchBean(BussinesSearchPropertyBean bussinessSearchBean) {
            this.bussinessSearchBean = bussinessSearchBean;
        }
        
        public boolean isSearching() {
            return needSearch;
        }
        
        /**
         * Start working on pressing the search button on UI
         * @return
         */
        public String search() {            
            needSearch = true;
            bussinessSearchBean.initBusSearchCond();            
            return null;
        }
        
        public List<TableSearch> getSearchResults() {
            if (!isSearching() || !bussinessSearchBean.isReady() || !bussinessSearchBean.arePropertieValuesSet()) {
                return Collections.emptyList();
            }
            if (tableSearchList == null) {
                ProjectModel model = WebStudioUtils.getWebStudio().getModel();
                model.runSearch(bussinessSearchBean.search);                
                tableSearchList = model.getSearchList(model.runSearch(bussinessSearchBean.search));                
            }
            return tableSearchList;
        }        
    }    
    
    
}
