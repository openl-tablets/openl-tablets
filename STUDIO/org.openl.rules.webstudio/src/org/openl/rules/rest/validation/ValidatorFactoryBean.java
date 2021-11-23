package org.openl.rules.rest.validation;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

/**
 * Factory bean for {@link LocalValidatorFactoryBean}
 *
 * @author Vladyslav Pikus
 */
public class ValidatorFactoryBean implements FactoryBean<LocalValidatorFactoryBean> {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    @Qualifier("validationMessageSource")
    private MessageSource validationMessageSource;

    @Override
    public LocalValidatorFactoryBean getObject() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setValidationMessageSource(validationMessageSource);
        validatorFactory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        validatorFactory.afterPropertiesSet();
        return validatorFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return LocalValidatorFactoryBean.class;
    }
}
