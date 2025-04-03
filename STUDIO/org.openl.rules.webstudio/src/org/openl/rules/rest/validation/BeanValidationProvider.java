package org.openl.rules.rest.validation;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import org.openl.rules.rest.exception.ValidationException;

/**
 * Custom Bean Validation provider. This validator must be used in case of impossibility to define POJO argument in
 * Spring Rest Controller. In the rest of simple cases use {@link org.springframework.validation.annotation.Validated}
 * annotation on method argument. Additional validators can be added to the default
 * {@link org.springframework.web.bind.WebDataBinder} via {@link org.springframework.web.bind.annotation.InitBinder}.
 *
 * @author Vladyslav Pikus
 */
public class BeanValidationProvider {

    private static final Validator[] EMPTY_VALIDATORS = new Validator[0];
    public static final Class<?>[] EMPTY_GROUPS = new Class<?>[0];

    private final Validator[] commonValidators;

    public BeanValidationProvider(List<Validator> commonValidators) {
        this.commonValidators = commonValidators.toArray(EMPTY_VALIDATORS);
    }

    /**
     * Invokes all bean validators. The following validators will not be executed if previous one returned error.
     *
     * @param bean       POJO bean to validate
     * @param validators additional validators
     */
    public void validate(Object bean, Validator... validators) {
        validate(bean, validators, EMPTY_GROUPS);
    }

    public void validate(Object bean, Class<?>[] groups) {
        validate(bean, EMPTY_VALIDATORS, groups);
    }

    private void validate(Object bean, Validator[] validators, Class<?>[] groups) {
        var binder = new CustomDataBinder(bean);
        binder.addValidators(commonValidators);
        binder.addValidators(validators);
        if (groups != null && groups.length > 0) {
            binder.validate((Object[]) groups);
        } else {
            binder.validate();
        }

        var bindingResult = binder.getBindingResult();
        if (bindingResult.hasErrors()) {
            throw new ValidationException(bindingResult);
        }
    }

    private static class CustomDataBinder extends DataBinder {

        public CustomDataBinder(Object target) {
            super(target);
        }

        @Override
        public void validate() {
            Object target = getTarget();
            Assert.state(target != null, "No target to validate");
            BindingResult bindingResult = getBindingResult();
            // Call each validator with the same binding result
            for (Validator validator : getValidators()) {
                validator.validate(target, bindingResult);
                if (bindingResult.hasErrors()) {
                    break;
                }
            }
        }
    }
}
