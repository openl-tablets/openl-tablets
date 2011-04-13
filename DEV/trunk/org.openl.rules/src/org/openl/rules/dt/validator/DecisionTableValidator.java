/**
 * Created Feb 7, 2007
 */
package org.openl.rules.dt.validator;

import java.util.Map;

import org.openl.OpenL;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.validator.IValidatedObject;
import org.openl.rules.validator.IValidationResult;
import org.openl.rules.validator.IValidator;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class DecisionTableValidator implements IValidator {

    public static DesionTableValidationResult validateTable(DecisionTable decisionTable,
            Map<String, IDomainAdaptor> domains,
            IOpenClass type) throws Exception {

        DecisionTableValidatedObject validatedObject = new DecisionTableValidatedObject(decisionTable, domains);
        OpenL openl = ((XlsModuleOpenClass) type).getOpenl();

        return (DesionTableValidationResult) new DecisionTableValidator().validate(validatedObject, openl);
    }

    public IValidationResult validate(IValidatedObject validatedObject, OpenL openl) {
        return new ValidationAlgorithm((IDecisionTableValidatedObject) validatedObject, openl).validate();
    }

    
    /**
     * Provides unique name for Condition parameters
     * 
     * @param condition
     * @param pname
     * @return
     */
    
    static public String getUniqueConditionParamName(ICondition condition, String pname) {
        return condition.getName() + "_" + pname;
    }

    

}
