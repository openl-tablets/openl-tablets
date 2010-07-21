package org.openl.rules.dt.type.domains;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

import org.openl.domain.EnumDomain;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

public class EnumDomainCollector implements IDomainCollector {
    
    private String propertyToSearch;
    
    private Set<Object> enumProp = new HashSet<Object>();

    public EnumDomainCollector(String propertyToSearch) {
        this.propertyToSearch = propertyToSearch;
    }

    public void gatherDomains(TableSyntaxNode tsn) {
        ITableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties != null) {
            Object propvalue = tableProperties.getPropertyValue(propertyToSearch);
            if (propvalue != null) {                    
                enumProp.add(propvalue);
            }
        }        
    }    
    
    @SuppressWarnings("unchecked")
    public IDomainAdaptor getGatheredDomain() {
        IDomainAdaptor result = null;
        if (!enumProp.isEmpty()) {
            Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propertyToSearch);
            Object[] resultArray = (Object[])Array.newInstance(propertyType, enumProp.size());
            
            EnumDomain enumDomain = new EnumDomain(enumProp.toArray(resultArray));
            result = new EnumDomainAdaptor(enumDomain);
            
        } else {
            // all values from enum will be used as domain. 
        }         
        return result; 
    }

}
