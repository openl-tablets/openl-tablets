package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.OpenL;

import org.openl.domain.DateRangeDomain;
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
    
    private Map<String, IDomainAdaptor> propDomains = new HashMap<String,IDomainAdaptor>();
    private static List<String> propsNeedDomain = new ArrayList<String>();
    
    static {
        String[] dimensionProperties = TablePropertyDefinitionUtils.getDimensionalTableProperties();
        
        for (String dimensionProp : dimensionProperties) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(dimensionProp);
            if (Date.class.equals(propType) || String.class.equals(propType)) {
                propsNeedDomain.add(dimensionProp);                
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
                    dtValidResult = DecisionTableValidator.validateTable((DecisionTable)tsn.getMember(), propDomains, openClass);     
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
        propDomains.clear();
        DateRangeDomain dateDomain = gatherDateDomain(tableSyntaxNodes);
        applyDateDomain(dateDomain);
        gatherStringDomains(tableSyntaxNodes);
    }

    private void gatherStringDomains(TableSyntaxNode[] tableSyntaxNodes) {
        for (String propNeedDomain : propsNeedDomain) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (String.class.equals(propType)) {
                List<String> stringProp = new ArrayList<String>();
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
                propDomains.put(propNeedDomain, strDomainAdaptor);
                propDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX, strDomainAdaptor);
            }
        }
    }

    private DateRangeDomain gatherDateDomain(TableSyntaxNode[] tableSyntaxNodes) {
        List<Date> dateProps = new ArrayList<Date>();
        for (String propNeedDomain : propsNeedDomain) {
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
        for (String propNeedDomain : propsNeedDomain) {
            Class<?> propType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propNeedDomain);
            if (Date.class.equals(propType)) {
                propDomains.put(propNeedDomain + DecisionTableCreator.LOCAL_PARAM_SUFFIX, dateDomainAdaptor);
                if (!propDomains.containsKey(DecisionTableCreator.CURRENT_DATE_PARAM)) {
                    propDomains.put(DecisionTableCreator.CURRENT_DATE_PARAM, dateDomainAdaptor);
                }
            }
        }

    }
}
