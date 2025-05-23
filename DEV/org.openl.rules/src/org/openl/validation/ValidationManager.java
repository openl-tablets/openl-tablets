package org.openl.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.ICompileContext;
import org.openl.binding.IBindingContext;
import org.openl.message.OpenLMessagesUtils;
import org.openl.types.IOpenClass;

public class ValidationManager {

    private static final ThreadLocal<Boolean> validationEnabled = new ThreadLocal<>(); // Workaround

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

    public static void validate(ICompileContext context, IOpenClass openClass, IBindingContext bindingContext) {
        List<ValidationResult> validationResults = processValidation(context, openClass);
        for (ValidationResult validationResult : validationResults) {
            bindingContext.addMessages(validationResult.getMessages());
        }
    }

    /**
     * Invokes validation process for each registered validator.
     *
     * @param openClass openClass to validate
     * @return list of validation results
     */
    public static List<ValidationResult> processValidation(ICompileContext context, IOpenClass openClass) {
        if (!ValidationManager.isValidationEnabled()) {
            return Collections.emptyList();
        }
        List<ValidationResult> results = new ArrayList<>();
        // Check that compile context initialized. If context is null or
        // validation switched off then skip validation process.
        if (context != null) {
            Set<IOpenLValidator> validators = context.getValidators();
            for (IOpenLValidator validator : validators) {
                ValidationResult result;
                try {
                    result = validator.validate(openClass);
                    results.add(result);
                } catch (Exception e) {
                    result = new ValidationResult(ValidationStatus.FAIL,
                            Collections.singletonList(
                                    OpenLMessagesUtils.newErrorMessage(String.format("Failed to execute validator: %s. %s",
                                            validator.getClass().getTypeName(),
                                            ExceptionUtils.getRootCauseMessage(e)))));
                }
                results.add(result);
            }
        }
        return results;
    }

}
