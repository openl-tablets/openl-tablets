package org.openl.rules.dt.type.domains;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.validation.properties.dimentional.ADimensionPropertyColumn;
import org.openl.rules.validation.properties.dimentional.DecisionTableCreator;

public class DimensionPropertiesDomainsCollector {
    
    private Map<String, IDomainCollector> domainCollectors = new HashMap<String, IDomainCollector>();
    private Map<String, IDomainAdaptor> propertiesDomains = new HashMap<String,IDomainAdaptor>();
    
    // date domain collector should be one for all dates in project. 
    private DateDomainCollector dateDomainCollector = new DateDomainCollector();    
    
    public DimensionPropertiesDomainsCollector() {
        initDomainCollectors();
    }
    
    public Map<String, IDomainAdaptor> getGatheredPropertiesDomains() {
        return new HashMap<String, IDomainAdaptor>(propertiesDomains);
    }
    
    public Set<String> getPropertiesNeedDomain() {
        return new HashSet<String>(domainCollectors.keySet());
    }
    
    public void gatherPropertiesDomains(TableSyntaxNode[] tableSyntaxNodes) {
        propertiesDomains.clear();
        gatherAllDomains(tableSyntaxNodes);
        applyAllDomains();        
    }

    private void applyAllDomains() {
        IDomainAdaptor dateDomainAdaptor = null;
        for (String propNeedDomain : domainCollectors.keySet()) {
            
            TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(propNeedDomain);
//            Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            Class<?> propertyType = propDef.getType().getInstanceClass();

            IDomainCollector domainCollector = domainCollectors.get(propNeedDomain);
            applyDomain(propDef, domainCollector.getGatheredDomain());
            if (dateDomainAdaptor == null && Date.class.equals(propertyType)) {
                dateDomainAdaptor = domainCollector.getGatheredDomain();
            } else if (propertyType.isArray() && propertyType.getComponentType().isEnum()) {
                applyArrayDomains(propNeedDomain, domainCollector);
            }
        }
        applyCurrentDateDomain(dateDomainAdaptor);
    }

    private void applyArrayDomains(String propNeedDomain, IDomainCollector domainCollector) {
        ArrayDomainCollector arrayCollector = (ArrayDomainCollector) domainCollector;
        IDomainAdaptor domainAdaptor = arrayCollector.getGatheredDomain();
        if (domainAdaptor != null) {
            propertiesDomains.put(propNeedDomain, domainAdaptor);
            for (int i = 1; i <= arrayCollector.getNumberOfDomainElements(); i++) {
                propertiesDomains.put(
                        String.format("%s%s%s", propNeedDomain, ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX, i), 
                        domainAdaptor);
            }
        }
    }

    private void applyDomain(TablePropertyDefinition propDef, IDomainAdaptor gatheredDomain) {
        String propName = propDef.getName();
        String key = propName + ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX;
        if (gatheredDomain != null && !propertiesDomains.containsKey(key)) {
            propertiesDomains.put(propDef.getExpression().getMatchExpression().getContextAtribute(), gatheredDomain);
            propertiesDomains.put(propName, gatheredDomain);
            propertiesDomains.put(key, gatheredDomain);
        }        
    }

    private void applyCurrentDateDomain(IDomainAdaptor dateDomainAdaptor) {
        if (dateDomainAdaptor != null && !propertiesDomains.containsKey(DecisionTableCreator.CURRENT_DATE_PARAM)) {
            propertiesDomains.put(DecisionTableCreator.CURRENT_DATE_PARAM, dateDomainAdaptor);
        }        
    }

    private void gatherAllDomains(TableSyntaxNode[] tableSyntaxNodes) {
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            for (String propNeedDomain : domainCollectors.keySet()) {
                IDomainCollector domainCollector = domainCollectors.get(propNeedDomain);
                domainCollector.gatherDomains(tsn);
            }
        }        
    }

    private void initDomainCollectors() {        
        for (TablePropertyDefinition property : TablePropertyDefinitionUtils.getDimensionalTableProperties()) {
            IDomainCollector domainCollector = getDomainCollector(property);
            if (domainCollector != null) {
                domainCollectors.put(property.getName(), domainCollector);
            }
        }        
    }

    private IDomainCollector getDomainCollector(TablePropertyDefinition property) {
        Class<?> propertyType = property.getType().getInstanceClass();
        String propertyName = property.getName();
        IDomainCollector result = null;
        
        if (result == null) {
            if (Date.class.equals(propertyType)) {
                dateDomainCollector.addPropertyToSearch(propertyName);
                result = dateDomainCollector;
            } else if (String.class.equals(propertyType)) {
                result = new StringDomainCollector(propertyName);
            } else if (propertyType.isEnum()) {
                result = new EnumDomainCollector(propertyName);
            } else if (propertyType.isArray() && propertyType.getComponentType().isEnum()) {
                result = new ArrayDomainCollector(propertyName);
            } else if (propertyType.isArray() && String.class.equals(propertyType.getComponentType())) {
                result = new ArrayDomainCollector(propertyName);
            } else {
                String message = String.format(
                        "Can`t find domain for property \"%s\" of type \"%s\"",
                        propertyName, propertyType.getSimpleName());
                OpenLMessagesUtils.addWarn(message);
            }
        }        
        return result;
    }
   

}
