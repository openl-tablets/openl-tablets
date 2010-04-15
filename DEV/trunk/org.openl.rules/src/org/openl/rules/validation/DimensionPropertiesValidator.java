package org.openl.rules.validation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.OpenL;

import org.openl.domain.DateRangeDomain;
import org.openl.domain.EnumDomain;
import org.openl.domain.StringDomain;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.DateRangeDomainAdaptor;
import org.openl.rules.dt.type.EnumDomainAdaptor;
import org.openl.rules.dt.type.IDomainAdaptor;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;


public class DimensionPropertiesValidator extends TablesValidator {
    
    private static final String VALIDATION_FAILED = "Validation failed for dispatcher table";

    private static Log LOG = LogFactory.getLog(DimensionPropertiesValidator.class);
    
    private Map<String, IDomainAdaptor> propertiesDomains = new HashMap<String,IDomainAdaptor>();
    private static List<String> propertiesNeedDomain = new ArrayList<String>();
    
    static {
        String[] dimensionProperties = TablePropertyDefinitionUtils.getDimensionalTableProperties();
        
        for (String dimensionProp : dimensionProperties) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(dimensionProp);
            boolean dateType = Date.class.equals(propType);
            boolean stringType = String.class.equals(propType);
            boolean enumtype = propType.isEnum();
            boolean arrayEnumType = propType.isArray() && propType.getComponentType().isEnum();
            if (dateType || stringType || enumtype || arrayEnumType) {
                propertiesNeedDomain.add(dimensionProp);                
            }
        }        
    }
    
    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        gatherPropDomains(tableSyntaxNodes);
        ValidationResult validationResult = null;        
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getDisplayName() != null && tsn.getDisplayName().contains(DispatcherTableBuilder.DEFAULT_METHOD_NAME)) {
                
                DesionTableValidationResult dtValidResult = null;
                try {
                    dtValidResult = DecisionTableValidator.validateTable((DecisionTable)tsn.getMember(), propertiesDomains, openClass);     
                } catch (Exception t) {
                    throw new OpenLRuntimeException(VALIDATION_FAILED, t);
                }
                if (dtValidResult != null && dtValidResult.hasProblems()) {
                    tsn.setValidationResult(dtValidResult);
                    if (validationResult == null) {
                        validationResult = new ValidationResult(ValidationStatus.FAIL); 
                    } 
                    ValidationUtils.addValidationMessage(validationResult, new OpenLErrorMessage(
                                new SyntaxNodeException(dtValidResult.toString(),
                                        null, tsn)));       
                } 
            }                        
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }    
    
    private void gatherPropDomains(TableSyntaxNode[] tableSyntaxNodes) {
        propertiesDomains.clear();
        DateRangeDomain dateDomain = gatherDateDomain(tableSyntaxNodes);
        applyDateDomain(dateDomain);
        gatherStringDomains(tableSyntaxNodes);
        gatherEnumDomains(tableSyntaxNodes);
        gatherArrayEnumDomains(tableSyntaxNodes);
    }

    @SuppressWarnings("unchecked")
    private void gatherArrayEnumDomains(TableSyntaxNode[] tableSyntaxNodes) {
        for (String propNeedDomain : propertiesNeedDomain) {
            Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (propertyType.isArray() && propertyType.getComponentType().isEnum()) {
                Class<?> componentType = propertyType.getComponentType();
                Set<Object> arrayEnumProperties = new HashSet<Object>();
                for (TableSyntaxNode tsn : tableSyntaxNodes) {
                    ITableProperties tableProperties = tsn.getTableProperties();
                    if (tableProperties != null) {
                        Object[] propValues = (Object[])tableProperties.getPropertyValue(propNeedDomain);
                        if (propValues != null) {
                            for (Object propValue : propValues) {
                                if (propValue != null)
                                    arrayEnumProperties.add(propValue);
                            }
                        }                        
                    }
                }    
                if (!arrayEnumProperties.isEmpty()) {
                    Object[] resultArray = (Object[])Array.newInstance(componentType, arrayEnumProperties.size());
                    
                    EnumDomain enumDomain = new EnumDomain(arrayEnumProperties.toArray(resultArray));
                    EnumDomainAdaptor enumDomainAdaptor = new EnumDomainAdaptor(enumDomain);
                    propertiesDomains.put(propNeedDomain, enumDomainAdaptor);
                    for (int i=1; i<= arrayEnumProperties.size(); i++) {
                        propertiesDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX + i, enumDomainAdaptor);
                    }
                } else {
                    // all values from enum will be used as domain. 
                }                
                
            }
        }
        
    }

    @SuppressWarnings("unchecked")
    private void gatherEnumDomains(TableSyntaxNode[] tableSyntaxNodes) {
        for (String propNeedDomain : propertiesNeedDomain) {
            Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (propertyType.isEnum()) {
                Set<Object> enumProp = new HashSet<Object>();
                for (TableSyntaxNode tsn : tableSyntaxNodes) {
                    ITableProperties tableProperties = tsn.getTableProperties();
                    if (tableProperties != null) {
                        Object propvalue = tableProperties.getPropertyValue(propNeedDomain);
                        if (propvalue != null)
                            enumProp.add(propvalue);
                    }
                }       
                if (!enumProp.isEmpty()) {
                    Object[] resultArray = (Object[])Array.newInstance(propertyType, enumProp.size());
                    
                    EnumDomain enumDomain = new EnumDomain(enumProp.toArray(resultArray));
                    EnumDomainAdaptor enumDomainAdaptor = new EnumDomainAdaptor(enumDomain);
                    propertiesDomains.put(propNeedDomain, enumDomainAdaptor);
                    propertiesDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX, enumDomainAdaptor);
                } else {
                    // all values from enum will be used as domain. 
                }                
            }
        }        
    }

    private void gatherStringDomains(TableSyntaxNode[] tableSyntaxNodes) {
        for (String propNeedDomain : propertiesNeedDomain) {
            Class<?> propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (String.class.equals(propertyType)) {
                Set<String> stringProp = new HashSet<String>();
                for (TableSyntaxNode tsn : tableSyntaxNodes) {
                    ITableProperties tableProperties = tsn.getTableProperties();
                    if (tableProperties != null) {
                        String propvalue = (String) tableProperties.getPropertyValue(propNeedDomain);
                        if (StringUtils.isNotEmpty(propvalue))
                            stringProp.add(propvalue);
                    }
                }
                if (stringProp.isEmpty()) {
                    //fake string domain it is because constrainer will be freezed with empty domain.  
                    stringProp.add("any");
                }
                StringDomain strDomain = new StringDomain(stringProp.toArray(new String[stringProp.size()]));
                EnumDomainAdaptor strDomainAdaptor = new EnumDomainAdaptor(strDomain);
                propertiesDomains.put(propNeedDomain, strDomainAdaptor);
                propertiesDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX, strDomainAdaptor);
            }
        }
    }

    private DateRangeDomain gatherDateDomain(TableSyntaxNode[] tableSyntaxNodes) {
        List<Date> dateProps = new ArrayList<Date>();
        for (String propNeedDomain : propertiesNeedDomain) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (Date.class.equals(propType)) {
                for (TableSyntaxNode tsn : tableSyntaxNodes) {
                    ITableProperties tableProperties = tsn.getTableProperties();
                    if (tableProperties != null) {
                        Date propValue = (Date) tableProperties.getPropertyValue(propNeedDomain);
                        if (propValue != null) {
                            dateProps.add(propValue);
                        }
                    }
                }
            }
        }

        if (!dateProps.isEmpty()) {
            Collections.sort(dateProps);
            DateRangeDomain dateRangeDomain = new DateRangeDomain(dateProps.get(0), dateProps.get(dateProps.size() - 1));
            return dateRangeDomain;
        } else {
            return null;
        }
    }

    private void applyDateDomain(DateRangeDomain domain) {
        DateRangeDomainAdaptor dateDomainAdaptor = new DateRangeDomainAdaptor(domain);
        for (String propNeedDomain : propertiesNeedDomain) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (Date.class.equals(propType)) {
                propertiesDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX, dateDomainAdaptor);
                if (!propertiesDomains.containsKey(DecisionTableCreator.CURRENT_DATE_PARAM)) {
                    propertiesDomains.put(DecisionTableCreator.CURRENT_DATE_PARAM, dateDomainAdaptor);
                }
            }
        }

    }
}
