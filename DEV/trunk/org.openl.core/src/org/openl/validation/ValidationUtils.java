package org.openl.validation;

import java.util.ArrayList;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

public class ValidationUtils {

    public static ValidationResult validationError(String message) {
        return validationError(message, "");
    }
    
    public static ValidationResult validationError(String message, String details) {

        ValidationResult validationResult = new ValidationResult(ValidationStatus.FAIL, null);

        OpenLMessage openlMessage = new OpenLMessage(message, details, Severity.ERROR);
        addValidationMessage(validationResult, openlMessage);

        return validationResult;
    }

    public static void addValidationMessage(ValidationResult validationResult, String message, Severity severity) {

        addValidationMessage(validationResult, message, "", severity);
    }

    public static void addValidationMessage(ValidationResult validationResult, String message, String details,
            Severity severity) {

        OpenLMessage openLMessage = new OpenLMessage(message, details, severity);
        addValidationMessage(validationResult, openLMessage);
    }

    public static void addValidationMessage(ValidationResult validationResult, OpenLMessage message) {

        validationResult.getMessages().add(message);
    }

    public static ValidationResult validationSuccess() {
        
        return new ValidationResult(ValidationStatus.SUCCESS, null);
    }
    
    public static List<OpenLMessage> getValidationMessages(List<ValidationResult> results) {
        
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
        
        for (ValidationResult result : results) {
            messages.addAll(result.getMessages());
        }
        
        return messages;
    }
   
}
