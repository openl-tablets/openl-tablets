package org.openl.rules.rest.validation;

import java.util.List;

import org.openl.rules.rest.exception.ValidationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

public class BeanValidationProvider {

    private final Validator[] commonValidators;

    public BeanValidationProvider(List<Validator> commonValidators) {
        this.commonValidators = commonValidators.toArray(new Validator[0]);
    }

    public void validate(Object bean, Validator... validators) {
        validateInternal(bean, commonValidators);
        validateInternal(bean, validators);
    }

    private void validateInternal(Object bean, Validator[] validators) {
        DataBinder binder = new DataBinder(bean);
        binder.addValidators(validators);
        binder.validate();

        BindingResult bindingResult = binder.getBindingResult();
        if (bindingResult.hasErrors()) {
            throw new ValidationException(bindingResult);
        }
    }
}
