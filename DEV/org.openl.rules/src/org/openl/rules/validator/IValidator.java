/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator;

import org.openl.OpenL;

/**
 * @author snshor
 */
public interface IValidator {

    IValidationResult validate(IValidatedObject objectToValidate, OpenL openl);

}
