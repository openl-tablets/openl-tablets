package org.openl.rules.dt.type.domains;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.validation.properties.dimentional.ADispatcherTableColumn;

/**
 * Collect domains for all dimension properties.
 *
 * @author DLiauchuk
 *
 */
public class DimensionPropertiesDomainsCollector {

    /**
     * Map of domain collectors. Key: property name. Value: domain collector for this property.
     */
    private Map<String, IDomainCollector> domainCollectors = new HashMap<>();

    /**
     * Map of domains for appropriate properties.
     */
    private Map<String, IDomainAdaptor> propertiesDomains = new HashMap<>();

    // date domain collector should be one for all dates in project.
    private DateDomainCollector dateDomainCollector = new DateDomainCollector();

    // FIXME: remove this variable
    protected static final String CURRENT_DATE_PARAM = "currentDate";

    public DimensionPropertiesDomainsCollector() {
        initDomainCollectors();
    }

    public Map<String, IDomainAdaptor> gatherPropertiesDomains(List<Map<String, Object>> methodsProperties) {
        gatherAllDomains(methodsProperties);
        applyAllDomains();
        return new HashMap<>(propertiesDomains);
    }

    private void applyAllDomains() {
        // ensure that map of gathered domains is empty.
        //
        propertiesDomains.clear();

        IDomainAdaptor dateDomainAdaptor = null;
        for (String propNeedDomain : domainCollectors.keySet()) {

            TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(propNeedDomain);
            // Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
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
                    String.format("%s%s%s", propNeedDomain, ADispatcherTableColumn.LOCAL_PARAM_SUFFIX, i),
                    domainAdaptor);
            }
        }
    }

    private void applyDomain(TablePropertyDefinition propDef, IDomainAdaptor gatheredDomain) {
        String propName = propDef.getName();
        String key = propName + ADispatcherTableColumn.LOCAL_PARAM_SUFFIX;
        if (gatheredDomain != null && !propertiesDomains.containsKey(key)) {
            if (propDef.getExpression() != null) {
                propertiesDomains.put(propDef.getExpression().getMatchExpression().getContextAttribute(),
                    gatheredDomain);
                propertiesDomains.put(propName, gatheredDomain);
                propertiesDomains.put(key, gatheredDomain);
            }
        }
    }

    private void applyCurrentDateDomain(IDomainAdaptor dateDomainAdaptor) {
        if (dateDomainAdaptor != null && !propertiesDomains
            .containsKey(DimensionPropertiesDomainsCollector.CURRENT_DATE_PARAM)) {
            propertiesDomains.put(DimensionPropertiesDomainsCollector.CURRENT_DATE_PARAM, dateDomainAdaptor);
        }
    }

    /**
     * Gather properties domains by domain collectors.
     */
    private void gatherAllDomains(List<Map<String, Object>> methodsProperties) {
        for (Map<String, Object> methodProperties : methodsProperties) {
            for (String propNeedDomain : domainCollectors.keySet()) {
                IDomainCollector domainCollector = domainCollectors.get(propNeedDomain);
                domainCollector.gatherDomains(methodProperties);
            }
        }
    }

    /**
     * Initialize domain collectors for each dimension property.
     */
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

        if (Date.class == propertyType) {
            dateDomainCollector.addPropertyToSearch(propertyName);
            result = dateDomainCollector;
        } else if (String.class == propertyType) {
            result = new StringDomainCollector(propertyName);
        } else if (propertyType.isEnum()) {
            result = new EnumDomainCollector(propertyName);
        } else if (propertyType.isArray() && propertyType.getComponentType().isEnum()) {
            result = new ArrayDomainCollector(propertyName);
        } else if (propertyType.isArray() && String.class == propertyType.getComponentType()) {
            result = new ArrayDomainCollector(propertyName);
        }
        return result;
    }
}
