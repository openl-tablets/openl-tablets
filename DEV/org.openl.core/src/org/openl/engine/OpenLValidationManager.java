package org.openl.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openl.ICompileContext;
import org.openl.OpenL;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

/**
 * Class that defines OpenL engine manager implementation for validation
 * operations.
 */
public class OpenLValidationManager extends OpenLHolder {

    private static ThreadLocal<Boolean> validationEnabled = new ThreadLocal<Boolean>(); // Workaroung
                                                                                        // for
                                                                                        // multiple
                                                                                        // validation
                                                                                        // for
                                                                                        // multi
                                                                                        // module
                                                                                        // projects.

    public static boolean isValidationEnabled() {
        Boolean validationIsOn = validationEnabled.get();
        return validationIsOn == null || validationIsOn;
    }

    public static void turnOffValidation() {
        validationEnabled.set(Boolean.FALSE);
    }

    public static void turnOnValidation() {
        validationEnabled.remove();
    }

    /**
     * Construct new instance of manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLValidationManager(OpenL openl) {
        super(openl);
    }

    /**
     * Invokes validation process for each registered validator.
     * 
     * @param openClass openClass to validate
     * @return list of validation results
     */
    public List<ValidationResult> validate(IOpenClass openClass) {
        if (isValidationEnabled()) {
            List<ValidationResult> results = new ArrayList<>();

            ICompileContext context = getOpenL().getCompileContext();

            // Check that compile context initialized. If context is null or
            // validation switched off then skip validation process.
            //
            if (context != null) {

                Set<IOpenLValidator> validators = context.getValidators();

                for (IOpenLValidator validator : validators) {

                    ValidationResult result = validator.validate(getOpenL(), openClass);

                    results.add(result);
                }

            }
            return results;
        }else{
            return Collections.emptyList();
        }
    }
}
