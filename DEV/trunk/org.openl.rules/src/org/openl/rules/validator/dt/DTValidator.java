/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import java.util.Map;
import org.openl.OpenL;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.validator.IValidatedObject;
import org.openl.rules.validator.IValidationResult;
import org.openl.rules.validator.IValidator;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenClass;

/**
 * @author snshor
 * 
 */
public class DTValidator implements IValidator {
    public static DTValidationResult validateDT(DecisionTable decisionTable,
            Map<String, IDomainAdaptor> domains, IOpenClass type)
            throws Exception {

        return new DTValidator().validateDT(new DTValidatedObject(decisionTable, domains),
                ((XlsModuleOpenClass) type).getOpenl());
    }

    public static DTValidationResult validateDT(String dtname,
            Map<String, IDomainAdaptor> domains, IOpenClass type)
            throws Exception {
        DecisionTable dt = (DecisionTable) AOpenClass.getSingleMethod(dtname,
                type.methods());

        return validateDT(dt, domains, type);
    }

    public IValidationResult validate(IValidatedObject ivo, OpenL openl) {
        return validateDT(ivo, openl);
    }

    public DTValidationResult validateDT(IValidatedObject ivo, OpenL openl) {
        return new ValidationAlgorithm((IDTValidatedObject) ivo, openl)
                .validateDT();
    }
}
