package org.openl.rules.validation;

import java.util.HashMap;
import java.util.Map;
import org.openl.OpenL;

import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.IDomainAdaptor;
import org.openl.rules.dt.type.IntRangeDomainAdaptor;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;


public class DimensionPropertiesValidator extends TablesValidator {

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        ValidationResult validationResult = null;        
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if ("Rules void valdateGapOverlap(int currentValue)".equals(tsn.getDisplayName())) {                
                Map<String, IDomainAdaptor> domains = makeParamDomains();
                
                DesionTableValidationResult dtValidResult = null;
                try {
                    //System.out.println("Validating <" + tableName+ ">");
                    dtValidResult = DecisionTableValidator.validateTable((DecisionTable)tsn.getMember(), domains, openClass);
                  
                    if (dtValidResult.hasProblems()) {
                        tsn.setValidationResult(dtValidResult);
                        //System.out.println("There are problems in table!!\n");
                    } else {
                        //System.out.println("NO PROBLEMS IN TABLE!!!!\n");
                    }
                } catch (Exception t) {
                    //t.printStackTrace();
                }
            }                        
        }
        return new ValidationResult(ValidationStatus.SUCCESS);
    }
    
    private Map<String, IDomainAdaptor> makeParamDomains() {
        IntRangeDomain intRangeDomain = new IntRangeDomain(0,50);
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("currentValue", intRangeDomainAdaptor);
        return domains;
    }

}
