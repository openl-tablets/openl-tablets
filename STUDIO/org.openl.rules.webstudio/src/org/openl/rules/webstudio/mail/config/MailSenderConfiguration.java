package org.openl.rules.webstudio.mail.config;

import java.util.function.BooleanSupplier;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.mail.MailSenderImpl;
import org.openl.rules.webstudio.service.UserSettingManagementService;

@Configuration
@ConfigurationPropertiesScan
public class MailSenderConfiguration {

    @Bean
    public MailSender mailSender(MailSenderProperties mailSenderProperties,
                                 UserSettingManagementService userSettingManagementService) {
        return new MailSenderImpl(mailSenderProperties, userSettingManagementService);
    }

    @Bean
    public BooleanSupplier mailSenderFeature(MailSenderProperties mailSenderProperties) {
        return mailSenderProperties::isValidEmailSettings;
    }

}
