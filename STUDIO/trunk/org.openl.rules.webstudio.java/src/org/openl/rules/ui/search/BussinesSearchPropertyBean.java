package org.openl.rules.ui.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.component.html.HtmlOutputLabel;

import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class BussinesSearchPropertyBean {

    List<TableProperty> propForSearch = new ArrayList<TableProperty>();
     
    
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
    }
    
    public List<TableProperty> getPropForSearch() {
        return propForSearch;
    }

    public void setPropForSearch(List<TableProperty> propForSearch) {
        this.propForSearch = propForSearch;
    }
    
    public void search() {
        for(TableProperty propToSearch : propForSearch) {
            if(propToSearch.getType().equals(String.class)){
                System.out.println("name="+propToSearch.getName()+" value="+(String)propToSearch.getValue());
            } else {
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                System.out.println("name="+propToSearch.getName()+" value="+format.format((Date)propToSearch.getValue()));
            }

        }
    }
}
