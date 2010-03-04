/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator;

import org.openl.OpenL;

/**
 * @author snshor
 *
 */
public interface IValidator {

    public IValidationResult validate(IValidatedObject ivo, OpenL openl);

}
