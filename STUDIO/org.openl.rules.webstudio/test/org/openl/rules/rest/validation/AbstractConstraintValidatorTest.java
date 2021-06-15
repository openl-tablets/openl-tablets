package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.openl.rules.rest.exception.RestRuntimeException;
import org.openl.rules.rest.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AbstractConstraintValidatorTest {

    @Autowired
    private BeanValidationProvider validationProvider;

    @Autowired
    private MessageSource validationMessageSource;

    protected void assertFieldError(String expectedField,
            String expectedMessage,
            Object expectedRejectedValue,
            FieldError actualError) {

        assertEquals(expectedField, actualError.getField());
        assertEquals(expectedRejectedValue, actualError.getRejectedValue());
        assertObjectError(expectedMessage, actualError);
    }

    protected void assertObjectError(String expectedMessage, ObjectError actualError) {
        assertEquals(expectedMessage, getLocalMessage(actualError));
    }

    protected BindingResult validateAndGetResult(Object bean, Validator... validators) {
        try {
            validationProvider.validate(bean, validators);
            return null;
        } catch (ValidationException e) {
            return e.getBindingResult();
        }
    }

    protected String getLocalMessage(ObjectError error) {
        try {
            return validationMessageSource.getMessage("openl.error." + error.getCode(), error.getArguments(), Locale.US);
        } catch (NoSuchMessageException e) {
            return error.getDefaultMessage();
        }
    }

    protected String getLocalMessage(RestRuntimeException e) {
        return validationMessageSource.getMessage(e.getErrorCode(), e.getArgs(), Locale.US);
    }
}
