package org.openl.studio.config;

import java.util.List;
import jakarta.validation.ValidatorFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import org.openl.studio.common.validation.BeanValidationProvider;

/**
 * OpenL Studio validation configuration
 *
 * @author Vladyslav Pikus
 */
@Configuration
public class ValidationConfiguration {

    @Bean
    public MessageSource validationMessageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:ValidationMessages");
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
