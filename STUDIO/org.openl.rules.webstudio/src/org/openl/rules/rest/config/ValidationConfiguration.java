package org.openl.rules.rest.config;

import org.openl.rules.rest.validation.BeanValidationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import javax.validation.ValidatorFactory;
import java.util.List;

/**
 * WebStudio validation configuration
 *
 * @author Vladyslav Pikus
 */
@Configuration
public class ValidationConfiguration {

    @Bean
    public MessageSource validationMessageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/validation");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean(AutowireCapableBeanFactory beanFactory,
            @Qualifier("validationMessageSource") MessageSource validationMessageSource) {
        var validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setValidationMessageSource(validationMessageSource);
        validatorFactory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return validatorFactory;
    }

    @Bean("webstudioValidatorBean")
    public CustomValidatorBean validatorBean(
            @Qualifier("localValidatorFactoryBean") ValidatorFactory validatorFactory) {
        var validatorBean = new CustomValidatorBean();
        validatorBean.setValidatorFactory(validatorFactory);
        return validatorBean;
    }

    @Bean
    public BeanValidationProvider beanValidationProvider(
            @Qualifier("webstudioValidatorBean") CustomValidatorBean validator) {
        return new BeanValidationProvider(List.of(validator));
    }

}
