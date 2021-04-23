package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;

import org.openl.rules.rest.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

public class AbstractConstraintValidatorTest {

    @Autowired
    private BeanValidationProvider validationProvider;

    protected static void assertFieldError(String expectedField,
            String expectedMessage,
            Object expectedRejectedValue,
            FieldError actualError) {

        assertEquals(expectedField, actualError.getField());
        assertEquals(expectedRejectedValue, actualError.getRejectedValue());
        assertObjectError(expectedMessage, actualError);
    }

    protected static void assertObjectError(String expectedMessage, ObjectError actualError) {
        assertEquals(expectedMessage, actualError.getDefaultMessage());
    }

    protected BindingResult validateAndGetResult(Object bean, Validator... validators) {
        try {
            validationProvider.validate(bean, validators);
            return null;
        } catch (ValidationException e) {
            return e.getBindingResult();
        }
    }
}
