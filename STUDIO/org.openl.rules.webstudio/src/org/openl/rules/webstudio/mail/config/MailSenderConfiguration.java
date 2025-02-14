package org.openl.rules.webstudio.mail.config;

import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.service.UserSettingManagementService;

@Configuration
public class MailSenderConfiguration {

    @Bean
    public MailSenderProperties mailSenderProperties(@Value("${mail.url}") String url,
                                                     @Value("${mail.username}") String user,
                                                     @Value("${mail.password}") String password) {
        return new MailSenderProperties(url, user, password);
    }

    @Bean
    public MailSender mailSender(MailSenderProperties mailSenderProperties,
                                 UserSettingManagementService userSettingManagementService) {
        return new MailSender(mailSenderProperties, userSettingManagementService);
    }

    @Bean
    public BooleanSupplier mailSenderFeature(MailSenderProperties mailSenderProperties) {
        return mailSenderProperties::isValidEmailSettings;
    }

}
